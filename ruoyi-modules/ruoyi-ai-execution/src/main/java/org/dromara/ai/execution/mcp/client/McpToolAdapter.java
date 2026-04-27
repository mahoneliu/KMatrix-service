package org.dromara.ai.execution.mcp.client;

import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.core.ToolExecutor;
import org.dromara.ai.execution.mcp.service.McpClientManager;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP 工具适配器
 * <p>
 * 将 MCP Server 提供的工具列表转换为 KMatrix 的 {@link ToolBinding} 模型。
 *
 * @author KMatrix
 */
@Slf4j
public class McpToolAdapter {

    /**
     * 将 MCP Server 的工具列表转换为 ToolBinding 列表
     *
     * @param serverId          MCP Server ID
     * @param mcpClientManager  MCP Client 管理器
     * @return ToolBinding 列表
     */
    public static List<ToolBinding> adaptTools(Long serverId, McpClientManager mcpClientManager) {
        List<ToolBinding> result = new ArrayList<>();
        List<ToolSpecification> tools = mcpClientManager.listTools(serverId);

        for (ToolSpecification spec : tools) {
            ToolExecutor executor = arguments -> {
                ToolExecutionRequest mcpReq = ToolExecutionRequest.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .name(spec.name())
                        .arguments(arguments)
                        .build();
                return mcpClientManager.executeTool(serverId, mcpReq);
            };

            result.add(ToolBinding.builder()
                    .toolName(spec.name())
                    .specification(spec)
                    .executor(executor)
                    .type("mcp")
                    .sourceId(serverId)
                    .build());

            log.info("MCP 工具适配成功: serverId={}, toolName={}", serverId, spec.name());
        }

        return result;
    }
}
