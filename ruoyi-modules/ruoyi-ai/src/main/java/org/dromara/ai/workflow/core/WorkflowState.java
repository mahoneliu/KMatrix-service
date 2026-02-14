package org.dromara.ai.workflow.core;

import lombok.Data;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流状态类
 * 用于在 LangGraph4j 工作流节点间传递数据
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class WorkflowState extends AgentState implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础信息常量 ==========
    // 基础信息统一存储在 globalState map 中，以适配 LangGraph4j 的 AsyncNodeAction
    public static final String KEY_INSTANCE_ID = "instanceId";
    public static final String KEY_USER_INPUT = "userInput";
    public static final String KEY_SESSION_ID = "sessionId";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_CURRENT_TIME = "currentTime";
    public static final String KEY_HISTORY_CONTEXT = "historyContext";
    public static final String KEY_DEBUG = "debug";
    public static final String KEY_SHOW_EXECUTION_INFO = "showExecutionInfo";

    public static final String KEY_NODE_OUTPUTS = "nodeOutputs";
    public static final String KEY_ERROR = "error";
    public static final String KEY_FINAL_RESPONSE = "finalResponse";
    public static final String KEY_CURRENT_NODE_ID = "currentNodeId";
    public static final String KEY_FINISHED = "finished";
    public static final String KEY_GLOBAL_STATE = "globalState";

    public WorkflowState() {
        super(new HashMap<>());
    }

    /**
     * Map 构造函数
     */
    public WorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    // SCHEMA: 定义字段的合并策略
    // nodeOutputs, globalState, error, finalResponse 均使用 last-value 语义(最新值覆盖)
    // 初始值通过 factory 提供，确保类型正确
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            KEY_NODE_OUTPUTS, Channels.<Map<String, Object>>base(() -> new HashMap<>()),
            KEY_ERROR, Channels.<String>base(() -> null),
            KEY_FINAL_RESPONSE, Channels.<String>base(() -> null),
            KEY_CURRENT_NODE_ID, Channels.<String>base(() -> ""),
            KEY_GLOBAL_STATE, Channels.<Map<String, Object>>base(() -> new HashMap<>()),
            KEY_FINISHED, Channels.<Boolean>base(() -> false));

    // ========== 执行状态 ==========

    public String getCurrentNodeId() {
        return this.value(KEY_CURRENT_NODE_ID).map(Object::toString).orElse("");
    }

    public Map<String, Object> getGlobalState() {
        return this.<Map<String, Object>>value(KEY_GLOBAL_STATE).orElseGet(HashMap::new);
    }

    public Map<String, Object> getNodeOutputs() {
        return this.<Map<String, Object>>value(KEY_NODE_OUTPUTS).orElseGet(HashMap::new);
    }

    // ========== 结果与状态 ==========

    public String getFinalResponse() {
        return this.value(KEY_FINAL_RESPONSE).map(Object::toString).orElse(null);
    }

    public boolean isFinished() {
        return this.<Boolean>value(KEY_FINISHED).orElse(false);
    }

    // ========== 辅助方法 ==========

    /**
     * 获取节点输出
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getNodeOutput(String nodeId) {
        return (Map<String, Object>) getNodeOutputs().get(nodeId);
    }

    /**
     * 从状态构造节点执行上下文
     * 注意：SseEmitter 通过闭包传递，不再在此处设置
     */
    @SuppressWarnings("unchecked")
    public NodeContext toNodeContext() {
        NodeContext context = new NodeContext();
        context.setGlobalState(getGlobalState());

        // 传递所有节点的输出数据
        Map<String, Object> nodeOutputsMap = getNodeOutputs();
        if (nodeOutputsMap != null && !nodeOutputsMap.isEmpty()) {
            Map<String, Map<String, Object>> allNodeOutputs = new HashMap<>();
            for (Map.Entry<String, Object> entry : nodeOutputsMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    allNodeOutputs.put(entry.getKey(), (Map<String, Object>) entry.getValue());
                }
            }
            context.setAllNodeOutputs(allNodeOutputs);
        }

        return context;
    }

    public String getError() {
        return this.value(KEY_ERROR).map(Object::toString).orElse(null);
    }

    // ========== 基础信息访问方法 ==========
    // 从 globalState map 中读取基础信息

    public String getUserInput() {
        Object val = getGlobalState().get(KEY_USER_INPUT);
        return val != null ? val.toString() : null;
    }

    public Long getSessionId() {
        Object value = getGlobalState().get(KEY_SESSION_ID);
        return value != null ? ((Number) value).longValue() : null;
    }

    public Long getInstanceId() {
        Object value = getGlobalState().get(KEY_INSTANCE_ID);
        return value != null ? ((Number) value).longValue() : null;
    }

    public Long getUserId() {
        Object value = getGlobalState().get(KEY_USER_ID);
        return value != null ? ((Number) value).longValue() : null;
    }

    public String getUserName() {
        Object value = getGlobalState().get(KEY_USER_NAME);
        return value != null ? value.toString() : null;
    }

    public String getCurrentTime() {
        Object value = getGlobalState().get(KEY_CURRENT_TIME);
        return value != null ? value.toString() : null;
    }

    public String getHistoryContext() {
        Object value = getGlobalState().get(KEY_HISTORY_CONTEXT);
        return value != null ? value.toString() : null;
    }

    public Boolean getDebug() {
        Object value = getGlobalState().get(KEY_DEBUG);
        return value != null ? (Boolean) value : null;
    }

    public Boolean getShowExecutionInfo() {
        Object value = getGlobalState().get(KEY_SHOW_EXECUTION_INFO);
        return value != null ? (Boolean) value : null;
    }
}
