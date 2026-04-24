package org.dromara.ai.app.service.tool;

import org.dromara.ai.execution.core.IToolProvider;
import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.mcp.service.McpClientManager;
import dev.langchain4j.mcp.client.McpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工具提供服务（向后兼容委托层）
 * <p>
 * 所有实际逻辑已迁移至 {@code org.dromara.ai.execution} 模块。
 * 本类保留以兼容旧引用，内部全部委托至新模块。
 *
 * @deprecated 请直接使用 {@link org.dromara.ai.execution.core.impl.ToolProviderServiceImpl}
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Deprecated
public class ToolProviderService implements IToolProvider {

    private final IToolProvider delegate;
    private final McpClientManager mcpClientManager;

    @Override
    public List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs) {
        return delegate.resolveBindings(toolRefs);
    }

    @Override
    public List<ToolBinding> resolveSkillInnerBindings(Long skillId) {
        return delegate.resolveSkillInnerBindings(skillId);
    }

    /**
     * @deprecated 请使用 {@link McpClientManager#getClient(Long)}
     */
    @Deprecated
    public McpClient getMcpClient(Long serverId, String serverUrl) {
        return mcpClientManager.getClient(serverId, serverUrl);
    }

    /**
     * @deprecated 请使用 {@link McpClientManager#getClient(Long)}
     */
    @Deprecated
    public McpClient getMcpClient(Long serverId) {
        return mcpClientManager.getClient(serverId);
    }

    /**
     * @deprecated 请使用 {@link McpClientManager#listResources(Long)}
     */
    @Deprecated
    public Object listResources(Long serverId) {
        return mcpClientManager.listResources(serverId);
    }

    /**
     * @deprecated 请使用 {@link McpClientManager#readResource(Long, String)}
     */
    @Deprecated
    public Object readResource(Long serverId, String uri) {
        return mcpClientManager.readResource(serverId, uri);
    }
}
