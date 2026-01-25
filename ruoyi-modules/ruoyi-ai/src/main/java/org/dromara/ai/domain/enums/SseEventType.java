package org.dromara.ai.domain.enums;

/**
 * SSE事件类型枚举
 *
 * @author Mahone
 * @date 2026-01-02
 */
public enum SseEventType {

    /**
     * 节点开始执行
     */
    NODE_START("node_start", "节点开始执行"),

    /**
     * 节点执行进度
     */
    NODE_PROGRESS("node_progress", "节点执行进度"),

    /**
     * 节点执行完成
     */
    NODE_COMPLETE("node_complete", "节点执行完成"),

    /**
     * 节点执行错误
     */
    NODE_ERROR("node_error", "节点执行错误"),

    /**
     * 节点执行详情
     */
    NODE_EXECUTION_DETAIL("node_execution_detail", "节点执行详情"),

    /**
     * 消息内容
     */
    MESSAGE("message", "消息内容"),

    /**
     * 工作流完成
     */
    WORKFLOW_COMPLETE("workflow_complete", "工作流完成"),

    /**
     * AI思考过程（流式输出）
     */
    THINKING("thinking", "AI思考过程"),

    /**
     * 对话完成（发送给前端用于结束 streaming 状态）
     */
    DONE("done", "对话完成");

    private final String eventName;
    private final String description;

    SseEventType(String eventName, String description) {
        this.eventName = eventName;
        this.description = description;
    }

    public String getEventName() {
        return eventName;
    }

    public String getDescription() {
        return description;
    }
}
