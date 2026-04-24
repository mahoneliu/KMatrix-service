package org.dromara.ai.workflow.workflow.nodes;

import org.dromara.ai.execution.mcp.service.McpClientManager;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class McpResourceNodeTest {

    @Mock
    private McpClientManager mcpClientManager;

    @InjectMocks
    private McpResourceNode mcpResourceNode;

    @Test
    void testExecute_Success() throws Exception {
        NodeContext context = new NodeContext();
        context.setConfig("serverId", 1L);
        context.setConfig("uri", "test/uri");

        when(mcpClientManager.readResource(1L, "test/uri")).thenReturn("Mock Resource Content");

        NodeOutput output = mcpResourceNode.execute(context);

        assertNotNull(output);
        assertEquals("Mock Resource Content", output.getOutput("content"));
        assertEquals("Mock Resource Content", output.getOutput("textContent"));
        assertEquals("Mock Resource Content", context.getGlobalValue("mcpResourceContent"));
    }

    @Test
    void testExecute_MissingServerId() {
        NodeContext context = new NodeContext();
        context.setConfig("uri", "test/uri");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mcpResourceNode.execute(context);
        });

        assertTrue(exception.getMessage().contains("未配置 MCP Server ID"));
    }

    @Test
    void testExecute_MissingUri() {
        NodeContext context = new NodeContext();
        context.setConfig("serverId", 1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mcpResourceNode.execute(context);
        });

        assertTrue(exception.getMessage().contains("未提供资源 URI"));
    }
}
