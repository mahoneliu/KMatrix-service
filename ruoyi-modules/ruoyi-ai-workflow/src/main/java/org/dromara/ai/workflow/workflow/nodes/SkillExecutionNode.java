package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.core.IToolProvider;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能节点
 * 独立执行指定的技能（可能包含单个或多个底层工具组合）
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Slf4j
@RequiredArgsConstructor
@Component(NodeTypeConstants.SKILL)
public class SkillExecutionNode extends AbstractWorkflowNode {

    private final IToolProvider toolProviderService;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行SKILL节点");
        NodeOutput output = new NodeOutput();

        // 1. 获取选中的技能配置
        Long skillId = context.getConfigAsLong(NodeConfigConstants.CFG_SKILL_ID);
        if (skillId == null) {
            throw new RuntimeException("技能节点未正确配置技能ID (skillId)");
        }

        // 2. 解析技能绑定的底层工具
        List<ToolBinding> bindings = toolProviderService.resolveSkillInnerBindings(skillId);
        if (bindings.isEmpty()) {
            throw new RuntimeException("技能未绑定任何可用工具, skillId: " + skillId);
        }

        // 3. 构建参数
        Map<String, Object> inputs = context.getNodeInputs();
        String argumentsStr = "{}";
        if (inputs != null && !inputs.isEmpty()) {
            argumentsStr = JSONUtil.toJsonStr(inputs);
        }

        // 4. 执行技能内的底层工具
        // 目前策略：顺序执行技能内的所有工具，若只有一个工具则直接返回该工具结果。
        // 如果有多个工具，合并结果。
        log.info("开始执行技能 [skillId={}], 包含工具数: {}, 输入参数: {}", skillId, bindings.size(), argumentsStr);
        
        StringBuilder combinedText = new StringBuilder();
        Map<String, Object> combinedResult = new HashMap<>();
        
        for (ToolBinding binding : bindings) {
            log.info("执行技能内底层工具 [{}]", binding.getToolName());
            try {
                String resultStr = binding.getExecutor().execute(argumentsStr);
                log.info("底层工具 [{}] 执行成功", binding.getToolName());
                
                // 将结果追加和合并
                combinedText.append(resultStr).append("\n");
                
                if (StrUtil.isNotBlank(resultStr) && JSONUtil.isTypeJSONObject(resultStr)) {
                    JSONObject jsonObj = JSONUtil.parseObj(resultStr);
                    jsonObj.forEach(combinedResult::put);
                }
            } catch (Exception e) {
                log.error("执行技能内底层工具 [{}] 时发生异常", binding.getToolName(), e);
                throw new RuntimeException("技能执行失败, 工具 [" + binding.getToolName() + "] 报错: " + e.getMessage(), e);
            }
        }

        // 5. 将结果放入输出
        output.addOutput(NodeIOConstants.OUTPUT_RESULT, combinedResult.isEmpty() ? combinedText.toString().trim() : combinedResult);
        output.addOutput(NodeIOConstants.OUTPUT_TEXT, combinedText.toString().trim());

        // 如果只有单一工具的结果，且是扁平的JSONObject，直接展开作为出参
        if (bindings.size() == 1 && !combinedResult.isEmpty()) {
            combinedResult.forEach(output::addOutput);
        }

        return output;
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.SKILL;
    }

    @Override
    public String getNodeName() {
        return "技能节点";
    }
}
