package org.dromara.ai.workflow.workflow.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.mcp.service.McpClientManager;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

/**
 * MCP资源读取节点
 * 从指定的 MCP Server 动态读取资源内容并注入工作流
 *
 * @author Mahone
 * @date 2026-04-22
 */
@Slf4j
@RequiredArgsConstructor
@Component("MCP_RESOURCE")
public class McpResourceNode extends AbstractWorkflowNode {

    private final McpClientManager mcpClientManager;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("Executing MCP_RESOURCE node");
        NodeOutput output = new NodeOutput();

        // 1. 获取配置
        Long serverId = context.getConfigAsLong("serverId");
        if (serverId == null) {
            throw new RuntimeException("MCP Server ID is not configured");
        }

        // uri 可以从 config 固定配置，也可以从 inputs 动态传入
        String uri = (String) context.getInput("uri");
        if (uri == null) {
            uri = context.getConfigAsString("uri");
        }
        
        if (uri == null || uri.trim().isEmpty()) {
            throw new RuntimeException("Resource URI is not provided");
        }

        log.info("MCP_RESOURCE node reading resource: serverId={}, uri={}", serverId, uri);

        // 2. 调用 ToolProviderService 读取资源
        try {
            Object resourceContent = mcpClientManager.readResource(serverId, uri);
            
            // 3. 将结果存入 NodeOutput 和 Context
            output.addOutput("content", resourceContent);
            
            // 对于单纯的文本资源，提取并存入 context 供下游方便使用
            String textContent = extractTextContent(resourceContent);
            if (textContent != null) {
                output.addOutput("textContent", textContent);
                context.setGlobalValue("mcpResourceContent", textContent);
            }
            
            log.info("MCP_RESOURCE node executed successfully");
        } catch (Exception e) {
            log.error("MCP_RESOURCE node execution failed", e);
            throw new RuntimeException("Failed to read MCP resource: " + e.getMessage(), e);
        }

        return output;
    }

    /**
     * 尝试从复杂的 MCP Resource 对象中提取纯文本
     */
    private String extractTextContent(Object resource) {
        if (resource == null) return null;
        if (resource instanceof String) return (String) resource;
        
        // 如果是复杂的 LangChain4j Resource 类，可以通过反射或序列化后提取
        // 这里提供一个简单的 ToString 降级
        return resource.toString();
    }

    @Override
    public String getNodeType() {
        return "MCP_RESOURCE";
    }

    @Override
    public String getNodeName() {
        return "MCP Resource Reader";
    }
}
