package org.dromara.ai.workflow.workflow.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.api.domain.vo.config.ParamDefinition;
import org.dromara.ai.workflow.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.workflow.domain.vo.NodeParamDefinitionVo;
import org.dromara.ai.workflow.service.IKmNodeDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流节点抽象基类
 * <p>
 * 提供所有节点的公共基础能力：从节点定义服务获取输入参数定义。
 * <p>
 * AI 类节点（需要调用大模型）应继承 {@link AbstractAiWorkflowNode}，
 * 它在此基础上额外提供模型加载、对话配置处理、流式调用、token 统计等能力。
 *
 * @author Mahone
 * @date 2026-02-08
 */
@Slf4j
public abstract class AbstractWorkflowNode implements WorkflowNode {

    @Autowired(required = false)
    private IKmNodeDefinitionService nodeDefinitionService;

    /**
     * 从节点定义服务中获取输入参数定义，用于自动类型转换
     */
    @Override
    public List<ParamDefinition> getInputParamDefs() {
        if (nodeDefinitionService == null) {
            log.warn("节点定义服务不可用,无法获取参数定义: {}", getNodeType());
            return null;
        }
        try {
            KmNodeDefinitionVo definition = nodeDefinitionService.getNodeDefinitionByType(getNodeType());
            if (definition == null) {
                log.warn("未找到节点定义: {}", getNodeType());
                return null;
            }
            List<NodeParamDefinitionVo> inputParams = definition.getInputParams();
            if (inputParams == null || inputParams.isEmpty()) {
                return null;
            }
            return inputParams.stream().map(this::convertToParamDefinition).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取节点输入参数定义失败: {}", getNodeType(), e);
            return null;
        }
    }

    private ParamDefinition convertToParamDefinition(NodeParamDefinitionVo vo) {
        ParamDefinition def = new ParamDefinition();
        def.setKey(vo.getKey());
        def.setLabel(vo.getLabel());
        def.setType(vo.getType());
        def.setRequired(vo.getRequired());
        def.setDescription(vo.getDescription());
        if (vo.getDefaultValue() != null) {
            def.setDefaultValue(vo.getDefaultValue().toString());
        }
        return def;
    }
}
