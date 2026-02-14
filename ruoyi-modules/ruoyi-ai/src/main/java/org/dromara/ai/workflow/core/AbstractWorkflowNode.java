package org.dromara.ai.workflow.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.domain.vo.NodeParamDefinitionVo;
import org.dromara.ai.domain.vo.config.ParamDefinition;
import org.dromara.ai.service.IKmNodeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流节点抽象基类
 * 提供基于节点定义服务的公用实现,避免每个节点重复实现 getInputParamDefs
 *
 * @author Mahone
 * @date 2026-02-08
 */
@Slf4j
public abstract class AbstractWorkflowNode implements WorkflowNode {

    @Autowired(required = false)
    private IKmNodeDefinitionService nodeDefinitionService;

    /**
     * 从节点定义服务中获取输入参数定义
     * 子类可以覆盖此方法提供自定义实现
     *
     * @return 输入参数定义列表
     */
    @Override
    public List<ParamDefinition> getInputParamDefs() {
        // 如果节点定义服务不可用,返回 null(降级处理)
        if (nodeDefinitionService == null) {
            log.warn("节点定义服务不可用,无法获取参数定义: {}", getNodeType());
            return null;
        }

        try {
            // 从缓存的节点定义列表中查找当前节点类型

            KmNodeDefinitionVo definition = nodeDefinitionService.getNodeDefinitionByType(getNodeType());
            if (definition == null) {
                log.warn("未找到节点定义: {}", getNodeType());
                return null;
            }

            // 转换 NodeParamDefinitionVo 为 ParamDefinition
            List<NodeParamDefinitionVo> inputParams = definition.getInputParams();
            if (inputParams == null || inputParams.isEmpty()) {
                return null;
            }

            return inputParams.stream()
                    .map(this::convertToParamDefinition)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取节点输入参数定义失败: {}", getNodeType(), e);
            return null;
        }
    }

    /**
     * 将 NodeParamDefinitionVo 转换为 ParamDefinition
     *
     * @param vo 节点参数定义 VO
     * @return 参数定义
     */
    private ParamDefinition convertToParamDefinition(NodeParamDefinitionVo vo) {
        ParamDefinition def = new ParamDefinition();
        def.setKey(vo.getKey());
        def.setLabel(vo.getLabel());
        def.setType(vo.getType());
        def.setRequired(vo.getRequired());
        def.setDescription(vo.getDescription());

        // defaultValue 类型转换: NodeParamDefinitionVo 是 Object, ParamDefinition 是 String
        if (vo.getDefaultValue() != null) {
            def.setDefaultValue(vo.getDefaultValue().toString());
        }

        return def;
    }
}
