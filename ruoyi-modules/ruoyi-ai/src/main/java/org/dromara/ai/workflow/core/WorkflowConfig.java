package org.dromara.ai.workflow.core;

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
    // private org.dromara.ai.workflow.engine.WorkflowEngineType preferredEngine;

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

    /**
     * 校验工作流配置
     *
     * @throws IllegalArgumentException 如果配置不合法
     */
    public void validate() {
        // 1. 检查 END 节点存在性和唯一性
        List<NodeConfig> endNodes = nodes.stream()
                .filter(node -> "END".equals(node.getType()))
                .collect(java.util.stream.Collectors.toList());

        if (endNodes.isEmpty()) {
            throw new IllegalArgumentException("工作流必须包含一个 END 节点");
        }

        if (endNodes.size() > 1) {
            throw new IllegalArgumentException("工作流只能有一个 END 节点");
        }

        // 2. 检查 END 节点必须是终端节点(没有出边)
        String endNodeId = endNodes.get(0).getId();
        boolean hasOutgoingEdge = edges.stream()
                .anyMatch(edge -> edge.getFrom().equals(endNodeId));

        if (hasOutgoingEdge) {
            throw new IllegalArgumentException("END 节点不能有出边");
        }

        // 3. 检查所有终端节点(没有出边的节点)
        // 收集所有有出边的节点
        java.util.Set<String> nodesWithOutgoingEdges = edges.stream()
                .map(EdgeConfig::getFrom)
                .collect(java.util.stream.Collectors.toSet());

        // 找出所有终端节点(没有出边的节点)
        List<NodeConfig> terminalNodes = nodes.stream()
                .filter(node -> !nodesWithOutgoingEdges.contains(node.getId()))
                .collect(java.util.stream.Collectors.toList());

        // 终端节点只能有一个,且必须是 END 类型
        if (terminalNodes.size() > 1) {
            String terminalNodeIds = terminalNodes.stream()
                    .map(NodeConfig::getId)
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "工作流只能有一个终端节点(没有出边的节点),当前有多个: " + terminalNodeIds);
        }

        if (terminalNodes.size() == 1) {
            NodeConfig terminalNode = terminalNodes.get(0);
            if (!"END".equals(terminalNode.getType())) {
                throw new IllegalArgumentException(
                        "终端节点必须是 END 类型,当前终端节点 " + terminalNode.getId() +
                                " 的类型是: " + terminalNode.getType());
            }
        }
    }
}
