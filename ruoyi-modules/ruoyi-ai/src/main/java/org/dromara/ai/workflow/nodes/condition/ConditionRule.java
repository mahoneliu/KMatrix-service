package org.dromara.ai.workflow.nodes.condition;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 条件规则
 * 表示一个具体的比较条件
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Data
public class ConditionRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则类型标识，固定为 "rule"
     * 用于区分 ConditionRule 和 ConditionGroup
     */
    private String type = "rule";

    /**
     * 变量引用（左值）
     */
    private VariableRef variable;

    /**
     * 比较运算符
     * 可选值: eq, ne, gt, lt, gte, lte, contains, notContains, startsWith, endsWith,
     * isEmpty, isNotEmpty
     */
    private String operator;

    /**
     * 比较值（右值）
     * 当 operator 为 isEmpty/isNotEmpty 时可为 null
     */
    private Object compareValue;

    /**
     * 比较值类型: static / variable
     * static: compareValue 为静态值
     * variable: compareValue 通过 compareVariable 引用获取
     */
    private String compareValueType = "static";

    /**
     * 比较值变量引用
     * 当 compareValueType=variable 时使用
     */
    private VariableRef compareVariable;
}
