package org.dromara.ai.execution.registry.adapter;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.registry.adapter.dto.McpRegistryEntryDTO;
import org.dromara.ai.execution.registry.constant.McpRegistryConstants;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pulsar / PulseMCP 注册源适配器
 * <p>
 * 对接 PulseMCP 的 API，专注于发现已托管的远程 (SSE/HTTP) MCP 服务。
 *
 * @author KMatrix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PulsarRegistryAdapter implements RegistrySourceAdapter {

    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    /** 读取超时（毫秒） */
    private static final int READ_TIMEOUT_MS = 30_000;

    @Override
    public String getPlatform() {
        return McpRegistryConstants.PLATFORM_PULSAR;
    }

    @Override
    public List<McpRegistryEntryDTO> fetchAll(KmMcpRegistrySource source) {
        RestClient restClient = buildRestClient(source.getApiBaseUrl(), source.getApiKey());
        List<McpRegistryEntryDTO> result = new ArrayList<>();
        String cursor = null;

        try {
            log.info("[PulsarRegistry] 开始请求 PulseMCP 列表");
            // 目前 PulseMCP API 处于测试阶段，可能返回单一列表或带游标的分页
            while (true) {
                String uri = "/servers" + (StrUtil.isNotBlank(cursor) ? "?cursor=" + cursor : "");

                PulseMcpResponse response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(PulseMcpResponse.class);

                if (response == null || response.getServers() == null || response.getServers().isEmpty()) {
                    break;
                }

                for (PulseMcpServerItem item : response.getServers()) {
                    if (item == null || StrUtil.isBlank(item.getName())) {
                        continue;
                    }
                    result.add(mapToDto(item));
                }

                cursor = response.getNextCursor();
                if (StrUtil.isBlank(cursor)) {
                    break;
                }
            }
        } catch (ResourceAccessException e) {
            log.error("[PulsarRegistry] 请求超时，url={}", source.getApiBaseUrl(), e);
            throw new ServiceException("Pulsar 注册源请求超时：" + e.getMessage());
        } catch (HttpClientErrorException e) {
            log.error("[PulsarRegistry] 客户端错误，status={}", e.getStatusCode(), e);
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new ServiceException("Pulsar 注册源认证失败，请在注册源配置中检查 API Key");
            }
            throw new ServiceException("Pulsar 注册源请求失败（" + e.getStatusCode() + "）");
        } catch (Exception e) {
            log.error("[PulsarRegistry] 同步异常", e);
            throw new ServiceException("Pulsar 注册源同步异常：" + e.getMessage());
        }

        return result;
    }

    private RestClient buildRestClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 KMatrix/1.0")
                .defaultHeader("Accept", "application/json");

        if (StringUtils.hasText(apiKey)) {
            // PulseMCP 使用 X-API-Key 头
            builder.defaultHeader("X-API-Key", apiKey);
        }

        return builder.build();
    }

    private McpRegistryEntryDTO mapToDto(PulseMcpServerItem item) {
        McpRegistryEntryDTO dto = new McpRegistryEntryDTO();
        dto.setExternalId(item.getName());
        dto.setEntryName(item.getName());
        dto.setDisplayName(item.getDisplayName() != null ? item.getDisplayName() : item.getName());
        dto.setDescription(item.getDescription());
        dto.setSourcePlatform(McpRegistryConstants.PLATFORM_PULSAR);
        dto.setIconUrl(item.getIconUrl());
        dto.setHomepageUrl(item.getHomepageUrl());

        // 优先寻找远程连接 (SSE/HTTP)
        if (item.getConnections() != null && !item.getConnections().isEmpty()) {
            // 查找第一个 SSE 类型
            PulseMcpConnection remoteConn = item.getConnections().stream()
                    .filter(c -> "sse".equalsIgnoreCase(c.getType()) || "http".equalsIgnoreCase(c.getType()))
                    .findFirst()
                    .orElse(item.getConnections().get(0));

            dto.setTransportType(mapTransportType(remoteConn.getType()));
            dto.setEndpointUrl(remoteConn.getUrl());
            dto.setCommand(remoteConn.getCommand());
            dto.setArgs(remoteConn.getArgs());
            dto.setEnvVars(remoteConn.getEnv());
        } else if (item.getRemoteUrl() != null) {
            // 兼容某些返回 remoteUrl 直接字段的情况
            dto.setTransportType(McpRegistryConstants.TRANSPORT_TYPE_SSE);
            dto.setEndpointUrl(item.getRemoteUrl());
        }

        dto.setEntryStatus(McpRegistryConstants.STATUS_ACTIVE);
        return dto;
    }

    private String mapTransportType(String type) {
        if (StrUtil.isBlank(type)) return McpRegistryConstants.TRANSPORT_TYPE_SSE;
        return switch (type.toLowerCase()) {
            case "sse" -> McpRegistryConstants.TRANSPORT_TYPE_SSE;
            case "stdio" -> McpRegistryConstants.TRANSPORT_TYPE_STDIO;
            case "http", "streamable_http" -> McpRegistryConstants.TRANSPORT_TYPE_HTTP;
            default -> type.toLowerCase();
        };
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PulseMcpResponse {
        private List<PulseMcpServerItem> servers;
        private String nextCursor;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PulseMcpServerItem {
        private String name;
        private String displayName;
        private String description;
        private String iconUrl;
        private String homepageUrl;
        private String remoteUrl;
        private List<PulseMcpConnection> connections;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PulseMcpConnection {
        private String type;
        private String url;
        private String command;
        private List<String> args;
        private Map<String, String> env;
    }
}
