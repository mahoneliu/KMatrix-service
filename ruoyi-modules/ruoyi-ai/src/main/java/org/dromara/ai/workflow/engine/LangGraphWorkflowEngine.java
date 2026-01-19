package org.dromara.ai.workflow.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.dromara.ai.domain.enums.NodeExecutionStatus;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.config.WorkflowConfig;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.ai.workflow.factory.NodeFactory;
import org.dromara.ai.workflow.state.WorkflowState;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.qos.logback.core.util.StringUtil;

import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

/**
 * LangGraph 工作流引擎
 * 基于 LangGraph4j 实现的工作流引擎
 *
 * @author Mahone
 * @date 2026-01-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangGraphWorkflowEngine implements WorkflowEngine {

    private final IWorkflowInstanceService instanceService;
    private final NodeFactory nodeFactory;
    private final ObjectStreamStateSerializer<WorkflowState> stateSerializer = new ObjectStreamStateSerializer<>(
            WorkflowState::new);

    @Override
    public String execute(WorkflowConfig config, WorkflowState chatWorkflowState, SseEmitter emitter)
            throws Exception {
        log.info("使用 LangGraph 引擎执行工作流");

        try {
            // 1. 构建 StateGraph，将 emitter 通过闭包传递给节点
            StateGraph<WorkflowState> graph = buildGraph(config, emitter);

            // 3. 编译并执行
            var compiled = graph.compile();
            // LangGraph4j 的 invoke 方法接受 Map 参数，返回 Optional<State>
            WorkflowState finalState = compiled.invoke(chatWorkflowState.data())
                    .orElseThrow(() -> new RuntimeException("工作流执行失败：未返回结果"));

            // 4. 检查错误 - 安全处理各种类型的 error
            String errorMessage = finalState.getError();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new RuntimeException(errorMessage);
            }

            // 5. 返回最终响应
            return finalState.getFinalResponse();
        } catch (Exception e) {
            log.error("LangGraph 工作流执行失败", e);
            throw e;
        }
    }

    @Override
    public WorkflowEngineType getEngineType() {
        return WorkflowEngineType.LANGGRAPH;
    }

    @Override
    public boolean supports(WorkflowConfig config) {
        // 不实现自动选择，仅支持显式指定
        return false;
    }

    /**
     * 构建 StateGraph
     * 
     * @param emitter SSE推送器，通过闭包传递给节点执行
     */
    private StateGraph<WorkflowState> buildGraph(WorkflowConfig config, SseEmitter emitter) throws Exception {
        // 使用 ObjectStreamStateSerializer 初始化 StateGraph
        StateGraph<WorkflowState> graph = new StateGraph<>(stateSerializer);

        // 添加所有节点
        // 注意：emitter 通过闭包捕获，即使节点并行执行在不同线程，也能正确获取
        for (WorkflowConfig.NodeConfig nodeConfig : config.getNodes()) {
            final String nodeId = nodeConfig.getId();
            graph.addNode(nodeId, node_async((state) -> executeNode(nodeConfig, state, emitter)));
        }

        // 分组处理条件边：按源节点分组
        Map<String, List<WorkflowConfig.EdgeConfig>> conditionalEdgesMap = new HashMap<>();
        Map<String, List<WorkflowConfig.EdgeConfig>> conditionNodeEdgesMap = new HashMap<>();

        // 处理边
        for (WorkflowConfig.EdgeConfig edgeConfig : config.getEdges()) {
            String fromNodeId = edgeConfig.getFrom();

            // 检查源节点是否为条件节点
            WorkflowConfig.NodeConfig fromNode = config.getNodes().stream()
                    .filter(n -> n.getId().equals(fromNodeId))
                    .findFirst()
                    .orElse(null);

            boolean isConditionNode = fromNode != null && "CONDITION".equals(fromNode.getType());

            if (isConditionNode) {
                // 条件节点的所有出边都作为条件边处理
                conditionNodeEdgesMap.computeIfAbsent(fromNodeId, k -> new ArrayList<>())
                        .add(edgeConfig);
            } else if (edgeConfig.getCondition() != null) {
                // 其他节点的条件边（旧逻辑）
                conditionalEdgesMap.computeIfAbsent(fromNodeId, k -> new ArrayList<>())
                        .add(edgeConfig);
            } else {
                // 普通边
                graph.addEdge(fromNodeId, edgeConfig.getTo());
            }
        }

        // 处理条件节点的条件边
        for (Map.Entry<String, List<WorkflowConfig.EdgeConfig>> entry : conditionNodeEdgesMap.entrySet()) {
            String fromNode = entry.getKey();
            List<WorkflowConfig.EdgeConfig> edges = entry.getValue();

            // 构建路由映射表：handleId -> 目标节点ID
            // 从边的 condition 字段中提取 handleId（前端编码格式：__HANDLE__:condition-0）
            Map<String, String> routeMap = new HashMap<>();

            for (WorkflowConfig.EdgeConfig edge : edges) {
                String handleId = extractHandleId(edge.getCondition());
                if (handleId != null) {
                    routeMap.put(handleId, edge.getTo());
                    log.debug("条件节点 {} 路由映射: {} -> {}", fromNode, handleId, edge.getTo());
                } else {
                    log.warn("条件节点 {} 的边 {} 没有 handleId 信息", fromNode, edge.getTo());
                }
            }

            // 添加默认路由：如果没有 default 边，则路由到 END
            if (!routeMap.containsKey("default")) {
                routeMap.put("default", END);
                log.debug("条件节点 {} 没有 default 边，默认路由到 END", fromNode);
            }

            // 添加条件边，路由函数读取节点输出的 routeKey 字段
            graph.addConditionalEdges(
                    fromNode,
                    new AsyncEdgeAction<WorkflowState>() {
                        @Override
                        public CompletableFuture<String> apply(WorkflowState state) {
                            // 从节点输出中获取路由键（handleId）
                            Map<String, Object> nodeOutput = state.getNodeOutput(fromNode);
                            if (nodeOutput != null) {
                                Object routeKeyObj = nodeOutput.get("routeKey");
                                if (routeKeyObj != null) {
                                    String routeKey = routeKeyObj.toString();
                                    log.info("条件节点 {} 路由键: {}, 目标: {}", fromNode, routeKey, routeMap.get(routeKey));

                                    // 检查路由键是否存在于映射表中
                                    if (routeMap.containsKey(routeKey)) {
                                        return CompletableFuture.completedFuture(routeKey);
                                    } else {
                                        log.warn("条件节点 {} 路由键 {} 不存在于映射表中，使用默认路由", fromNode, routeKey);
                                    }
                                }
                            }

                            // 默认路由
                            log.warn("条件节点 {} 未找到路由键，使用默认路由", fromNode);
                            return CompletableFuture.completedFuture("default");
                        }
                    },
                    routeMap);
        }

        // 处理其他节点的条件边（旧逻辑）
        for (Map.Entry<String, List<WorkflowConfig.EdgeConfig>> entry : conditionalEdgesMap.entrySet()) {
            String fromNode = entry.getKey();
            List<WorkflowConfig.EdgeConfig> edges = entry.getValue();

            // 构建路由映射表：targetNode -> targetNode
            Map<String, String> routeMap = new HashMap<>();
            for (WorkflowConfig.EdgeConfig edge : edges) {
                routeMap.put(edge.getTo(), edge.getTo());
            }
            // 添加默认路由：当所有条件都不满足时结束工作流
            routeMap.put("default", END);

            // 添加条件边，使用复合评估逻辑
            graph.addConditionalEdges(
                    fromNode,
                    // edge_async(state -> state.data().get("next").toString()),
                    new AsyncEdgeAction<WorkflowState>() {
                        @Override
                        public CompletableFuture<String> apply(WorkflowState state) {
                            // 按顺序评估条件
                            for (WorkflowConfig.EdgeConfig edge : edges) {
                                String result = evaluateCondition(edge.getCondition(), state);
                                // 如果条件满足（evaluateCondition 返回了预期值而不是 "default"），则返回目标节点ID
                                String expectedValue = extractExpectedValue(edge.getCondition());
                                if (expectedValue != null && expectedValue.equals(result)) {
                                    return CompletableFuture.completedFuture(edge.getTo());
                                }
                            }
                            // 所有条件都不满足，返回默认路由（END）
                            return CompletableFuture.completedFuture("default");
                        }
                    },
                    routeMap);
        }

        // 设置入口点（从 START 节点到第一个节点）
        graph.addEdge(START, config.getEntryPoint());

        // 找出所有终端节点（没有出边的节点）并连接到 END
        // 注意: 工作流配置已经在保存时通过 validate() 方法校验过,确保有且仅有一个 END 节点
        WorkflowConfig.NodeConfig endNode = config.getNodes().stream()
                .filter(node -> "END".equals(node.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("工作流必须包含 END 节点"));

        // 将 END 节点连接到 LangGraph 的 END
        log.info("将 END 节点 {} 连接到 LangGraph END", endNode.getId());
        graph.addEdge(endNode.getId(), END);

        return graph;
    }

    /**
     * 从边的 condition 字段中提取 handleId
     * 前端编码格式：__HANDLE__:condition-0 或 __HANDLE__:default
     * 
     * @param condition 边的条件字段
     * @return handleId，如果没有则返回 null
     */
    private String extractHandleId(String condition) {
        if (condition != null && condition.startsWith("__HANDLE__:")) {
            return condition.substring("__HANDLE__:".length());
        }
        return null;
    }

    /**
     * 执行节点
     * 
     * @param emitter 通过闭包传递的SSE推送器，确保并行节点也能正确获取
     */
    private Map<String, Object> executeNode(
            WorkflowConfig.NodeConfig nodeConfig,
            WorkflowState state,
            SseEmitter emitter) {

        // 从 state 构造 NodeContext（提升作用域以便catch块访问）
        NodeContext context = state.toNodeContext();

        // 设置 SseEmitter 到 context（emitter 通过参数传递，支持并行节点）
        context.setSseEmitter(emitter);

        // 检查是否为调试模式
        boolean isDebug = state.getDebug();

        Long executionId = null;
        long duration = 0;
        // 记录开始时间和节点名称
        String nodeName = nodeConfig.getName() != null ? nodeConfig.getName() : "";
        long startTime = System.currentTimeMillis();
        context.setNodeName(nodeName);
        context.setStartTime(startTime);

        try {
            // 记录当前节点（不再直接修改 state，而是稍后通过返回 Map 更新）
            String currentNodeId = nodeConfig.getId();

            // 调试模式：不更新数据库
            if (!isDebug) {
                instanceService.updateCurrentNode(state.getInstanceId(), currentNodeId);
            }

            // 创建节点实例
            WorkflowNode node = nodeFactory.createNode(nodeConfig.getType());
            nodeName = StringUtil.isNullOrEmpty(nodeName) ? node.getNodeName() : nodeName;

            // 准备输入参数
            Map<String, Object> inputs = resolveInputs(nodeConfig.getInputs(), state);

            // 设置节点配置和输入
            context.setNodeConfig(nodeConfig.getConfig() != null ? nodeConfig.getConfig() : new HashMap<>());
            context.setNodeInputs(inputs);

            // 创建节点执行记录（调试模式：不写数据库）
            if (!isDebug) {
                executionId = instanceService.createNodeExecution(
                        state.getInstanceId(), nodeConfig.getId(), nodeConfig.getType(), inputs);
            }

            // 执行节点
            NodeOutput output = node.execute(context);

            // 计算执行耗时
            duration = System.currentTimeMillis() - startTime;

            // 保存输出到本地变量（不直接修改 state，通过返回 Map 更新）
            Map<String, Object> nodeOutputs = new HashMap<>(state.getNodeOutputs());
            nodeOutputs.put(nodeConfig.getId(), output.getOutputs());
            Map<String, Object> globalState = context.getGlobalState();

            // 更新节点执行记录（调试模式：不写数据库）
            if (!isDebug) {
                instanceService.updateNodeExecution(executionId, NodeExecutionStatus.COMPLETED, output.getOutputs(),
                        nodeName, duration);
                // 更新全局状态到实例
                instanceService.updateGlobalState(state.getInstanceId(), globalState);
            }

            // 发送节点执行详情事件
            Map<String, Object> executionDetail = new HashMap<>();
            executionDetail.put("nodeName", nodeName);
            executionDetail.put("nodeType", nodeConfig.getType());
            executionDetail.put("inputs", inputs);
            executionDetail.put("outputs", output.getOutputs());
            executionDetail.put("durationMs", duration);

            // 添加 token 使用统计(如果有)
            Map<String, Object> tokenUsage = context.getTokenUsage();
            if (tokenUsage != null && !tokenUsage.isEmpty()) {
                executionDetail.put("tokenUsage", tokenUsage);
            }

            sendSseEvent(context.getSseEmitter(), SseEventType.NODE_EXECUTION_DETAIL, executionDetail);

            // 检查是否结束（准备状态更新）
            boolean finished = output.isFinished();
            String finalResponse = null;
            if (finished) {
                finalResponse = (String) output.getOutput("finalResponse");
            }

            // 返回更新后的状态（LangGraph使用map合并）
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentNodeId", currentNodeId);
            updates.put("nodeOutputs", nodeOutputs);
            updates.put("globalState", globalState);
            updates.put("finished", finished);
            if (finalResponse != null) {
                updates.put("finalResponse", finalResponse);
            }

            return updates;

        } catch (Exception e) {
            log.error("节点执行失败: {}", nodeConfig.getId(), e);
            duration = System.currentTimeMillis() - startTime;

            // 更新失败记录（调试模式：不写数据库）
            if (!isDebug) {
                instanceService.updateNodeExecution(executionId, NodeExecutionStatus.FAILED, null,
                        nodeName, duration);
            }

            sendSseEvent(context.getSseEmitter(), SseEventType.NODE_ERROR,
                    Map.of("error", e.getMessage(), "nodeId", nodeConfig.getId()));

            Map<String, Object> updates = new HashMap<>();
            updates.put("error", e.getMessage());
            updates.put("finished", true);
            return updates;
        }
    }

    /**
     * 解析输入参数
     */
    private Map<String, Object> resolveInputs(Map<String, Object> inputDefs, WorkflowState state) {
        Map<String, Object> inputs = new HashMap<>();

        if (inputDefs == null) {
            return inputs;
        }

        for (Map.Entry<String, Object> entry : inputDefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 解析表达式 ${nodeId.outputKey} 或 ${globalKey}
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("${") && strValue.endsWith("}")) {
                    value = resolveExpression(strValue, state);
                }
            }

            inputs.put(key, value);
        }

        return inputs;
    }

    /**
     * 解析表达式
     */
    private Object resolveExpression(String expression, WorkflowState state) {
        String expr = expression.substring(2, expression.length() - 1);

        // 解析 nodeId.outputKey
        String[] parts = expr.split("\\.");
        if (parts.length == 2) {
            String nodeId = parts[0];
            String outputKey = parts[1];

            Map<String, Object> outputs = state.getNodeOutput(nodeId);
            if (outputs != null) {
                return outputs.get(outputKey);
            }
        }

        // 尝试从全局状态获取
        return state.getGlobalState().get(expr);
    }

    /**
     * 从条件表达式中提取期望值
     * 例如：从 "intent == 'greeting'" 中提取 "greeting"
     */
    private String extractExpectedValue(String condition) {
        try {
            String[] parts = condition.trim().split("\\s+");
            if (parts.length >= 3) {
                return parts[2].replaceAll("^['\"]|['\"]$", "");
            }
        } catch (Exception e) {
            log.error("提取条件期望值失败: {}", condition, e);
        }
        return null;
    }

    /**
     * 计算条件表达式
     */
    private String evaluateCondition(String condition, WorkflowState state) {
        // 支持的格式: ${intent} == 'greeting'
        try {
            String[] parts = condition.trim().split("\\s+");
            if (parts.length < 3) {
                return "default";
            }

            String variable = parts[0];
            String operator = parts[1];
            String expectedValue = parts[2].replaceAll("^['\"]|['\"]$", "");

            // 从状态中获取变量值
            Object value = state.getGlobalState().get(variable);
            if (value == null) {
                // 尝试从最近的节点输出获取
                String currentNodeId = state.getCurrentNodeId();
                Map<String, Object> outputs = state.getNodeOutput(currentNodeId);
                if (outputs != null) {
                    value = outputs.get(variable);
                }
            }

            if (value == null) {
                return "default";
            }

            String actualValue = value.toString();

            // 执行比较
            boolean conditionMet = false;
            switch (operator) {
                case "==":
                case "equals":
                    conditionMet = actualValue.equals(expectedValue);
                    break;
                case "!=":
                case "notEquals":
                    conditionMet = !actualValue.equals(expectedValue);
                    break;
                case "contains":
                    conditionMet = actualValue.contains(expectedValue);
                    break;
                default:
                    conditionMet = false;
            }

            // 返回路由键：条件满足时返回期望值（用于匹配路由表），否则返回 "default"
            return conditionMet ? expectedValue : "default";
        } catch (Exception e) {
            log.error("条件计算失败: {}", condition, e);
            return "default";
        }
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, SseEventType eventType, Map<String, Object> data) {
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventType.getEventName())
                    .data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }
}
