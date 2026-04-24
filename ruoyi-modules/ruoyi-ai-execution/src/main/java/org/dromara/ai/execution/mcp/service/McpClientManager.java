package org.dromara.ai.execution.mcp.service;

import org.dromara.ai.execution.domain.KmMcpServer;
import org.dromara.ai.execution.mapper.KmMcpServerMapper;
import org.dromara.ai.execution.mcp.transport.McpTransportFactory;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Client 生命周期管理
 * <p>
 * 维护 MCP Client 的缓存，提供获取、刷新、工具列表查询、工具执行等能力。
 *
 * @author KMatrix
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class McpClientManager {

    /** MCP HTTP 请求超时（毫秒） */
    private static final int MCP_TIMEOUT_MS = 30_000;

    private final KmMcpServerMapper mcpServerMapper;

    /** 长期保持活性的 MCP Client 缓存 */
    private final Map<Long, McpClient> mcpClientCache = new ConcurrentHashMap<>();

    /**
     * 获取或初始化 MCP Client
     *
     * @param serverId  MCP Server ID
     * @param serverUrl 可选的 SSE URL，为 null 时从数据库查询
     * @return MCP Client 实例，不可用时返回 null
     */
    public McpClient getClient(Long serverId, String serverUrl) {
        if (StrUtil.isBlank(serverUrl)) {
            KmMcpServer server = mcpServerMapper.selectById(serverId);
            if (server == null || !"0".equals(server.getStatus())) {
                return null;
            }
            serverUrl = McpTransportFactory.extractServerUrl(server.getServerConfig());
            if (StrUtil.isBlank(serverUrl)) {
                return null;
            }
        }

        String finalServerUrl = serverUrl;
        return mcpClientCache.computeIfAbsent(serverId, id -> {
            log.info("初始化 MCP Client: serverId={}, url={}", id, finalServerUrl);
            var transport = McpTransportFactory.createHttpTransport(finalServerUrl);
            return DefaultMcpClient.builder()
                    .transport(transport)
                    .toolExecutionTimeout(Duration.ofMillis(MCP_TIMEOUT_MS))
                    .build();
        });
    }

    /**
     * 获取或初始化 MCP Client（仅通过 ID）
     */
    public McpClient getClient(Long serverId) {
        return getClient(serverId, null);
    }

    /**
     * 刷新指定 MCP Client（关闭旧连接，重新创建）
     */
    public McpClient refreshClient(Long serverId) {
        McpClient old = mcpClientCache.remove(serverId);
        if (old != null) {
            try {
                // DefaultMcpClient 目前没有显式 close，移除缓存即可
                log.info("已移除旧 MCP Client: serverId={}", serverId);
            } catch (Exception e) {
                log.warn("关闭旧 MCP Client 异常: serverId={}", serverId, e);
            }
        }
        return getClient(serverId);
    }

    /**
     * 列出 MCP Server 提供的工具列表
     */
    public List<ToolSpecification> listTools(Long serverId) {
        McpClient client = getClient(serverId);
        if (client == null) {
            log.warn("MCP Server 不可用: serverId={}", serverId);
            return new ArrayList<>();
        }
        try {
            return client.listTools();
        } catch (Exception e) {
            log.error("获取 MCP Server 工具列表失败: serverId={}", serverId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 执行 MCP 工具
     */
    public String executeTool(Long serverId, ToolExecutionRequest request) {
        McpClient client = getClient(serverId);
        if (client == null) {
            throw new RuntimeException("MCP Server 不可用: " + serverId);
        }
        try {
            ToolExecutionResult result = client.executeTool(request);
            return result.resultText();
        } catch (Exception e) {
            log.error("执行 MCP 工具失败: serverId={}, tool={}", serverId, request.name(), e);
            throw new RuntimeException("MCP 工具执行失败", e);
        }
    }

    /**
     * 列出 MCP Server 提供的资源
     */
    public Object listResources(Long serverId) {
        McpClient client = getClient(serverId);
        if (client == null) {
            throw new RuntimeException("MCP Server 不可用: " + serverId);
        }
        try {
            java.lang.reflect.Method method = client.getClass().getMethod("listResources");
            return method.invoke(client);
        } catch (NoSuchMethodException e) {
            log.warn("当前 LangChain4j 版本不支持 listResources: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取 MCP 资源列表失败: serverId={}", serverId, e);
            throw new RuntimeException("获取资源失败", e);
        }
    }

    /**
     * 读取 MCP Server 的特定资源内容
     */
    public Object readResource(Long serverId, String uri) {
        McpClient client = getClient(serverId);
        if (client == null) {
            throw new RuntimeException("MCP Server 不可用: " + serverId);
        }
        try {
            Class<?> reqClass = Class.forName("dev.langchain4j.mcp.client.request.ReadResourceRequest");
            Object req = reqClass.getMethod("builder").invoke(null);
            Object reqBuilder = req.getClass().getMethod("uri", String.class).invoke(req, uri);
            Object finalReq = reqBuilder.getClass().getMethod("build").invoke(reqBuilder);

            java.lang.reflect.Method method = client.getClass().getMethod("readResource", reqClass);
            return method.invoke(client, finalReq);
        } catch (Exception e) {
            log.error("读取 MCP 资源失败: serverId={}, uri={}", serverId, uri, e);
            throw new RuntimeException("读取资源失败", e);
        }
    }
}
