package org.dromara.ai.execution.mcp.transport;

import org.dromara.ai.execution.domain.KmMcpServer;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP Transport 工厂
 * <p>
 * 根据 {@link KmMcpServer} 配置创建对应的 {@link McpTransport}。
 *
 * @author KMatrix
 */
@Slf4j
public class McpTransportFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 根据 MCP Server 配置创建 McpTransport
     *
     * @param server MCP Server 配置实体
     * @return McpTransport 实例，配置无效时返回 null
     */
    public static McpTransport createHttpTransport(KmMcpServer server) {
        String serverUrl = extractServerUrl(server.getServerConfig());
        if (StrUtil.isBlank(serverUrl)) {
            log.error("MCP Server 配置缺少 url/baseUrl 字段: serverId={}", server.getServerId());
            return null;
        }
        Map<String, String> headers = extractServerHeaders(server.getServerConfig());
        return createHttpTransport(serverUrl, headers);
    }

    /**
     * 根据 URL 创建 StreamableHttpMcpTransport
     *
     * @param url MCP Server 的 Streamable HTTP 端点 URL
     * @return McpTransport 实例
     */
    public static McpTransport createHttpTransport(String url) {
        return createHttpTransport(url, null);
    }

    /**
     * 根据 URL 和自定义 Header 创建 StreamableHttpMcpTransport
     *
     * @param url     MCP Server 的 Streamable HTTP 端点 URL
     * @param headers 自定义请求 Header，可为 null
     * @return McpTransport 实例
     */
    public static McpTransport createHttpTransport(String url, Map<String, String> headers) {
        log.info("创建 MCP StreamableHttpTransport: url={}, headers={}", url, headers != null ? headers.keySet() : "none");
        var builder = StreamableHttpMcpTransport.builder()
                .url(url)
                .logRequests(true)
                .logResponses(true);
        if (headers != null && !headers.isEmpty()) {
            builder.customHeaders(headers);
        }
        return builder.build();
    }

    /**
     * 从 serverConfig JSON 中提取 url 字段
     * <p>
     * 兼容多种字段名：url、baseUrl（阿里云百炼等云端 MCP 配置常用 baseUrl）
     */
    public static String extractServerUrl(String serverConfig) {
        if (StrUtil.isBlank(serverConfig)) {
            return null;
        }
        try {
            Map<String, Object> config = MAPPER.readValue(serverConfig, new TypeReference<Map<String, Object>>() {
            });
            // 优先取 url，若不存在则取 baseUrl
            String url = (String) config.get("url");
            if (StrUtil.isBlank(url)) {
                url = (String) config.get("baseUrl");
            }
            return url;
        } catch (Exception e) {
            log.error("解析 serverConfig 失败: {}", serverConfig, e);
            return null;
        }
    }

    /**
     * 从 serverConfig JSON 中提取 headers 字段
     */
    public static Map<String, String> extractServerHeaders(String serverConfig) {
        if (StrUtil.isBlank(serverConfig)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> config = MAPPER.readValue(serverConfig, new TypeReference<Map<String, Object>>() {
            });

            Map<String, String> headers = (Map<String, String>) config.get("headers");
            if (headers == null) {
                headers = new HashMap<>();
            }
            return headers;
        } catch (Exception e) {
            log.error("解析 serverConfig 失败: {}", serverConfig, e);
            return new HashMap<>();
        }
    }
}
