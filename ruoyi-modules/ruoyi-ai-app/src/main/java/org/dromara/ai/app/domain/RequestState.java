package org.dromara.ai.app.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 请求状态管理类
 * 用于跟踪流式请求的处理状态和已收集的内容
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Data
public class RequestState {

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 请求状态
     */
    private RequestStatus status;

    /**
     * 已收集的内容
     */
    private StringBuilder collectedContent;

    /**
     * 是否已中断（volatile 确保线程可见性）
     */
    private volatile boolean aborted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造函数
     *
     * @param requestId 请求ID
     */
    public RequestState(String requestId) {
        this.requestId = requestId;
        this.status = RequestStatus.PROCESSING;
        this.collectedContent = new StringBuilder();
        this.aborted = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 标记请求为已中断
     */
    public void abort() {
        this.aborted = true;
    }

    /**
     * 追加内容
     *
     * @param chunk 内容块
     */
    public void appendContent(String chunk) {
        if (chunk != null) {
            collectedContent.append(chunk);
        }
    }

    /**
     * 获取已收集的内容
     *
     * @return 已收集的内容
     */
    public String getCollectedContent() {
        return collectedContent.toString();
    }

    /**
     * 检查是否已中断
     *
     * @return 是否已中断
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * 获取内容长度
     *
     * @return 内容长度
     */
    public int getContentLength() {
        return collectedContent.length();
    }
}
