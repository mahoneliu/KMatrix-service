package org.dromara.ai.execution.registry.service;

import org.dromara.ai.execution.registry.domain.bo.McpImportBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySearchBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySourceBo;
import org.dromara.ai.execution.registry.domain.bo.McpServerManualBo;
import org.dromara.ai.execution.registry.domain.vo.McpRegistryEntryVo;
import org.dromara.ai.execution.registry.domain.vo.McpRegistrySourceVo;
import org.dromara.ai.execution.registry.domain.vo.SyncResultVo;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;
import java.util.Map;

/**
 * MCP 注册源集成 Service 接口
 *
 * @author Mahone
 */
public interface McpRegistryService {

    /**
     * 触发指定注册源的全量同步（可由定时任务或手动触发）
     *
     * @param sourceId 注册源 ID
     * @return 同步结果
     */
    SyncResultVo syncSource(Long sourceId);

    /**
     * 搜索注册源条目（支持关键词、来源平台、标签筛选，分页返回）
     *
     * @param bo 搜索请求对象
     * @return 分页条目列表
     */
    TableDataInfo<McpRegistryEntryVo> searchEntries(McpRegistrySearchBo bo);

    /**
     * 获取注册源条目详情
     *
     * @param entryId 条目 ID
     * @return 条目视图对象
     */
    McpRegistryEntryVo getEntryDetail(Long entryId);

    /**
     * 从注册源条目导入为 MCP Server 配置
     *
     * @param entryId 注册源条目 ID
     * @param bo      导入请求对象
     * @return 导入后的 MCP Server 视图对象
     */
    Object importEntry(Long entryId, McpImportBo bo);

    /**
     * 列出所有注册源配置
     *
     * @return 注册源配置列表
     */
    List<McpRegistrySourceVo> listSources();

    /**
     * 更新注册源配置（启用/禁用、同步间隔等）
     *
     * @param bo 注册源更新请求对象
     */
    void updateSource(McpRegistrySourceBo bo);

    /**
     * 删除注册源及其缓存条目
     *
     * @param sourceId 注册源 ID
     */
    void deleteSource(Long sourceId);

    /**
     * 手工添加 MCP Server
     *
     * @param bo 手工导入请求对象
     * @return 新增结果（包含 serverId、serverName、action）
     */
    Map<String, Object> addManualServer(McpServerManualBo bo);

}
