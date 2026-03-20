package org.dromara.ai.workflow.nodes.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.Builder;
import lombok.Data;

/**
 * 工具绑定信息
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Data
@Builder
public class ToolBinding {
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * Langchain4j 工具规范
     */
    private ToolSpecification specification;

    /**
     * 工具执行器
     */
    private ToolExecutor executor;
}
