package org.dromara.ai.app.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 中断响应视图对象
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbortResponseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 中断状态
     */
    private String status;

    /**
     * 已生成的部分内容
     */
    private String partialContent;

    /**
     * 中断时间
     */
    private LocalDateTime abortedAt;

    /**
     * 中断原因
     */
    private String abortReason;

    /**
     * 消息
     */
    private String message;

    /**
     * 构造函数（简化版）
     *
     * @param requestId 请求ID
     * @param status 中断状态
     * @param partialContent 部分内容
     * @param abortedAt 中断时间
     */
    public AbortResponseVo(String requestId, String status, String partialContent, LocalDateTime abortedAt) {
        this.requestId = requestId;
        this.status = status;
        this.partialContent = partialContent;
        this.abortedAt = abortedAt;
    }

    /**
     * 构造函数 (5参数)
     */
    public AbortResponseVo(String requestId, String status, String partialContent, LocalDateTime abortedAt, String abortReason) {
        this.requestId = requestId;
        this.status = status;
        this.partialContent = partialContent;
        this.abortedAt = abortedAt;
        this.abortReason = abortReason;
    }
}
