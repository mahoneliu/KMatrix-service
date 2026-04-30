package org.dromara.ai.api.enums;

import lombok.Getter;

/**
 * SSE事件类型枚举
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Getter
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
    DONE("done", "对话完成"),

    /**
     * 引用信息
     */
    CITATION("citation", "引用信息"),

    /**
     * 工具调用跟踪（LLM 请求调用工具跟踪 ）
     */
    TOOL_TRACE("TOOL_TRACE", "工具调用跟踪"),

    /**
     * 会话信息更新
     */
    SESSION_UPDATE("session_update", "会话信息更新");

    private final String eventName;
    private final String description;

    SseEventType(String eventName, String description) {
        this.eventName = eventName;
        this.description = description;
    }

}
