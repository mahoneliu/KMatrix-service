package org.dromara.ai.app.exception;

/**
 * 请求中断异常
 * 用于表示流式请求被用户中断或因异常而中止
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
public class AbortException extends RuntimeException {

    /**
     * 中断原因
     */
    private String abortReason;

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public AbortException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param abortReason 中断原因
     */
    public AbortException(String message, String abortReason) {
        super(message);
        this.abortReason = abortReason;
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param abortReason 中断原因
     * @param cause 原始异常
     */
    public AbortException(String message, String abortReason, Throwable cause) {
        super(message, cause);
        this.abortReason = abortReason;
    }

    /**
     * 获取中断原因
     *
     * @return 中断原因
     */
    public String getAbortReason() {
        return abortReason;
    }

    /**
     * 设置中断原因
     *
     * @param abortReason 中断原因
     */
    public void setAbortReason(String abortReason) {
        this.abortReason = abortReason;
    }
}
