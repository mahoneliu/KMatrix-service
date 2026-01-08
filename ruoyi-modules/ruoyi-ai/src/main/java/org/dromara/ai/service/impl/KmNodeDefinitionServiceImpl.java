package org.dromara.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmNodeDefinition;
import org.dromara.ai.domain.bo.KmNodeDefinitionBo;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.domain.vo.NodeParamDefinitionVo;
import org.dromara.ai.mapper.KmNodeDefinitionMapper;
import org.dromara.ai.service.IKmNodeDefinitionService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流节点定义服务实现
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KmNodeDefinitionServiceImpl implements IKmNodeDefinitionService {

    private final KmNodeDefinitionMapper nodeDefinitionMapper;

    /**
     * 获取所有节点类型定义
     * 使用缓存避免重复读取文件
     *
     * @return 节点类型定义列表
     */
    @Override
    @Cacheable(value = "workflow:nodeDefinitions", unless = "#result == null || #result.isEmpty()")
    public List<KmNodeDefinitionVo> getNodeDefinitions() {
        try {
            // 从数据库查询所有启用的节点定义
            LambdaQueryWrapper<KmNodeDefinition> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KmNodeDefinition::getIsEnabled, "1")
                    .orderByAsc(KmNodeDefinition::getCategory)
                    .orderByAsc(KmNodeDefinition::getNodeType);

            List<KmNodeDefinition> entities = nodeDefinitionMapper.selectList(queryWrapper);

            // 转换为VO
            List<KmNodeDefinitionVo> result = entities.stream()
                    .map(this::convertToVo)
                    .collect(Collectors.toList());

            log.info("从数据库成功加载 {} 个节点类型定义", result.size());
            return result;
        } catch (Exception e) {
            log.error("加载节点定义配置失败", e);
            throw new ServiceException("加载节点定义配置失败: " + e.getMessage());
        }
    }

    /**
     * 将Entity转换为VO
     */
    private KmNodeDefinitionVo convertToVo(KmNodeDefinition entity) {
        KmNodeDefinitionVo vo = new KmNodeDefinitionVo();
        vo.setNodeDefId(entity.getNodeDefId());
        vo.setNodeType(entity.getNodeType());
        vo.setNodeLabel(entity.getNodeLabel());
        vo.setNodeIcon(entity.getNodeIcon());
        vo.setNodeColor(entity.getNodeColor());
        vo.setCategory(entity.getCategory());
        vo.setDescription(entity.getDescription());
        vo.setIsSystem(entity.getIsSystem());
        vo.setIsEnabled(entity.getIsEnabled());

        // 解析JSON参数定义
        if (StrUtil.isNotBlank(entity.getInputParams())) {
            vo.setInputParams(
                    JSONUtil.toList(entity.getInputParams(), NodeParamDefinitionVo.class));
        }
        if (StrUtil.isNotBlank(entity.getOutputParams())) {
            vo.setOutputParams(
                    JSONUtil.toList(entity.getOutputParams(), NodeParamDefinitionVo.class));
        }

        return vo;
    }

    // ========== 节点定义管理方法实现 ==========

    /**
     * 分页查询节点定义列表
     */
    @Override
    public TableDataInfo<KmNodeDefinitionVo> queryPageList(
            KmNodeDefinitionBo bo,
            PageQuery pageQuery) {
        LambdaQueryWrapper<KmNodeDefinition> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        wrapper.like(StrUtil.isNotBlank(bo.getNodeType()),
                KmNodeDefinition::getNodeType, bo.getNodeType())
                .like(StrUtil.isNotBlank(bo.getNodeLabel()),
                        KmNodeDefinition::getNodeLabel, bo.getNodeLabel())
                .eq(StrUtil.isNotBlank(bo.getCategory()),
                        KmNodeDefinition::getCategory, bo.getCategory())
                .eq(StrUtil.isNotBlank(bo.getIsSystem()),
                        KmNodeDefinition::getIsSystem, bo.getIsSystem())
                .eq(StrUtil.isNotBlank(bo.getIsEnabled()),
                        KmNodeDefinition::getIsEnabled, bo.getIsEnabled())
                .orderByAsc(KmNodeDefinition::getCategory)
                .orderByAsc(KmNodeDefinition::getNodeType);

        // 分页查询
        Page<KmNodeDefinition> page = nodeDefinitionMapper
                .selectPage(pageQuery.build(), wrapper);

        // 转换为VO Page
        Page<KmNodeDefinitionVo> voPage = new Page<>(
                page.getCurrent(), page.getSize(), page.getTotal());
        List<KmNodeDefinitionVo> voList = page.getRecords().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return TableDataInfo.build(voPage);
    }

    /**
     * 根据ID查询节点定义详情
     */
    @Override
    public KmNodeDefinitionVo getNodeDefinitionById(Long nodeDefId) {
        KmNodeDefinition entity = nodeDefinitionMapper.selectById(nodeDefId);
        if (entity == null) {
            throw new ServiceException("节点定义不存在");
        }
        return convertToVo(entity);
    }

    /**
     * 新增节点定义
     */
    @Override
    @CacheEvict(value = "workflow:nodeDefinitions", allEntries = true)
    public Long addNodeDefinition(KmNodeDefinitionBo bo) {
        // 1. 校验节点类型是否已存在
        LambdaQueryWrapper<KmNodeDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmNodeDefinition::getNodeType, bo.getNodeType());
        if (nodeDefinitionMapper.exists(wrapper)) {
            throw new ServiceException(
                    MessageUtils.message("node.type.exists", bo.getNodeType()));
        }

        // 2. 转换并保存
        KmNodeDefinition entity = BeanUtil.toBean(bo, KmNodeDefinition.class);
        entity.setVersion(1);
        entity.setIsSystem("0"); // 用户创建的节点非系统节点

        // 3. 序列化参数为JSON
        if (CollUtil.isNotEmpty(bo.getInputParams())) {
            entity.setInputParams(JSONUtil.toJsonStr(bo.getInputParams()));
        } else {
            entity.setInputParams("[]");
        }
        if (CollUtil.isNotEmpty(bo.getOutputParams())) {
            entity.setOutputParams(JSONUtil.toJsonStr(bo.getOutputParams()));
        } else {
            entity.setOutputParams("[]");
        }

        nodeDefinitionMapper.insert(entity);
        log.info("新增节点定义成功: nodeType={}, nodeDefId={}", bo.getNodeType(), entity.getNodeDefId());
        return entity.getNodeDefId();
    }

    /**
     * 复制节点定义
     */
    @Override
    @CacheEvict(value = "workflow:nodeDefinitions", allEntries = true)
    public Long copyNodeDefinition(Long nodeDefId, String newNodeType) {
        // 1. 查询原节点
        KmNodeDefinition source = nodeDefinitionMapper.selectById(nodeDefId);
        if (source == null) {
            throw new ServiceException("节点定义不存在");
        }

        // 2. 校验新节点类型
        LambdaQueryWrapper<KmNodeDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmNodeDefinition::getNodeType, newNodeType);
        if (nodeDefinitionMapper.exists(wrapper)) {
            throw new ServiceException(
                    MessageUtils.message("node.type.exists", newNodeType));
        }

        // 3. 创建副本
        KmNodeDefinition copy = new KmNodeDefinition();
        BeanUtil.copyProperties(source, copy, "nodeDefId", "nodeType", "createTime", "updateTime",
                "createBy", "updateBy");
        copy.setNodeType(newNodeType);
        copy.setNodeLabel(source.getNodeLabel() + "_副本");
        copy.setVersion(1);
        copy.setIsSystem("0"); // 复制的节点不是系统节点

        nodeDefinitionMapper.insert(copy);
        log.info("复制节点定义成功: sourceId={}, newNodeType={}, newId={}", nodeDefId, newNodeType, copy.getNodeDefId());
        return copy.getNodeDefId();
    }

    /**
     * 更新节点定义
     */
    @Override
    @CacheEvict(value = "workflow:nodeDefinitions", allEntries = true)
    public void updateNodeDefinition(KmNodeDefinitionBo bo) {
        // 1. 查询原节点
        KmNodeDefinition entity = nodeDefinitionMapper.selectById(bo.getNodeDefId());
        if (entity == null) {
            throw new ServiceException("节点定义不存在");
        }

        // 2. 系统节点禁止修改核心字段
        if ("1".equals(entity.getIsSystem())) {
            throw new ServiceException("系统节点不允许修改");
        }

        // 3. 更新字段
        BeanUtil.copyProperties(bo, entity, "nodeDefId", "version", "isSystem", "createTime",
                "createBy");

        // 4. 序列化参数
        if (CollUtil.isNotEmpty(bo.getInputParams())) {
            entity.setInputParams(JSONUtil.toJsonStr(bo.getInputParams()));
        } else {
            entity.setInputParams("[]");
        }
        if (CollUtil.isNotEmpty(bo.getOutputParams())) {
            entity.setOutputParams(JSONUtil.toJsonStr(bo.getOutputParams()));
        } else {
            entity.setOutputParams("[]");
        }

        nodeDefinitionMapper.updateById(entity);
        log.info("更新节点定义成功: nodeDefId={}, nodeType={}", entity.getNodeDefId(), entity.getNodeType());
    }

    /**
     * 删除节点定义
     */
    @Override
    @CacheEvict(value = "workflow:nodeDefinitions", allEntries = true)
    public void deleteNodeDefinition(Long nodeDefId) {
        KmNodeDefinition entity = nodeDefinitionMapper.selectById(nodeDefId);
        if (entity == null) {
            throw new ServiceException("节点定义不存在");
        }

        // 系统节点禁止删除
        if ("1".equals(entity.getIsSystem())) {
            throw new ServiceException("系统节点不允许删除");
        }

        // 硬删除
        nodeDefinitionMapper.deleteById(nodeDefId);
        log.info("删除节点定义成功: nodeDefId={}, nodeType={}", nodeDefId, entity.getNodeType());
    }

    /**
     * 批量删除节点定义
     */
    @Override
    @CacheEvict(value = "workflow:nodeDefinitions", allEntries = true)
    public void deleteNodeDefinitions(Long[] nodeDefIds) {
        for (Long nodeDefId : nodeDefIds) {
            deleteNodeDefinition(nodeDefId);
        }
    }
}
