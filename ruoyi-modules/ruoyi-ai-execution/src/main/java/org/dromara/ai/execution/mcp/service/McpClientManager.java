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
     * @param serverUrl 可选的 MCP Server URL，为 null 时从数据库查询
     * @return MCP Client 实例，不可用时返回 null
     */
    public McpClient getClient(Long serverId, String serverUrl) {
        KmMcpServer server = null;
        if (StrUtil.isBlank(serverUrl)) {
            server = mcpServerMapper.selectById(serverId);
            if (server == null || !"0".equals(server.getStatus())) {
                log.warn("MCP Server 不存在或已停用: serverId={}", serverId);
                return null;
            }
            serverUrl = McpTransportFactory.extractServerUrl(server.getServerConfig());
            if (StrUtil.isBlank(serverUrl)) {
                log.error("MCP Server 配置缺少 url/baseUrl: serverId={}", serverId);
                return null;
            }
        }

        String finalServerUrl = serverUrl;
        KmMcpServer finalServer = server;
        try {
            return mcpClientCache.computeIfAbsent(serverId, id -> {
                log.info("初始化 MCP Client: serverId={}, url={}", id, finalServerUrl);
                if (finalServer != null && StrUtil.isNotBlank(finalServer.getServerConfig())) {
                    Map<String, String> headers = McpTransportFactory.extractServerHeaders(finalServer.getServerConfig());
                    if (!headers.isEmpty()) {
                        log.info("MCP Server 自定义 Headers: serverId={}, keys={}", id, headers.keySet());
                        // 检查 Authorization header 是否包含占位符
                        String auth = headers.get("Authorization");
                        if (auth != null && (auth.contains("${") || auth.contains("<") || "Bearer ".equals(auth.trim()))) {
                            log.warn("MCP Server Authorization header 可能包含未替换的占位符: serverId={}", id);
                        }
                    }
                }
                var transport = finalServer != null
                        ? McpTransportFactory.createHttpTransport(finalServer)
                        : McpTransportFactory.createHttpTransport(finalServerUrl);
                if (transport == null) {
                    log.error("创建 MCP Transport 失败: serverId={}", id);
                    return null;
                }
                return DefaultMcpClient.builder()
                        .transport(transport)
                        .toolExecutionTimeout(Duration.ofMillis(MCP_TIMEOUT_MS))
                        .build();
            });
        } catch (Exception e) {
            // 初始化失败（如 401 认证失败），清理缓存避免后续请求命中不可用的 client
            mcpClientCache.remove(serverId);
            // 提取根因给出友好错误
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String msg = cause.getMessage();
            if (msg != null && msg.contains("401")) {
                log.error("MCP Server 认证失败(401): serverId={}, url={}", serverId, finalServerUrl);
                throw new RuntimeException("MCP Server 认证失败(401)，请检查 serverConfig 中的 headers 配置，确保 API Key 正确且已替换占位符（如 ${API_KEY}）", e);
            }
            if (msg != null && msg.contains("403")) {
                log.error("MCP Server 访问被拒绝(403): serverId={}", serverId);
                throw new RuntimeException("MCP Server 访问被拒绝(403)，请检查 API Key 权限", e);
            }
            if (msg != null && msg.contains("404")) {
                log.error("MCP Server 地址不存在(404): serverId={}, url={}", serverId, finalServerUrl);
                throw new RuntimeException("MCP Server 地址不存在(404)，请检查 URL 配置", e);
            }
            log.error("MCP Client 初始化失败: serverId={}, url={}", serverId, finalServerUrl, e);
            throw new RuntimeException("MCP Client 初始化失败: " + msg, e);
        }
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
     * <p>
     * 很多 MCP Server（尤其是工具型服务）不提供 resources 能力，
     * 调用 resources/list 时可能返回 500 或其他错误，此时优雅降级返回空列表。
     */
    public Object listResources(Long serverId) {
        McpClient client = getClient(serverId);
        if (client == null) {
            log.warn("MCP Server 不可用: serverId={}", serverId);
            return new ArrayList<>();
        }
        try {
            java.lang.reflect.Method method = client.getClass().getMethod("listResources");
            return method.invoke(client);
        } catch (NoSuchMethodException e) {
            log.warn("当前 LangChain4j 版本不支持 listResources: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            // 提取根因判断错误类型
            Throwable cause = e.getCause();
            while (cause != null && cause.getCause() != null) {
                cause = cause.getCause();
            }
            String msg = cause != null ? cause.getMessage() : e.getMessage();
            // 500 通常是服务端不支持该方法（如纯工具型 MCP Server），优雅降级
            if (msg != null && msg.contains("500")) {
                log.info("MCP Server 不支持 resources/list（服务端返回500），降级返回空列表: serverId={}", serverId);
                return new ArrayList<>();
            }
            // 401/403 认证问题仍需抛出
            if (msg != null && msg.contains("401")) {
                throw new RuntimeException("MCP Server 认证失败(401)，请检查 serverConfig 中的 headers 配置，确保 API Key 正确", e);
            }
            if (msg != null && msg.contains("403")) {
                throw new RuntimeException("MCP Server 访问被拒绝(403)，请检查 API Key 权限", e);
            }
            // 其他错误也优雅降级，避免影响主流程
            log.warn("获取 MCP 资源列表失败，降级返回空列表: serverId={}, error={}", serverId, msg);
            return new ArrayList<>();
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
