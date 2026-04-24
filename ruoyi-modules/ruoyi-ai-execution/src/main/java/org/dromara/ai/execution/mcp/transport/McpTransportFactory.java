package org.dromara.ai.execution.mcp.transport;

import org.dromara.ai.execution.domain.KmMcpServer;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

/**
 * MCP Transport 工厂
 * <p>
 * 根据 {@link KmMcpServer} 配置创建对应的 {@link HttpMcpTransport}。
 *
 * @author KMatrix
 */
@Slf4j
public class McpTransportFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 根据 MCP Server 配置创建 HttpMcpTransport
     *
     * @param server MCP Server 配置实体
     * @return HttpMcpTransport 实例，配置无效时返回 null
     */
    public static HttpMcpTransport createHttpTransport(KmMcpServer server) {
        String serverUrl = extractServerUrl(server.getServerConfig());
        if (StrUtil.isBlank(serverUrl)) {
            log.error("MCP Server 配置缺少 url 字段: serverId={}", server.getServerId());
            return null;
        }
        return createHttpTransport(serverUrl);
    }

    /**
     * 根据 SSE URL 创建 HttpMcpTransport
     *
     * @param sseUrl SSE 端点 URL
     * @return HttpMcpTransport 实例
     */
    public static HttpMcpTransport createHttpTransport(String sseUrl) {
        log.info("创建 MCP HttpTransport: url={}", sseUrl);
        return HttpMcpTransport.builder()
                .sseUrl(sseUrl)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 从 serverConfig JSON 中提取 url 字段
     */
    public static String extractServerUrl(String serverConfig) {
        if (StrUtil.isBlank(serverConfig)) {
            return null;
        }
        try {
            Map<String, Object> config = MAPPER.readValue(serverConfig, new TypeReference<Map<String, Object>>() {
            });
            return (String) config.get("url");
        } catch (Exception e) {
            log.error("解析 serverConfig 失败: {}", serverConfig, e);
            return null;
        }
    }
}
