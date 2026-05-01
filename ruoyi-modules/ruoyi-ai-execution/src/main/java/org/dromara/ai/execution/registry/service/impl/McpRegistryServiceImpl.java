package org.dromara.ai.execution.registry.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.domain.KmMcpServer;
import org.dromara.ai.execution.mapper.KmMcpServerMapper;
import org.dromara.ai.execution.registry.adapter.RegistrySourceAdapter;
import org.dromara.ai.execution.registry.adapter.dto.McpRegistryEntryDTO;
import org.dromara.ai.execution.registry.constant.McpRegistryConstants;
import org.dromara.ai.execution.registry.domain.KmMcpRegistryEntry;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;
import org.dromara.ai.execution.registry.domain.bo.McpImportBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySearchBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySourceBo;
import org.dromara.ai.execution.registry.domain.bo.McpServerManualBo;
import org.dromara.ai.execution.registry.domain.vo.McpRegistryEntryVO;
import org.dromara.ai.execution.registry.domain.vo.McpRegistrySourceVO;
import org.dromara.ai.execution.registry.domain.vo.SyncResultVO;
import org.dromara.ai.execution.registry.mapper.McpRegistryEntryMapper;
import org.dromara.ai.execution.registry.mapper.McpRegistrySourceMapper;
import org.dromara.ai.execution.registry.service.McpRegistryService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP 注册源集成 Service 实现
 *
 * @author Mahone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpRegistryServiceImpl implements McpRegistryService {

    /** 每批 UPSERT 的条目数量 */
    private static final int BATCH_SIZE = 100;

    private final McpRegistrySourceMapper sourceMapper;
    private final McpRegistryEntryMapper entryMapper;
    private final KmMcpServerMapper mcpServerMapper;
    private final List<RegistrySourceAdapter> adapters;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // 同步相关
    // =========================================================================

    /**
     * 触发指定注册源的全量同步
     * <p>
     * 整体方法不加事务，每批 UPSERT 通过 {@link #batchUpsertEntries} 独立事务保证原子性。
     */
    @Override
    public SyncResultVO syncSource(Long sourceId) {
        // 1. 查询注册源配置
        KmMcpRegistrySource source = sourceMapper.selectById(sourceId);
        if (source == null) {
            throw new ServiceException("注册源不存在，sourceId=" + sourceId);
        }

        // 2. 检查注册源是否启用
        if (!"1".equals(source.getIsEnabled())) {
            throw new ServiceException("注册源已禁用，无法同步，sourceId=" + sourceId);
        }

        // 3. 更新同步状态为 running
        updateSyncStatus(sourceId, McpRegistryConstants.SYNC_STATUS_RUNNING, null, null, null);

        try {
            // 4. 找到对应适配器
            RegistrySourceAdapter adapter = findAdapter(source.getPlatform());

            // 5. 拉取全量条目
            log.info("[McpRegistry] 开始同步注册源: sourceId={}, platform={}", sourceId, source.getPlatform());
            List<McpRegistryEntryDTO> dtoList = adapter.fetchAll(source);
            log.info("[McpRegistry] 拉取完成，共 {} 条条目", dtoList.size());

            // 6. 转换 DTO -> Entity
            List<KmMcpRegistryEntry> entities = dtoList.stream()
                    // 额外防御校验：确保唯一标识不为空
                    .filter(dto -> StrUtil.isNotBlank(dto.getExternalId()) && StrUtil.isNotBlank(dto.getEntryName()))
                    .map(dto -> convertToEntity(dto, sourceId))
                    .collect(Collectors.toList());

            // 7. 分批 UPSERT（每批独立事务）
            List<List<KmMcpRegistryEntry>> batches = CollUtil.split(entities, BATCH_SIZE);
            for (List<KmMcpRegistryEntry> batch : batches) {
                batchUpsertEntries(batch);
            }

            // 8. 软删除本次未出现的条目
            List<String> activeExternalIds = dtoList.stream()
                    .map(McpRegistryEntryDTO::getExternalId)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
            int offlineCount = entryMapper.markOfflineBySourceId(sourceId, activeExternalIds);
            log.info("[McpRegistry] 标记下线条目 {} 条", offlineCount);

            // 9. 更新同步成功状态
            updateSyncStatus(sourceId, McpRegistryConstants.SYNC_STATUS_SUCCESS,
                    LocalDateTime.now(), entities.size(), null);

            // 10. 构建返回结果
            SyncResultVO result = new SyncResultVO();
            result.setSourceId(sourceId);
            result.setSourceName(source.getSourceName());
            result.setSyncCount(entities.size());
            result.setSyncStatus(McpRegistryConstants.SYNC_STATUS_SUCCESS);
            result.setSyncTime(LocalDateTime.now());
            return result;

        } catch (Exception e) {
            // 10. 捕获异常，更新失败状态
            log.error("[McpRegistry] 同步失败: sourceId={}, error={}", sourceId, e.getMessage(), e);
            updateSyncStatus(sourceId, McpRegistryConstants.SYNC_STATUS_FAILED, null, null, e.getMessage());

            SyncResultVO result = new SyncResultVO();
            result.setSourceId(sourceId);
            result.setSourceName(source.getSourceName());
            result.setSyncCount(0);
            result.setSyncStatus(McpRegistryConstants.SYNC_STATUS_FAILED);
            result.setSyncTime(LocalDateTime.now());
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 批量 UPSERT 条目（独立事务，保证单批次原子性）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpsertEntries(List<KmMcpRegistryEntry> batch) {
        if (CollUtil.isEmpty(batch)) {
            return;
        }
        entryMapper.batchUpsert(batch);
    }

    // =========================================================================
    // 搜索相关
    // =========================================================================

    /**
     * 搜索注册源条目
     */
    @Override
    public TableDataInfo<McpRegistryEntryVO> searchEntries(McpRegistrySearchBo bo) {
        // 1. 计算分页偏移量
        int pageNum = bo.getPageNum() < 1 ? 1 : bo.getPageNum();
        int pageSize = bo.getPageSize() < 1 ? 20 : bo.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        // 2. 序列化 tags 为 JSON 字符串
        String tagsJson = serializeToJson(bo.getTags());

        String keyword = StrUtil.isBlank(bo.getKeyword()) ? null : bo.getKeyword().trim();
        String sourcePlatform = StrUtil.isBlank(bo.getSourcePlatform()) ? null : bo.getSourcePlatform().trim();

        // 3. 查询条目列表
        List<McpRegistryEntryVO> rows = entryMapper.searchEntries(keyword, sourcePlatform, tagsJson, offset, pageSize);

        // 4. 查询总数
        long total = entryMapper.countEntries(keyword, sourcePlatform, tagsJson);

        // 5. 查询已导入的 entry_id 集合，设置 isImported 字段
        if (CollUtil.isNotEmpty(rows)) {
            Set<Long> importedEntryIds = queryImportedEntryIds();
            rows.forEach(vo -> vo.setIsImported(importedEntryIds.contains(vo.getEntryId())));
        }

        // 6. 构建分页返回
        TableDataInfo<McpRegistryEntryVO> tableData = new TableDataInfo<>();
        tableData.setRows(rows);
        tableData.setTotal(total);
        tableData.setCode(200);
        tableData.setMsg("查询成功");
        return tableData;
    }

    /**
     * 获取注册源条目详情
     */
    @Override
    public McpRegistryEntryVO getEntryDetail(Long entryId) {
        // 1. 查询条目
        McpRegistryEntryVO vo = entryMapper.selectVoById(entryId);
        if (vo == null) {
            throw new ServiceException("注册源条目不存在，entryId=" + entryId);
        }

        // 2. 查询是否已导入
        Set<Long> importedEntryIds = queryImportedEntryIds();
        vo.setIsImported(importedEntryIds.contains(entryId));

        return vo;
    }

    // =========================================================================
    // 导入相关（任务 8 实现）
    // =========================================================================

    /**
     * 从注册源条目导入为 MCP Server 配置
     * <p>
     * 1. 查询注册源条目，不存在则抛出 ServiceException<br>
     * 2. 校验连接端点格式<br>
     * 3. 检查是否已存在相同 source_entry_id 的 MCP Server，根据 overwrite 决定新增或更新<br>
     * 4. 构建 MCP Server 实体并持久化<br>
     * 5. 返回导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object importEntry(Long entryId, McpImportBo bo) {
        // 1. 查询注册源条目
        KmMcpRegistryEntry entry = entryMapper.selectById(entryId);
        if (entry == null) {
            throw new ServiceException("注册源条目不存在");
        }

        // 2. 校验连接端点格式
        String transportType = entry.getTransportType();
        if ("sse".equals(transportType) || "streamable_http".equals(transportType)) {
            String endpointUrl = entry.getEndpointUrl();
            if (StrUtil.isBlank(endpointUrl)) {
                throw new ServiceException("endpointUrl 不能为空（传输类型为 " + transportType + "）");
            }
            if (!endpointUrl.startsWith("http://") && !endpointUrl.startsWith("https://")) {
                throw new ServiceException("endpointUrl 格式非法，必须以 http:// 或 https:// 开头");
            }
        } else if ("stdio".equals(transportType)) {
            if (StrUtil.isBlank(entry.getCommand())) {
                throw new ServiceException("command 不能为空（传输类型为 stdio）");
            }
        }

        // 3. 检查是否已存在相同 source_entry_id 的 MCP Server
        LambdaQueryWrapper<KmMcpServer> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(KmMcpServer::getSourceEntryId, entryId)
                .eq(KmMcpServer::getDelFlag, "0");
        KmMcpServer existing = mcpServerMapper.selectOne(existWrapper);

        if (existing != null && Boolean.FALSE.equals(bo.getOverwrite())) {
            throw new ServiceException("该 MCP Server 已存在，如需更新请选择覆盖");
        }

        // 4. 构建 server_config JSON
        String serverConfig = buildServerConfig(entry);

        // 5. 确定 serverName
        String serverName = StrUtil.isNotBlank(bo.getServerName())
                ? bo.getServerName()
                : (StrUtil.isNotBlank(entry.getDisplayName()) ? entry.getDisplayName() : entry.getEntryName());

        if (existing != null) {
            // 覆盖更新
            existing.setServerName(serverName);
            existing.setTransportType(transportType);
            existing.setServerConfig(serverConfig);
            existing.setDescription(entry.getDescription());
            existing.setSourceRegistryId(entry.getSourceId());
            existing.setSourceEntryId(entryId);
            existing.setImportSource(McpRegistryConstants.IMPORT_SOURCE_REGISTRY);
            mcpServerMapper.updateById(existing);

            Map<String, Object> result = new HashMap<>();
            result.put("serverId", existing.getServerId());
            result.put("serverName", existing.getServerName());
            result.put("action", "updated");
            return result;
        } else {
            // 新增
            KmMcpServer server = new KmMcpServer();
            server.setServerName(serverName);
            server.setTransportType(transportType);
            server.setServerConfig(serverConfig);
            server.setDescription(entry.getDescription());
            server.setStatus("0");
            server.setDelFlag("0");
            server.setSourceRegistryId(entry.getSourceId());
            server.setSourceEntryId(entryId);
            server.setImportSource(McpRegistryConstants.IMPORT_SOURCE_REGISTRY);
            mcpServerMapper.insert(server);

            Map<String, Object> result = new HashMap<>();
            result.put("serverId", server.getServerId());
            result.put("serverName", server.getServerName());
            result.put("action", "created");
            return result;
        }
    }

    /**
     * 手工添加 MCP Server
     * <p>
     * 1. 校验必填字段及格式<br>
     * 2. 检查名称唯一性<br>
     * 3. 构建实体，设置 import_source = 'manual'<br>
     * 4. 持久化到 km_mcp_server 表<br>
     * 5. 返回成功结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addManualServer(McpServerManualBo bo) {
        // 1. 校验必填字段
        if (StrUtil.isBlank(bo.getServerName())) {
            throw new ServiceException("serverName 不能为空");
        }
        if (StrUtil.isBlank(bo.getTransportType())) {
            throw new ServiceException("transportType 不能为空");
        }
        String transportType = bo.getTransportType();
        if (!"sse".equals(transportType) && !"stdio".equals(transportType) && !"streamable_http".equals(transportType)) {
            throw new ServiceException("transportType 必须是 'sse'、'streamable_http' 或 'stdio'");
        }
        if ("sse".equals(transportType) || "streamable_http".equals(transportType)) {
            if (StrUtil.isBlank(bo.getEndpointUrl())) {
                throw new ServiceException("endpointUrl 不能为空（传输类型为 " + transportType + "）");
            }
            if (!bo.getEndpointUrl().startsWith("http://") && !bo.getEndpointUrl().startsWith("https://")) {
                throw new ServiceException("endpointUrl 格式非法，必须以 http:// 或 https:// 开头");
            }
        } else if ("stdio".equals(transportType)) {
            if (StrUtil.isBlank(bo.getCommand())) {
                throw new ServiceException("command 不能为空（传输类型为 stdio）");
            }
        }

        // 2. 检查名称唯一性
        LambdaQueryWrapper<KmMcpServer> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(KmMcpServer::getServerName, bo.getServerName())
                .eq(KmMcpServer::getDelFlag, "0");
        Long count = mcpServerMapper.selectCount(nameWrapper);
        if (count > 0) {
            throw new ServiceException("MCP Server 名称已存在");
        }

        // 3. 构建 server_config JSON
        String serverConfig = buildManualServerConfig(bo);

        // 4. 构建实体
        KmMcpServer server = new KmMcpServer();
        server.setServerName(bo.getServerName());
        server.setTransportType(transportType);
        server.setServerConfig(serverConfig);
        server.setDescription(bo.getDescription());
        server.setStatus("0");
        server.setDelFlag("0");
        server.setImportSource(McpRegistryConstants.IMPORT_SOURCE_MANUAL);
        server.setSourceRegistryId(null);
        server.setSourceEntryId(null);
        mcpServerMapper.insert(server);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("serverId", server.getServerId());
        result.put("serverName", server.getServerName());
        result.put("action", "created");
        return result;
    }

    // =========================================================================
    // 注册源配置管理（任务 9 实现）
    // =========================================================================

    /**
     * 列出所有注册源配置
     */
    @Override
    public List<McpRegistrySourceVO> listSources() {
        LambdaQueryWrapper<KmMcpRegistrySource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmMcpRegistrySource::getDelFlag, "0");
        return sourceMapper.selectVoList(wrapper);
    }

    /**
     * 更新注册源配置（启用/禁用、同步间隔等）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSource(McpRegistrySourceBo bo) {
        // 1. 查询注册源，不存在则抛出异常
        KmMcpRegistrySource source = sourceMapper.selectById(bo.getSourceId());
        if (source == null || "2".equals(source.getDelFlag())) {
            throw new ServiceException("注册源不存在");
        }

        // 2. 校验 syncInterval 范围（3600~604800）
        if (bo.getSyncInterval() != null) {
            if (bo.getSyncInterval() < McpRegistryConstants.SYNC_INTERVAL_MIN
                    || bo.getSyncInterval() > McpRegistryConstants.SYNC_INTERVAL_MAX) {
                throw new ServiceException("同步间隔必须在 "
                        + McpRegistryConstants.SYNC_INTERVAL_MIN + " 到 "
                        + McpRegistryConstants.SYNC_INTERVAL_MAX + " 秒之间（1小时~7天）");
            }
        }

        // 3. 更新字段
        if (StrUtil.isNotBlank(bo.getSourceName())) {
            source.setSourceName(bo.getSourceName());
        }
        if (bo.getSyncInterval() != null) {
            source.setSyncInterval(bo.getSyncInterval());
        }
        if (StrUtil.isNotBlank(bo.getIsEnabled())) {
            source.setIsEnabled(bo.getIsEnabled());
        }
        if (StrUtil.isNotBlank(bo.getApiKey())) {
            source.setApiKey(bo.getApiKey());
        }
        if (bo.getRemark() != null) {
            source.setRemark(bo.getRemark());
        }

        // 4. 持久化
        sourceMapper.updateById(source);

        // 5. 发布变更事件，通知调度器更新任务
        eventPublisher.publishEvent(new org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent(
                this, source, org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent.ChangeType.UPDATED));
    }

    /**
     * 删除注册源及其缓存条目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(Long sourceId) {
        // 1. 查询注册源，不存在则抛出异常
        KmMcpRegistrySource source = sourceMapper.selectById(sourceId);
        if (source == null || "2".equals(source.getDelFlag())) {
            throw new ServiceException("注册源不存在");
        }

        // 2. 级联删除 km_mcp_registry_entry 中该注册源的所有条目
        entryMapper.deleteBySourceId(sourceId);

        // 3. 逻辑删除注册源（设置 del_flag = '2'）
        source.setDelFlag("2");
        sourceMapper.updateById(source);

        // 4. 发布变更事件，通知调度器取消任务
        eventPublisher.publishEvent(new org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent(
                this, source, org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent.ChangeType.DELETED));
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /**
     * 根据注册源条目构建 server_config JSON 字符串
     * <p>
     * server_config 格式参考现有 km_mcp_server 表的 JSON 结构：
     * SSE/streamable_http: {"url": "..."}
     * Stdio: {"command": "...", "args": [...], "env": {...}}
     */
    private String buildServerConfig(KmMcpRegistryEntry entry) {
        Map<String, Object> config = new HashMap<>();
        String transportType = entry.getTransportType();
        if ("sse".equals(transportType) || "streamable_http".equals(transportType)) {
            config.put("url", entry.getEndpointUrl());
        } else if ("stdio".equals(transportType)) {
            config.put("command", entry.getCommand());
            if (StrUtil.isNotBlank(entry.getArgs())) {
                try {
                    config.put("args", objectMapper.readValue(entry.getArgs(), List.class));
                } catch (Exception e) {
                    log.warn("[McpRegistry] 解析 args 失败: {}", e.getMessage());
                }
            }
            if (StrUtil.isNotBlank(entry.getEnvVars())) {
                try {
                    config.put("env", objectMapper.readValue(entry.getEnvVars(), Map.class));
                } catch (Exception e) {
                    log.warn("[McpRegistry] 解析 envVars 失败: {}", e.getMessage());
                }
            }
        }
        return serializeToJson(config);
    }

    /**
     * 根据手工导入 BO 构建 server_config JSON 字符串
     */
    private String buildManualServerConfig(McpServerManualBo bo) {
        Map<String, Object> config = new HashMap<>();
        String transportType = bo.getTransportType();
        if ("sse".equals(transportType) || "streamable_http".equals(transportType)) {
            config.put("url", bo.getEndpointUrl());
        } else if ("stdio".equals(transportType)) {
            config.put("command", bo.getCommand());
            if (CollUtil.isNotEmpty(bo.getArgs())) {
                config.put("args", bo.getArgs());
            }
            if (bo.getEnvVars() != null && !bo.getEnvVars().isEmpty()) {
                config.put("env", bo.getEnvVars());
            }
        }
        return serializeToJson(config);
    }

    /**
     * 根据平台标识查找对应适配器
     */
    private RegistrySourceAdapter findAdapter(String platform) {
        if (StrUtil.isBlank(platform)) {
            throw new ServiceException("注册源平台标识为空");
        }
        return adapters.stream()
                .filter(a -> platform.equals(a.getPlatform()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到平台 [" + platform + "] 对应的适配器"));
    }

    /**
     * 将 McpRegistryEntryDTO 转换为 KmMcpRegistryEntry 实体
     * <p>
     * JSONB 字段（args、envVars、packages、tags）需要序列化为 JSON 字符串。
     */
    private KmMcpRegistryEntry convertToEntity(McpRegistryEntryDTO dto, Long sourceId) {
        KmMcpRegistryEntry entity = new KmMcpRegistryEntry();
        entity.setSourceId(sourceId);
        entity.setExternalId(dto.getExternalId());
        entity.setEntryName(dto.getEntryName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setDescription(dto.getDescription());
        entity.setAuthor(dto.getAuthor());
        entity.setVersion(dto.getVersion());
        entity.setTransportType(dto.getTransportType());
        entity.setEndpointUrl(dto.getEndpointUrl());
        entity.setCommand(dto.getCommand());
        entity.setDnsVerified(dto.getDnsVerified());
        entity.setSourcePlatform(dto.getSourcePlatform());
        entity.setEntryStatus(StrUtil.isBlank(dto.getEntryStatus())
                ? McpRegistryConstants.STATUS_ACTIVE : dto.getEntryStatus());
        entity.setRating(dto.getRating());
        entity.setUseCount(dto.getUseCount());
        entity.setIconUrl(dto.getIconUrl());
        entity.setHomepageUrl(dto.getHomepageUrl());
        entity.setCreateTime(dto.getCreateTime());
        entity.setUpdateTime(dto.getUpdateTime());

        // JSONB 字段序列化
        entity.setArgs(serializeToJson(dto.getArgs()));
        entity.setEnvVars(serializeToJson(dto.getEnvVars()));
        entity.setPackages(serializeToJson(dto.getPackages()));
        entity.setTags(serializeToJson(dto.getTags()));

        return entity;
    }

    /**
     * 将对象序列化为 JSON 字符串（用于 JSONB 字段存储）
     * <p>
     * 序列化失败时记录警告日志，返回 null。
     */
    private String serializeToJson(Object value) {
        if (value == null) {
            return null;
        }
        // 如果已经是字符串，直接返回
        if (value instanceof String) {
            String str = (String) value;
            return StrUtil.isBlank(str) ? null : str;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[McpRegistry] JSONB 字段序列化失败: value={}, error={}", value, e.getMessage());
            return null;
        }
    }

    /**
     * 更新注册源同步状态
     */
    private void updateSyncStatus(Long sourceId, String status,
                                   LocalDateTime syncTime, Integer syncCount, String errorMsg) {
        LambdaUpdateWrapper<KmMcpRegistrySource> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KmMcpRegistrySource::getSourceId, sourceId)
                .set(KmMcpRegistrySource::getLastSyncStatus, status);

        if (syncTime != null) {
            wrapper.set(KmMcpRegistrySource::getLastSyncTime, syncTime);
        }
        if (syncCount != null) {
            wrapper.set(KmMcpRegistrySource::getLastSyncCount, syncCount);
        }
        // 成功时清空错误信息，失败时写入错误信息
        if (McpRegistryConstants.SYNC_STATUS_SUCCESS.equals(status)) {
            wrapper.set(KmMcpRegistrySource::getLastSyncError, null);
        } else if (errorMsg != null) {
            wrapper.set(KmMcpRegistrySource::getLastSyncError, errorMsg);
        }

        sourceMapper.update(null, wrapper);
    }

    /**
     * 查询已导入为 MCP Server 的 entry_id 集合
     * <p>
     * km_mcp_server 表的 source_entry_id 字段由数据库迁移脚本添加（设计文档 ALTER TABLE）。
     * 若字段尚未迁移，则捕获异常并返回空集合，不影响主流程。
     */
    private Set<Long> queryImportedEntryIds() {
        try {
            // 通过子查询从 km_mcp_server 中获取已导入的 source_entry_id 列表
            List<Object> rawIds = entryMapper.selectObjs(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KmMcpRegistryEntry>()
                            .select("entry_id")
                            .inSql("entry_id",
                                    "SELECT source_entry_id FROM km_mcp_server "
                                    + "WHERE source_entry_id IS NOT NULL AND del_flag = '0'"));
            if (CollUtil.isEmpty(rawIds)) {
                return Set.of();
            }
            return rawIds.stream()
                    .filter(id -> id != null)
                    .map(id -> Long.parseLong(id.toString()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("[McpRegistry] 查询已导入 entry_id 失败（可能 source_entry_id 字段尚未迁移）: {}", e.getMessage());
            return Set.of();
        }
    }

}
