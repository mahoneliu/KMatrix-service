package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 逻辑运算符枚举
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Getter
@AllArgsConstructor
public enum LogicalOperator {

    /** 且 - 所有条件都满足 */
    AND("AND", "且"),
    /** 或 - 任一条件满足 */
    OR("OR", "或");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static LogicalOperator fromCode(String code) {
        for (LogicalOperator op : values()) {
            if (op.getCode().equalsIgnoreCase(code)) {
                return op;
            }
        }
        throw new IllegalArgumentException("不支持的逻辑运算符: " + code);
    }
}
