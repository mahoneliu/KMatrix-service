package org.dromara.ai.domain.enums;

/**
 * 节点执行状态枚举
 *
 * @author Mahone
 * @date 2026-01-02
 */
public enum NodeExecutionStatus {

    /**
     * 等待执行
     */
    PENDING("等待执行"),

    /**
     * 执行中
     */
    RUNNING("执行中"),

    /**
     * 执行完成
     */
    COMPLETED("执行完成"),

    /**
     * 执行失败
     */
    FAILED("执行失败"),

    /**
     * 已跳过
     */
    SKIPPED("已跳过");

    private final String description;

    NodeExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
