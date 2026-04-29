package org.dromara.ai.workflow.workflow.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeRouteConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.nodes.condition.ConditionEvaluator;
import org.dromara.ai.workflow.workflow.nodes.condition.ConditionGroup;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环节点
 * 使用 ConditionEvaluator 评估条件。条件满足时路由到继续 (continue)，否则路由到退出 (exit)。
 *
 * @author Mahone
 * @date 2026-03-22
 */
@Slf4j
@Component(NodeTypeConstants.LOOP)
@RequiredArgsConstructor
public class LoopNode extends AbstractWorkflowNode {

    public static final String KEY_ITERATION_COUNT = "iterationCount";
    public static final String ROUTE_CONTINUE = NodeRouteConstants.LOOP_ROUTE_CONTINUE;
    public static final String ROUTE_EXIT = NodeRouteConstants.LOOP_ROUTE_EXIT;
    public static final int DEFAULT_MAX_ITERATIONS = 50;

    private final ConditionEvaluator conditionEvaluator;
    private final ObjectMapper objectMapper;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 LOOP 节点");

        NodeOutput output = new NodeOutput();
        Map<String, Object> config = context.getNodeConfig();

        // 获取或初始化迭代次数
        String iterKey = "LOOP_ITERATIONS_" + context.getNodeId();
        Map<String, Object> globalState = context.getGlobalState();
        int iterationCount = 0;
        if (globalState.containsKey(iterKey)) {
            iterationCount = (Integer) globalState.get(iterKey);
        }

        // 最大迭代次数保护
        int maxIterations = DEFAULT_MAX_ITERATIONS;
        if (config != null && config.containsKey(NodeConfigConstants.CFG_LOOP_MAX_ITERATIONS) && config.get(NodeConfigConstants.CFG_LOOP_MAX_ITERATIONS) != null) {
            maxIterations = Integer.parseInt(String.valueOf(config.get(NodeConfigConstants.CFG_LOOP_MAX_ITERATIONS)));
        }

        iterationCount++;

        log.info("LOOP节点 {} 当前迭代次数: {} / {}", context.getNodeId(), iterationCount, maxIterations);

        // 存回 global state 中
        globalState.put(iterKey, iterationCount);

        // 解析条件
        ConditionGroup conditionGroup = null;
        if (config != null) {
            Object conditionObj = config.get(NodeConfigConstants.CFG_LOOP_CONDITION);
            if (conditionObj != null) {
                try {
                    conditionGroup = objectMapper.convertValue(conditionObj, ConditionGroup.class);
                } catch (Exception e) {
                    log.error("解析循环条件配置失败: {}", e.getMessage(), e);
                }
            }
        }

        WorkflowState state = buildWorkflowState(context);

        boolean continueLoop = false;
        if (iterationCount > maxIterations) {
            log.error("LOOP节点 {} 达到最大迭代次数 {}, 强制抛出异常", context.getNodeId(), maxIterations);
            throw new RuntimeException("达到最大循环迭代次数: " + maxIterations + ", 工作流已被强制终止以防止死循环");
        } else {
            if (conditionGroup != null) {
                // 评估条件组，若满足则继续循环
                continueLoop = conditionEvaluator.evaluateGroup(conditionGroup, state);
            }
        }

        String routeKey = continueLoop ? ROUTE_CONTINUE : ROUTE_EXIT;

        if (!continueLoop) {
            // 如果跳出循环，重置迭代计次器（考虑到未来可能有同一工作流中的不同批次运行等情况）
            globalState.remove(iterKey);
        }

        output.addOutput(KEY_ITERATION_COUNT, iterationCount);
        output.addOutput(NodeRouteConstants.OUTPUT_ROUTE_KEY, routeKey);

        log.info("LOOP 节点执行完成, iterationCount={}, continueLoop={}, routeKey={}", iterationCount, continueLoop,
                routeKey);
        return output;
    }

    private WorkflowState buildWorkflowState(NodeContext context) {
        Map<String, Object> initData = new HashMap<>();
        initData.put(WorkflowState.KEY_GLOBAL_STATE, context.getGlobalState());
        Map<String, Object> nodeOutputs = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : context.getAllNodeOutputs().entrySet()) {
            nodeOutputs.put(entry.getKey(), entry.getValue());
        }
        initData.put(WorkflowState.KEY_NODE_OUTPUTS, nodeOutputs);
        return new WorkflowState(initData);
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.LOOP;
    }

    @Override
    public String getNodeName() {
        return "循环节点";
    }
}
