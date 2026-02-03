package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 * 用于跟踪向量化和问题生成任务的执行状态
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum TaskState {
    /**
     * 等待/排队中
     */
    PENDING(0, "等待中"),
    /**
     * 执行中
     */
    STARTED(1, "执行中"),
    /**
     * 成功
     */
    SUCCESS(2, "成功"),
    /**
     * 失败
     */
    FAILED(3, "失败"),
    /**
     * 取消中
     */
    CANCELLING(4, "取消中"),
    /**
     * 已取消
     */
    CANCELLED(5, "已取消");

    private final int code;
    private final String info;

    public static TaskState fromCode(int code) {
        for (TaskState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        return null;
    }

    /**
     * 判断是否为终态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
