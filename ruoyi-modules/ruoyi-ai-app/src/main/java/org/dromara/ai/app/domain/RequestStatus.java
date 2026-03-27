package org.dromara.ai.app.domain;

/**
 * 请求状态枚举
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
public enum RequestStatus {

    /**
     * 处理中
     */
    PROCESSING("processing", "处理中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 已中断
     */
    ABORTED("aborted", "已中断"),

    /**
     * 错误
     */
    ERROR("error", "错误");

    /**
     * 状态值
     */
    private final String value;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param value 状态值
     * @param description 状态描述
     */
    RequestStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取状态值
     *
     * @return 状态值
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static RequestStatus fromValue(String value) {
        for (RequestStatus status : RequestStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
