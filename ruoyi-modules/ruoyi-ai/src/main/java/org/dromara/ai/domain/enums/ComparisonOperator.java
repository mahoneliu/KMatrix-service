package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 比较运算符枚举
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Getter
@AllArgsConstructor
public enum ComparisonOperator {

    /** 等于 */
    EQ("eq", "等于"),
    /** 不等于 */
    NE("ne", "不等于"),
    /** 大于 */
    GT("gt", "大于"),
    /** 小于 */
    LT("lt", "小于"),
    /** 大于等于 */
    GTE("gte", "大于等于"),
    /** 小于等于 */
    LTE("lte", "小于等于"),
    /** 包含 */
    CONTAINS("contains", "包含"),
    /** 不包含 */
    NOT_CONTAINS("notContains", "不包含"),
    /** 以...开始 */
    STARTS_WITH("startsWith", "以...开始"),
    /** 以...结束 */
    ENDS_WITH("endsWith", "以...结束"),
    /** 为空 */
    IS_EMPTY("isEmpty", "为空"),
    /** 不为空 */
    IS_NOT_EMPTY("isNotEmpty", "不为空");

    private final String code;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static ComparisonOperator fromCode(String code) {
        for (ComparisonOperator op : values()) {
            if (op.getCode().equals(code)) {
                return op;
            }
        }
        throw new IllegalArgumentException("不支持的比较运算符: " + code);
    }

    /**
     * 判断是否为一元运算符（不需要比较值）
     */
    public boolean isUnary() {
        return this == IS_EMPTY || this == IS_NOT_EMPTY;
    }
}
