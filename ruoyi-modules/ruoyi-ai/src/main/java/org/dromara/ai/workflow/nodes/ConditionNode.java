package org.dromara.ai.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 条件判断节点
 * 根据条件表达式决定分支
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("CONDITION")
public class ConditionNode implements WorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行CONDITION节点");

        NodeOutput output = new NodeOutput();

        // 获取条件参数
        String value = String.valueOf(context.getInput("value"));
        String operator = (String) context.getInput("operator");
        String compareValue = (String) context.getInput("compareValue");

        // 执行条件判断
        boolean result = evaluate(value, operator, compareValue);

        // 保存输出
        output.addOutput("result", result);

        log.info("CONDITION节点执行完成, result={}", result);
        return output;
    }

    private boolean evaluate(String value, String operator, String compareValue) {
        if (value == null || operator == null) {
            return false;
        }

        switch (operator) {
            case "==":
            case "equals":
                return value.equals(compareValue);
            case "!=":
            case "notEquals":
                return !value.equals(compareValue);
            case "contains":
                return value.contains(compareValue);
            case "startsWith":
                return value.startsWith(compareValue);
            case "endsWith":
                return value.endsWith(compareValue);
            default:
                log.warn("不支持的运算符: {}", operator);
                return false;
        }
    }

    @Override
    public String getNodeType() {
        return "CONDITION";
    }

    @Override
    public String getNodeName() {
        return "条件判断";
    }
}
