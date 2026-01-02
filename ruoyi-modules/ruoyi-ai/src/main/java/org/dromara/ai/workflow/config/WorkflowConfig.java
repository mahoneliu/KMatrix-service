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
}
