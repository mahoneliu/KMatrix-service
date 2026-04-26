package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 变量聚合器节点
 * <p>
 * 节点类型标识: VARIABLE_AGGREGATOR
 * <p>
 * 将互斥工作流分支（如 IF/ELSE、意图分类器）的输出汇聚为单一输出变量，
 * 消除在每条分支上重复配置下游节点的需要。
 * <p>
 * 配置项 (来自节点 config):
 * - enableGrouping (Boolean): 是否启用分组聚合，默认 false
 * - variables (List): 单组模式下的变量引用列表，每项包含 sourceNodeId/sourceParam/type
 * - groups (List): 分组模式下的分组列表，每项包含 groupName/variables
 * - outputKey (String): 单组模式下的输出键名，默认 "output"
 * <p>
 * 运行时行为:
 * - 遍历所有配置的变量引用，找到第一个非 null 的值作为输出
 * - 由于互斥分支只有一条会执行，因此只有一个变量会有值
 * - 若多个变量同时有值（违反互斥性），取第一个有值的变量（优先级规则）并记录警告
 * - 若所有变量均为 null，输出 null
 * <p>
 * 输出:
 * - 单组模式: output (或自定义 outputKey) = 有值的变量
 * - 分组模式: 每个分组名作为独立输出 key
 *
 * @author Mahone
 * @date 2026-05-01
 */
@Slf4j
@Component("VARIABLE_AGGREGATOR")
public class VariableAggregatorNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 VARIABLE_AGGREGATOR 节点");

        NodeOutput output = new NodeOutput();
        Map<String, Object> config = context.getNodeConfig();

        boolean enableGrouping = getBoolean(config, "enableGrouping", false);

        if (enableGrouping) {
            // 分组模式：每个分组独立聚合
            executeGroupMode(context, config, output);
        } else {
            // 单组模式：聚合所有变量到一个输出
            executeSingleMode(context, config, output);
        }

        log.info("VARIABLE_AGGREGATOR 节点执行完成");
        return output;
    }

    /**
     * 单组模式执行
     */
    private void executeSingleMode(NodeContext context, Map<String, Object> config, NodeOutput output) {
        String outputKey = getStr(config, "outputKey", "output");
        List<Map<String, Object>> variables = parseVariableList(config.get("variables"));

        Object aggregatedValue = aggregateVariables(variables, context, outputKey);
        output.addOutput(outputKey, aggregatedValue);
        log.info("VARIABLE_AGGREGATOR 单组模式输出: {}={}", outputKey, aggregatedValue);
    }

    /**
     * 分组模式执行
     */
    private void executeGroupMode(NodeContext context, Map<String, Object> config, NodeOutput output) {
        List<Map<String, Object>> groups = parseVariableList(config.get("groups"));

        if (groups.isEmpty()) {
            log.warn("VARIABLE_AGGREGATOR 分组模式未配置任何分组");
            return;
        }

        for (Map<String, Object> group : groups) {
            String groupName = getStr(group, "groupName", null);
            if (StrUtil.isBlank(groupName)) {
                log.warn("VARIABLE_AGGREGATOR 分组缺少 groupName，跳过");
                continue;
            }

            List<Map<String, Object>> variables = parseVariableList(group.get("variables"));
            Object aggregatedValue = aggregateVariables(variables, context, groupName);
            output.addOutput(groupName, aggregatedValue);
            log.info("VARIABLE_AGGREGATOR 分组 [{}] 输出: {}", groupName, aggregatedValue);
        }
    }

    /**
     * 聚合变量列表，返回第一个非 null 的值
     * 若多个变量同时有值，取第一个并记录警告（互斥性校验）
     */
    private Object aggregateVariables(List<Map<String, Object>> variables, NodeContext context, String groupLabel) {
        if (variables.isEmpty()) {
            log.warn("VARIABLE_AGGREGATOR 分组 [{}] 未配置任何变量", groupLabel);
            return null;
        }

        List<Object> nonNullValues = new ArrayList<>();
        List<String> nonNullSources = new ArrayList<>();

        for (Map<String, Object> varRef : variables) {
            String sourceNodeId = getStr(varRef, "sourceNodeId", null);
            String sourceParam = getStr(varRef, "sourceParam", null);

            if (StrUtil.isBlank(sourceNodeId) || StrUtil.isBlank(sourceParam)) {
                log.warn("VARIABLE_AGGREGATOR 变量引用缺少 sourceNodeId 或 sourceParam，跳过");
                continue;
            }

            Object value = resolveVariableValue(context, sourceNodeId, sourceParam);
            if (value != null) {
                nonNullValues.add(value);
                nonNullSources.add(sourceNodeId + "." + sourceParam);
            }
        }

        if (nonNullValues.isEmpty()) {
            log.info("VARIABLE_AGGREGATOR 分组 [{}] 所有变量均为 null，输出 null", groupLabel);
            return null;
        }

        if (nonNullValues.size() > 1) {
            log.warn("VARIABLE_AGGREGATOR 分组 [{}] 检测到多个变量同时有值（违反互斥性）: {}，取第一个值",
                    groupLabel, nonNullSources);
        }

        return nonNullValues.get(0);
    }

    /**
     * 从节点输出中解析变量值
     * 支持 global 全局变量和节点输出变量
     */
    private Object resolveVariableValue(NodeContext context, String sourceNodeId, String sourceParam) {
        // 全局变量
        if ("global".equals(sourceNodeId) || "app".equals(sourceNodeId)
                || "interface".equals(sourceNodeId) || "session".equals(sourceNodeId)) {
            return context.getGlobalValue(sourceParam);
        }

        // 节点输出变量
        Map<String, Map<String, Object>> allNodeOutputs = context.getAllNodeOutputs();
        if (allNodeOutputs == null) {
            return null;
        }

        Map<String, Object> nodeOutputs = allNodeOutputs.get(sourceNodeId);
        if (nodeOutputs == null) {
            return null;
        }

        return nodeOutputs.get(sourceParam);
    }

    // ========== 工具方法 ==========

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseVariableList(Object obj) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
        }
        return result;
    }

    private String getStr(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    @Override
    public String getNodeType() {
        return "VARIABLE_AGGREGATOR";
    }

    @Override
    public String getNodeName() {
        return "变量聚合器";
    }
}
