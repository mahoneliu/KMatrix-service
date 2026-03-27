package org.dromara.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.RequestState;
import org.dromara.ai.app.domain.RequestStatus;
import org.dromara.ai.app.exception.AbortException;
import org.springframework.stereotype.Component;

/**
 * 聊天流式响应处理器
 * 用于处理流式响应、中断信号和部分内容收集
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Slf4j
@Component
public class ChatStreamHandler {

    private final RequestStateManager requestStateManager;

    public ChatStreamHandler(RequestStateManager requestStateManager) {
        this.requestStateManager = requestStateManager;
    }

    /**
     * 开始流式响应处理
     * 创建请求状态并开始处理流
     *
     * @param requestId 请求ID
     * @param sessionId 会话ID
     * @return 请求状态
     */
    public RequestState startStreamResponse(String requestId, Long sessionId) {
        RequestState state = new RequestState(requestId);
        state.setSessionId(sessionId);
        requestStateManager.put(requestId, state);
        log.debug("Started stream response for request: {}, sessionId: {}", requestId, sessionId);
        return state;
    }

    /**
     * 处理流数据块
     * 检查中断信号，收集内容
     *
     * @param requestId 请求ID
     * @param chunk 数据块
     * @throws AbortException 如果请求已被中断
     */
    public void handleStreamChunk(String requestId, String chunk) throws AbortException {
        RequestState state = requestStateManager.get(requestId);
        if (state == null) {
            log.warn("Request state not found for request: {}", requestId);
            return;
        }

        // 检查中断信号
        if (state.isAborted()) {
            throw new AbortException("Request has been aborted", "user_abort");
        }

        // 收集内容
        state.appendContent(chunk);
        log.trace("Collected chunk for request {}, total length: {}", requestId, state.getContentLength());
    }

    /**
     * 完成流式响应
     *
     * @param requestId 请求ID
     */
    public void completeStreamResponse(String requestId) {
        RequestState state = requestStateManager.get(requestId);
        if (state != null) {
            state.setStatus(RequestStatus.COMPLETED);
            log.debug("Completed stream response for request: {}", requestId);
        }
    }

    /**
     * 中止流式响应
     *
     * @param requestId 请求ID
     */
    public void abortStreamResponse(String requestId) {
        RequestState state = requestStateManager.get(requestId);
        if (state != null) {
            state.abort();
            state.setStatus(RequestStatus.ABORTED);
            log.debug("Aborted stream response for request: {}", requestId);
        }
    }

    /**
     * 处理流式响应异常
     *
     * @param requestId 请求ID
     * @param exception 异常
     */
    public void handleStreamException(String requestId, Exception exception) {
        RequestState state = requestStateManager.get(requestId);
        if (state != null) {
            state.setStatus(RequestStatus.ERROR);
            log.error("Stream error for request {}: {}", requestId, exception.getMessage(), exception);
        }
    }

    /**
     * 获取请求状态
     *
     * @param requestId 请求ID
     * @return 请求状态
     */
    public RequestState getRequestState(String requestId) {
        return requestStateManager.get(requestId);
    }

    /**
     * 获取已收集的内容
     *
     * @param requestId 请求ID
     * @return 已收集的内容
     */
    public String getCollectedContent(String requestId) {
        RequestState state = requestStateManager.get(requestId);
        if (state != null) {
            return state.getCollectedContent();
        }
        return "";
    }

    /**
     * 清理请求状态
     *
     * @param requestId 请求ID
     */
    public void cleanupRequestState(String requestId) {
        requestStateManager.remove(requestId);
        log.debug("Cleaned up request state for request: {}", requestId);
    }

    /**
     * 检查请求是否已中断
     *
     * @param requestId 请求ID
     * @return 是否已中断
     */
    public boolean isAborted(String requestId) {
        RequestState state = requestStateManager.get(requestId);
        return state != null && state.isAborted();
    }

    /**
     * 检查请求是否存在
     *
     * @param requestId 请求ID
     * @return 是否存在
     */
    public boolean exists(String requestId) {
        return requestStateManager.exists(requestId);
    }
}
