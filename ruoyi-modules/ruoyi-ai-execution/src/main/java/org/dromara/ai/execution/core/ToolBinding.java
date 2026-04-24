package org.dromara.ai.execution.core;

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
     * LangChain4j 工具规范
     */
    private ToolSpecification specification;

    /**
     * 工具执行器
     */
    private ToolExecutor executor;

    /**
     * 工具类型标识（builtin / mcp / skill）
     */
    private String type;

    /**
     * 来源ID（builtinToolId / mcpServerId / skillId）
     */
    private Long sourceId;

    /**
     * 是否为 Skill 包裹层
     */
    private boolean skillWrapper;
}
