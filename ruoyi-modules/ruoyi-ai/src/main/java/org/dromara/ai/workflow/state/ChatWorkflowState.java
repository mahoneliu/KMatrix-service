package org.dromara.ai.workflow.state;

import lombok.Data;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.dromara.ai.workflow.core.NodeContext;
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
public class ChatWorkflowState extends AgentState implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 基础信息常量 ==========
    // 基础信息统一存储在 globalState map 中，以适配 LangGraph4j 的 AsyncNodeAction
    private static final String KEY_USER_INPUT = "userInput";
    private static final String KEY_SESSION_ID = "sessionId";
    private static final String KEY_INSTANCE_ID = "instanceId";
    private static final String KEY_USER_ID = "userId";

    /**
     * 无参构造函数
     */
    public ChatWorkflowState() {
        super(new HashMap<>());
    }

    /**
     * Map 构造函数
     */
    public ChatWorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    // SCHEMA: 定义字段的合并策略
    // nodeOutputs, globalState, error, finalResponse 均使用 last-value 语义(最新值覆盖)
    // 初始值通过 factory 提供，确保类型正确
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            "nodeOutputs", Channels.<Map<String, Object>>base(() -> new HashMap<>()),
            "error", Channels.<String>base(() -> null),
            "finalResponse", Channels.<String>base(() -> null),
            "currentNodeId", Channels.<String>base(() -> ""),
            "globalState", Channels.<Map<String, Object>>base(() -> new HashMap<>()),
            "finished", Channels.<Boolean>base(() -> false));

    // ========== 执行状态 ==========

    public String getCurrentNodeId() {
        return this.value("currentNodeId").map(Object::toString).orElse("");
    }

    public Map<String, Object> getGlobalState() {
        return this.<Map<String, Object>>value("globalState").orElseGet(HashMap::new);
    }

    public Map<String, Object> getNodeOutputs() {
        return this.<Map<String, Object>>value("nodeOutputs").orElseGet(HashMap::new);
    }

    // ========== 结果与状态 ==========

    public String getFinalResponse() {
        return this.value("finalResponse").map(Object::toString).orElse(null);
    }

    public boolean isFinished() {
        return this.<Boolean>value("finished").orElse(false);
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
    public NodeContext toNodeContext() {
        NodeContext context = new NodeContext();
        context.setSessionId(getSessionId());
        context.setInstanceId(getInstanceId());
        context.setUserId(getUserId());
        context.setGlobalState(getGlobalState());
        return context;
    }

    public String getError() {
        return this.value("error").map(Object::toString).orElse(null);
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
}
