package org.dromara.ai.domain.enums;

/**
 * 工作流实例状态枚举
 *
 * @author Mahone
 * @date 2026-01-02
 */
public enum WorkflowInstanceStatus {

    /**
     * 运行中
     */
    RUNNING("运行中"),

    /**
     * 已暂停
     */
    PAUSED("已暂停"),

    /**
     * 已完成
     */
    COMPLETED("已完成"),

    /**
     * 失败
     */
    FAILED("失败");

    private final String description;

    WorkflowInstanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
