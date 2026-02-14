package org.dromara.ai.workflow.nodes.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.enums.ComparisonOperator;
import org.dromara.ai.domain.enums.LogicalOperator;
import org.dromara.ai.workflow.core.WorkflowState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 条件评估器
 * 负责解析和执行结构化条件表达式
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * 评估条件分支列表，返回第一个满足条件的分支名称
     *
     * @param branches 条件分支列表
     * @param state    工作流状态
     * @return 满足条件的分支名称，如果都不满足则返回 "default"
     */
    public String evaluateBranches(List<ConditionBranch> branches, WorkflowState state) {
        if (branches == null || branches.isEmpty()) {
            return "default";
        }

        for (int i = 0; i < branches.size(); i++) {
            ConditionBranch branch = branches.get(i);
            try {
                if (evaluateGroup(branch.getCondition(), state)) {
                    log.info("条件分支 [{}] 匹配成功", branch.getName());
                    return branch.getName();
                }
            } catch (Exception e) {
                log.error("评估条件分支 [{}] 时发生错误: {}", branch.getName(), e.getMessage(), e);
            }
        }

        log.info("所有条件分支都不满足，返回默认分支");
        return "default";
    }

    /**
     * 评估条件组
     *
     * @param group 条件组
     * @param state 工作流状态
     * @return 条件组是否满足
     */
    public boolean evaluateGroup(ConditionGroup group, WorkflowState state) {
        if (group == null || group.getConditions() == null || group.getConditions().isEmpty()) {
            log.warn("条件组为空，默认返回 false");
            return false;
        }

        LogicalOperator logicalOp = LogicalOperator.fromCode(group.getLogicalOperator());
        List<Object> conditions = group.getConditions();

        for (Object condition : conditions) {
            boolean result = evaluateCondition(condition, state);

            if (logicalOp == LogicalOperator.AND && !result) {
                // AND 模式：任一条件不满足即返回 false
                return false;
            } else if (logicalOp == LogicalOperator.OR && result) {
                // OR 模式：任一条件满足即返回 true
                return true;
            }
        }

        // AND 模式：所有条件都满足，返回 true
        // OR 模式：所有条件都不满足，返回 false
        return logicalOp == LogicalOperator.AND;
    }

    /**
     * 评估单个条件（可能是规则或嵌套组）
     */
    private boolean evaluateCondition(Object condition, WorkflowState state) {
        if (condition == null) {
            return false;
        }

        // 处理 Map 类型（从 JSON 反序列化而来）
        if (condition instanceof Map<?, ?> mapCondition) {
            String type = (String) mapCondition.get("type");

            if ("group".equals(type)) {
                // 嵌套条件组
                ConditionGroup nestedGroup = objectMapper.convertValue(mapCondition, ConditionGroup.class);
                return evaluateGroup(nestedGroup, state);
            } else {
                // 条件规则（type=rule 或未指定）
                ConditionRule rule = objectMapper.convertValue(mapCondition, ConditionRule.class);
                return evaluateRule(rule, state);
            }
        }

        // 直接是对象类型
        if (condition instanceof ConditionGroup nestedGroup) {
            return evaluateGroup(nestedGroup, state);
        } else if (condition instanceof ConditionRule rule) {
            return evaluateRule(rule, state);
        }

        log.warn("未知的条件类型: {}", condition.getClass().getName());
        return false;
    }

    /**
     * 评估单个条件规则
     */
    private boolean evaluateRule(ConditionRule rule, WorkflowState state) {
        if (rule == null || rule.getVariable() == null || rule.getOperator() == null) {
            log.warn("条件规则配置不完整");
            return false;
        }

        try {
            // 获取左值（变量值）
            Object leftValue = resolveVariable(rule.getVariable(), state);

            // 获取比较运算符
            ComparisonOperator operator = ComparisonOperator.fromCode(rule.getOperator());

            // 对于一元运算符，不需要右值
            if (operator.isUnary()) {
                return compareUnary(leftValue, operator);
            }

            // 获取右值（比较值）
            Object rightValue;
            if ("variable".equals(rule.getCompareValueType()) && rule.getCompareVariable() != null) {
                rightValue = resolveVariable(rule.getCompareVariable(), state);
            } else {
                rightValue = rule.getCompareValue();
            }

            // 执行比较
            return compare(leftValue, operator, rightValue);

        } catch (Exception e) {
            log.error("评估条件规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解析变量值
     */
    private Object resolveVariable(VariableRef variable, WorkflowState state) {
        if (variable == null) {
            return null;
        }

        String sourceType = variable.getSourceType();
        String sourceKey = variable.getSourceKey();
        String sourceParam = variable.getSourceParam();

        if ("global".equals(sourceType)) {
            // 全局参数: app / interface / session
            Map<String, Object> globalState = state.getGlobalState();
            if (globalState == null) {
                return null;
            }
            // 全局参数存储在 globalState 中，按 sourceKey 分类
            Object categoryData = globalState.get(sourceKey);
            if (categoryData instanceof Map<?, ?> categoryMap) {
                return categoryMap.get(sourceParam);
            }
            // 兼容直接存储在 globalState 中的情况
            return globalState.get(sourceParam);
        } else if ("node".equals(sourceType)) {
            // 节点输出
            Map<String, Object> nodeOutput = state.getNodeOutput(sourceKey);
            if (nodeOutput == null) {
                return null;
            }
            return nodeOutput.get(sourceParam);
        }

        log.warn("未知的变量来源类型: {}", sourceType);
        return null;
    }

    /**
     * 执行一元比较运算
     */
    private boolean compareUnary(Object value, ComparisonOperator operator) {
        boolean isEmpty = isValueEmpty(value);

        return switch (operator) {
            case IS_EMPTY -> isEmpty;
            case IS_NOT_EMPTY -> !isEmpty;
            default -> false;
        };
    }

    /**
     * 判断值是否为空
     */
    private boolean isValueEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return str.isEmpty();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    /**
     * 执行二元比较运算
     */
    private boolean compare(Object leftValue, ComparisonOperator operator, Object rightValue) {
        // 空值处理
        if (leftValue == null) {
            return rightValue == null && operator == ComparisonOperator.EQ;
        }

        String leftStr = String.valueOf(leftValue);
        String rightStr = rightValue != null ? String.valueOf(rightValue) : "";

        return switch (operator) {
            case EQ -> leftStr.equals(rightStr);
            case NE -> !leftStr.equals(rightStr);
            case GT, LT, GTE, LTE -> compareNumeric(leftValue, rightValue, operator);
            case CONTAINS -> leftStr.contains(rightStr);
            case NOT_CONTAINS -> !leftStr.contains(rightStr);
            case STARTS_WITH -> leftStr.startsWith(rightStr);
            case ENDS_WITH -> leftStr.endsWith(rightStr);
            default -> false;
        };
    }

    /**
     * 执行数值比较
     */
    private boolean compareNumeric(Object leftValue, Object rightValue, ComparisonOperator operator) {
        try {
            BigDecimal left = toBigDecimal(leftValue);
            BigDecimal right = toBigDecimal(rightValue);

            if (left == null || right == null) {
                log.warn("数值比较失败：无法转换为数字 left={}, right={}", leftValue, rightValue);
                return false;
            }

            int cmp = left.compareTo(right);

            return switch (operator) {
                case GT -> cmp > 0;
                case LT -> cmp < 0;
                case GTE -> cmp >= 0;
                case LTE -> cmp <= 0;
                default -> false;
            };
        } catch (Exception e) {
            log.error("数值比较异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将值转换为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
