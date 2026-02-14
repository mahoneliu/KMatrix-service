package org.dromara.ai.workflow.nodes.condition;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 条件组
 * 支持嵌套的条件组合，可包含多个条件规则或子条件组
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Data
public class ConditionGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则类型标识，固定为 "group"
     * 用于区分 ConditionRule 和 ConditionGroup
     */
    private String type = "group";

    /**
     * 逻辑运算符: AND / OR
     * 组内所有条件按此运算符组合
     */
    private String logicalOperator = "AND";

    /**
     * 条件列表
     * 可包含 ConditionRule 或嵌套的 ConditionGroup
     * 由于 Jackson 反序列化需要，这里使用 Object 类型
     */
    private List<Object> conditions = new ArrayList<>();
}
