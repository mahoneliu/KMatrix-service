package org.dromara.ai.workflow.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流配置
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
public class WorkflowConfig {

    /**
     * 工作流ID
     */
    private String workflowId;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 节点列表
     */
    private List<NodeConfig> nodes;

    /**
     * 边列表
     */
    private List<EdgeConfig> edges;

    /**
     * 入口节点ID
     */
    private String entryPoint;

    /**
     * 节点配置
     */
    @Data
    public static class NodeConfig {
        /**
         * 节点ID
         */
        private String id;

        /**
         * 节点类型
         */
        private String type;

        /**
         * 节点名称
         */
        private String name;

        /**
         * 节点配置
         */
        private Map<String, Object> config;

        /**
         * 输入参数映射
         */
        private Map<String, Object> inputs;

        /**
         * 执行条件
         */
        private String condition;
    }

    /**
     * 边配置
     */
    @Data
    public static class EdgeConfig {
        /**
         * 起始节点ID
         */
        private String from;

        /**
         * 目标节点ID
         */
        private String to;

        /**
         * 条件 (可选)
         */
        private String condition;
    }

    /**
     * 首选引擎类型
     */
    private org.dromara.ai.workflow.engine.WorkflowEngineType preferredEngine;

    /**
     * 判断是否包含循环
     */
    public boolean hasLoop() {
        // TODO: 实现循环检测算法
        return false;
    }

    /**
     * 判断是否有并行节点
     */
    public boolean hasParallelNodes() {
        if (edges == null)
            return false;
        // 检查是否有节点有多个出边
        java.util.Map<String, Long> outDegree = edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(EdgeConfig::getFrom,
                        java.util.stream.Collectors.counting()));
        return outDegree.values().stream().anyMatch(count -> count > 1);
    }

    /**
     * 判断是否有子工作流
     */
    public boolean hasSubWorkflow() {
        if (nodes == null)
            return false;
        return nodes.stream()
                .anyMatch(node -> "SUB_WORKFLOW".equals(node.getType()));
    }

    /**
     * 判断是否为 Multi-Agent 工作流
     */
    public boolean isMultiAgent() {
        if (nodes == null)
            return false;
        long agentCount = nodes.stream()
                .filter(node -> "AGENT".equals(node.getType()))
                .count();
        return agentCount >= 2;
    }
}
