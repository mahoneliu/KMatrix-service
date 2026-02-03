package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务类型枚举
 * 用于文档和分块的异步任务类型标识
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum TaskType {
    /**
     * 向量化任务
     */
    EMBEDDING(1, "向量化"),
    /**
     * 生成问题任务
     */
    GENERATE_PROBLEM(2, "生成问题");

    private final int code;
    private final String info;

    public static TaskType fromCode(int code) {
        for (TaskType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
