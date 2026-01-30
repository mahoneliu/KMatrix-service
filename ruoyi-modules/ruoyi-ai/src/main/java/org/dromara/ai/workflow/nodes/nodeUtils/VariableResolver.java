package org.dromara.ai.workflow.nodes.nodeUtils;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.WorkflowState;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流变量解析工具类
 * 从 LangGraphWorkflowEngine 抽离的变量解析逻辑
 * 支持完全匹配和字符串插值两种模式
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Slf4j
public class VariableResolver {

    private VariableResolver() {
        // 工具类,禁止实例化
    }

    // 正则模式: 匹配 ${...}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 解析输入参数
     * 支持两种模式:
     * 1. 完全匹配: "${nodeId.outputKey}" 直接解析为原始类型
     * 2. 字符串插值: "Context: ${ctx}\nQuestion: ${query}" 使用正则替换为字符串
     *
     * @param inputDefs 输入参数定义
     * @param state     工作流状态
     * @return 解析后的参数 Map
     */
    public static Map<String, Object> resolveInputs(Map<String, Object> inputDefs, WorkflowState state) {
        Map<String, Object> inputs = new HashMap<>();

        if (inputDefs == null) {
            return inputs;
        }

        for (Map.Entry<String, Object> entry : inputDefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String strValue = (String) value;

                // 检查是否为完全匹配的单变量 (如 "${nodeId.output}")
                if (strValue.startsWith("${") && strValue.endsWith("}") && strValue.indexOf("${", 2) == -1) {
                    // 完全匹配: 保持原始类型
                    value = resolveExpression(strValue, state);
                } else {
                    // 字符串插值: 使用正则替换所有 ${...}
                    value = interpolateString(strValue, state);
                }
            }

            inputs.put(key, value);
        }

        return inputs;
    }

    /**
     * 解析单个表达式
     * 例如: "${nodeId.output}" -> 从 state 中获取对应值
     *
     * @param expression 表达式字符串
     * @param state      工作流状态
     * @return 解析后的值
     */
    public static Object resolveExpression(String expression, WorkflowState state) {
        if (expression == null || !expression.startsWith("${") || !expression.endsWith("}")) {
            return expression;
        }

        // 去除 ${ 和 }
        String expr = expression.substring(2, expression.length() - 1);
        return resolveExpressionByName(expr, state);
    }

    /**
     * 字符串插值
     * 将字符串中的所有 ${...} 替换为对应的值
     *
     * @param template 模板字符串
     * @param state    工作流状态
     * @return 插值后的字符串
     */
    public static String interpolateString(String template, WorkflowState state) {
        if (template == null) {
            return null;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expr = matcher.group(1);
            Object resolved = resolveExpressionByName(expr, state);
            String replacement = resolved != null ? resolved.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 根据表达式名称解析值
     * 支持两种格式:
     * 1. nodeId.outputKey - 从节点输出获取
     * 2. globalKey - 从全局状态获取
     *
     * @param expr  表达式名称 (不含 ${ })
     * @param state 工作流状态
     * @return 解析后的值
     */
    private static Object resolveExpressionByName(String expr, WorkflowState state) {
        // 解析 nodeId.outputKey
        String[] parts = expr.split("\\.");
        if (parts.length == 2) {
            String nodeId = parts[0];
            String outputKey = parts[1];

            Map<String, Object> outputs = state.getNodeOutput(nodeId);
            if (outputs != null) {
                return outputs.get(outputKey);
            }
        }

        // 尝试从全局状态获取
        return state.getGlobalState().get(expr);
    }
}
