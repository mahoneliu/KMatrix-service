package org.dromara.ai.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.enums.NodeExecutionStatus;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.config.WorkflowConfig;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.ai.workflow.factory.NodeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行器
 * 基于配置动态执行工作流
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WorkflowExecutor {

    private final NodeFactory nodeFactory;
    private final IWorkflowInstanceService instanceService;
    private final ObjectMapper objectMapper;

    /**
     * 执行工作流
     *
     * @param app       应用信息
     * @param sessionId 会话ID
     * @param userInput 用户输入
     * @param emitter   SSE推送器
     * @param userId    用户ID
     * @return 最终响应
     */
    public String executeWorkflow(KmAppVo app, Long sessionId, String userInput,
            SseEmitter emitter, Long userId) throws Exception {

        // 1. 解析工作流配置
        WorkflowConfig config = objectMapper.readValue(app.getDslData(), WorkflowConfig.class);
        if (config == null || config.getNodes() == null) {
            throw new RuntimeException("工作流配置无效");
        }

        // 2. 创建工作流实例
        Long instanceId = instanceService.createInstance(app.getAppId(), sessionId, app.getDslData());

        // 3. 初始化上下文
        NodeContext context = new NodeContext();
        context.setApp(app);
        context.setSessionId(sessionId);
        context.setInstanceId(instanceId);
        context.setUserId(userId);
        context.setSseEmitter(emitter);
        context.setGlobalValue("userInput", userInput);

        String finalResponse = null;

        try {
            // 4. 执行节点
            finalResponse = executeNodes(config, context, instanceId, emitter);

            // 5. 标记实例完成
            instanceService.completeInstance(instanceId);

            // 6. 发送 done 事件（携带 sessionId）
            sendSseEvent(emitter, SseEventType.DONE, Map.of(
                    "sessionId", context.getSessionId().toString()));

        } catch (Exception e) {
            // 保存错误信息
            instanceService.failInstance(instanceId, e.getMessage());

            // 发送错误事件
            sendSseEvent(emitter, SseEventType.NODE_ERROR, Map.of(
                    "error", e.getMessage()));

            throw e;
        }

        return finalResponse;
    }

    /**
     * 执行所有节点
     */
    private String executeNodes(WorkflowConfig config, NodeContext context,
            Long instanceId, SseEmitter emitter) throws Exception {

        String currentNodeId = config.getEntryPoint();
        Map<String, NodeOutput> nodeOutputs = new HashMap<>();
        String finalResponse = null;

        while (currentNodeId != null) {
            // 查找节点配置
            WorkflowConfig.NodeConfig nodeConfig = findNodeConfig(config, currentNodeId);
            if (nodeConfig == null) {
                log.error("节点配置不存在: {}", currentNodeId);
                break;
            }

            // 更新当前节点
            instanceService.updateCurrentNode(instanceId, currentNodeId);

            // 创建节点实例
            WorkflowNode node = nodeFactory.createNode(nodeConfig.getType());

            // 解析输入参数
            Map<String, Object> inputs = resolveInputs(nodeConfig.getInputs(), nodeOutputs, context);

            // 设置节点配置
            context.setNodeConfig(nodeConfig.getConfig() != null ? nodeConfig.getConfig() : new HashMap<>());

            // 设置节点输入
            context.setNodeInputs(inputs);

            // 创建节点执行记录
            Long executionId = instanceService.createNodeExecution(
                    instanceId, currentNodeId, nodeConfig.getType(), inputs);

            // 发送节点开始事件（仅发送节点名称）
            String nodeName = nodeConfig.getName() != null ? nodeConfig.getName() : node.getNodeName();
            sendSseEvent(emitter, SseEventType.NODE_START, Map.of("nodeName", nodeName));

            try {
                // 执行节点
                NodeOutput output = node.execute(context);

                // 保存输出
                nodeOutputs.put(currentNodeId, output);

                // 更新节点执行记录
                instanceService.updateNodeExecution(executionId, NodeExecutionStatus.COMPLETED, output.getOutputs());

                // 更新全局状态
                instanceService.updateGlobalState(instanceId, context.getGlobalState());

                // 发送节点完成事件（仅发送节点名称）
                sendSseEvent(emitter, SseEventType.NODE_COMPLETE, Map.of("nodeName", nodeName));

                // 检查是否结束
                if (output.isFinished()) {
                    finalResponse = (String) output.getOutput("finalResponse");
                    break;
                }

                // 查找下一个节点
                currentNodeId = findNextNode(config, currentNodeId, output, context);

            } catch (Exception e) {
                // 更新节点执行记录为失败
                Map<String, Object> errorOutput = Map.of("error", e.getMessage());
                instanceService.updateNodeExecution(executionId, NodeExecutionStatus.FAILED, errorOutput);

                throw e;
            }
        }

        return finalResponse;
    }

    /**
     * 查找节点配置
     */
    private WorkflowConfig.NodeConfig findNodeConfig(WorkflowConfig config, String nodeId) {
        return config.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析输入参数
     */
    private Map<String, Object> resolveInputs(Map<String, Object> inputDefs,
            Map<String, NodeOutput> nodeOutputs,
            NodeContext context) {
        Map<String, Object> inputs = new HashMap<>();

        if (inputDefs == null) {
            return inputs;
        }

        for (Map.Entry<String, Object> entry : inputDefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 解析表达式 ${nodeId.outputKey}
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("${") && strValue.endsWith("}")) {
                    value = resolveExpression(strValue, nodeOutputs, context);
                }
            }

            inputs.put(key, value);
        }

        return inputs;
    }

    /**
     * 解析表达式
     */
    private Object resolveExpression(String expression, Map<String, NodeOutput> nodeOutputs,
            NodeContext context) {
        // 去掉 ${ 和 }
        String expr = expression.substring(2, expression.length() - 1);

        // 解析 nodeId.outputKey
        String[] parts = expr.split("\\.");
        if (parts.length == 2) {
            String nodeId = parts[0];
            String outputKey = parts[1];

            NodeOutput output = nodeOutputs.get(nodeId);
            if (output != null) {
                return output.getOutput(outputKey);
            }
        }

        // 尝试从全局状态获取
        return context.getGlobalValue(expr);
    }

    /**
     * 查找下一个节点
     */
    private String findNextNode(WorkflowConfig config, String currentNodeId,
            NodeOutput output, NodeContext context) {
        // 如果输出中指定了下一个节点(条件分支)
        if (output.getNextNode() != null) {
            return output.getNextNode();
        }

        // 查找边配置
        for (WorkflowConfig.EdgeConfig edge : config.getEdges()) {
            if (edge.getFrom().equals(currentNodeId)) {
                // 如果有条件，检查条件是否满足
                if (edge.getCondition() != null) {
                    if (evaluateCondition(edge.getCondition(), output, context)) {
                        return edge.getTo();
                    }
                } else {
                    // 没有条件，直接返回
                    return edge.getTo();
                }
            }
        }

        return null;
    }

    /**
     * 计算条件表达式
     */
    private boolean evaluateCondition(String condition, NodeOutput output, NodeContext context) {
        // 支持的格式: ${intent} == 'greeting'
        // 或者简化格式: intent == 'greeting'
        try {
            // 去掉空格
            condition = condition.trim();

            // 解析条件: 变量 运算符 值
            String[] parts = condition.split("\\s+");
            if (parts.length < 3) {
                log.warn("条件表达式格式不正确: {}", condition);
                return false;
            }

            String variable = parts[0];
            String operator = parts[1];
            String expectedValue = parts[2];

            // 去掉引号
            expectedValue = expectedValue.replaceAll("^['\"]|['\"]$", "");

            // 获取变量值
            String actualValue = null;

            // 尝试从全局状态获取
            Object globalValue = context.getGlobalValue(variable);
            if (globalValue != null) {
                actualValue = globalValue.toString();
            }

            // 尝试从节点输出获取
            if (actualValue == null && output != null) {
                Object outputValue = output.getOutput(variable);
                if (outputValue != null) {
                    actualValue = outputValue.toString();
                }
            }

            if (actualValue == null) {
                log.warn("条件变量未找到: {}", variable);
                return false;
            }

            // 执行比较
            switch (operator) {
                case "==":
                case "equals":
                    return actualValue.equals(expectedValue);
                case "!=":
                case "notEquals":
                    return !actualValue.equals(expectedValue);
                case "contains":
                    return actualValue.contains(expectedValue);
                default:
                    log.warn("不支持的运算符: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            log.error("条件计算失败: {}", condition, e);
            return false;
        }
    }

    /**
     * 发送SSE事件
     */
    private void sendSseEvent(SseEmitter emitter, SseEventType eventType, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType.getEventName())
                    .data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }
}
