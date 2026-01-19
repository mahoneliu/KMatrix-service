package org.dromara.ai.workflow.nodes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.condition.ConditionBranch;
import org.dromara.ai.workflow.condition.ConditionEvaluator;
import org.dromara.ai.workflow.condition.ConditionGroup;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.ai.workflow.state.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件判断节点
 * 支持结构化条件配置，多分支条件判断
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("CONDITION")
@RequiredArgsConstructor
public class ConditionNode implements WorkflowNode {

    public static final String KEY_MATCHED_BRANCH = "matchedBranch";
    public static final String KEY_BRANCH_INDEX = "branchIndex";

    private final ConditionEvaluator conditionEvaluator;
    private final ObjectMapper objectMapper;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 CONDITION 节点");

        NodeOutput output = new NodeOutput();

        // 从节点配置中获取分支列表
        Map<String, Object> config = context.getNodeConfig();
        List<ConditionBranch> branches = parseBranches(config);

        // 获取当前工作流状态
        WorkflowState state = buildWorkflowState(context);

        // 评估条件分支
        String matchedBranch = conditionEvaluator.evaluateBranches(branches, state);

        // 获取匹配分支的 handleId
        String handleId = "default";
        int branchIndex = -1;
        for (int i = 0; i < branches.size(); i++) {
            if (branches.get(i).getName().equals(matchedBranch)) {
                branchIndex = i;
                // 使用分支的 handleId，如果没有则生成默认值
                handleId = branches.get(i).getHandleId();
                if (handleId == null || handleId.isEmpty()) {
                    handleId = "condition-" + i;
                }
                break;
            }
        }

        // 保存输出：使用 handleId 作为路由键
        // 路由函数将根据 handleId 找到对应的边
        output.addOutput(KEY_MATCHED_BRANCH, matchedBranch);
        output.addOutput(KEY_BRANCH_INDEX, branchIndex);
        output.addOutput("routeKey", handleId); // 输出 handleId 作为路由键

        log.info("CONDITION 节点执行完成, matchedBranch={}, branchIndex={}, routeKey={}",
                matchedBranch, branchIndex, handleId);
        return output;
    }

    /**
     * 解析条件分支配置
     */
    @SuppressWarnings("unchecked")
    private List<ConditionBranch> parseBranches(Map<String, Object> config) {
        if (config == null) {
            return new ArrayList<>();
        }

        Object branchesObj = config.get("branches");
        if (branchesObj == null) {
            // 兼容旧版配置
            return parseOldConfig(config);
        }

        try {
            // 使用 ObjectMapper 进行类型转换
            return objectMapper.convertValue(branchesObj, new TypeReference<List<ConditionBranch>>() {
            });
        } catch (Exception e) {
            log.error("解析条件分支配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 兼容旧版配置格式
     * 旧格式: { conditions: [{ expression: "...", targetNodeId: "..." }] }
     */
    @SuppressWarnings("unchecked")
    private List<ConditionBranch> parseOldConfig(Map<String, Object> config) {
        List<ConditionBranch> branches = new ArrayList<>();

        Object conditionsObj = config.get("conditions");
        if (conditionsObj instanceof List<?> conditionsList) {
            for (int i = 0; i < conditionsList.size(); i++) {
                Object item = conditionsList.get(i);
                if (item instanceof Map<?, ?> conditionMap) {
                    String expression = (String) conditionMap.get("expression");
                    if (expression != null && !expression.isEmpty()) {
                        ConditionBranch branch = new ConditionBranch();
                        branch.setName("branch_" + i);
                        branch.setHandleId("condition-" + i);

                        // 将表达式转换为简单的条件组
                        ConditionGroup group = new ConditionGroup();
                        group.setLogicalOperator("AND");
                        // 旧版表达式不做解析，直接跳过（需要用户重新配置）
                        log.warn("检测到旧版条件配置，建议重新配置条件节点: {}", expression);

                        branch.setCondition(group);
                        branches.add(branch);
                    }
                }
            }
        }

        return branches;
    }

    /**
     * 构建工作流状态对象（用于条件评估）
     */
    @SuppressWarnings("unchecked")
    private WorkflowState buildWorkflowState(NodeContext context) {
        Map<String, Object> initData = new HashMap<>();
        initData.put(WorkflowState.KEY_GLOBAL_STATE, context.getGlobalState());

        // 转换节点输出格式
        Map<String, Object> nodeOutputs = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : context.getAllNodeOutputs().entrySet()) {
            nodeOutputs.put(entry.getKey(), entry.getValue());
        }
        initData.put(WorkflowState.KEY_NODE_OUTPUTS, nodeOutputs);

        return new WorkflowState(initData);
    }

    @Override
    public String getNodeType() {
        return "CONDITION";
    }

    @Override
    public String getNodeName() {
        return "条件判断";
    }
}
