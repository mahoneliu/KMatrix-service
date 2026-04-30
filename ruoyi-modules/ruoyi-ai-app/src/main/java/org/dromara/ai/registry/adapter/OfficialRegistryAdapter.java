package org.dromara.ai.registry.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.registry.adapter.dto.McpRegistryEntryDTO;
import org.dromara.ai.registry.constant.McpRegistryConstants;
import org.dromara.ai.registry.domain.KmMcpRegistrySource;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 官方 MCP 注册源适配器
 * <p>
 * 对接 registry.modelcontextprotocol.io，使用游标分页（cursor/limit）拉取全量服务器列表。
 * API 文档：https://registry.modelcontextprotocol.io/v0.1/servers
 *
 * @author Mahone
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfficialRegistryAdapter implements RegistrySourceAdapter {

    /** 每页拉取数量 */
    private static final int PAGE_LIMIT = 100;

    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    /** 读取超时（毫秒） */
    private static final int READ_TIMEOUT_MS = 30_000;

    // =========================================================================
    // RegistrySourceAdapter 实现
    // =========================================================================

    @Override
    public String getPlatform() {
        return McpRegistryConstants.PLATFORM_OFFICIAL;
    }

    @Override
    public List<McpRegistryEntryDTO> fetchAll(KmMcpRegistrySource source) {
        RestClient restClient = buildRestClient(source.getApiBaseUrl());
        List<McpRegistryEntryDTO> result = new ArrayList<>();
        String cursor = null;

        try {
            do {
                // 构建请求 URI
                String uri = buildUri(cursor);
                log.debug("[OfficialRegistry] 拉取页面，cursor={}", cursor);

                OfficialServersResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OfficialServersResponse.class);

                if (response == null || response.getServers() == null || response.getServers().isEmpty()) {
                    break;
                }

                for (OfficialServerItem item : response.getServers()) {
                    McpRegistryEntryDTO dto = mapToDto(item);
                    result.add(dto);
                }

                // 获取下一页游标
                cursor = (response.getMetadata() != null) ? response.getMetadata().getNextCursor() : null;

            } while (cursor != null && !cursor.isBlank());

        } catch (ResourceAccessException e) {
            log.error("[OfficialRegistry] 请求超时，url={}", source.getApiBaseUrl(), e);
            throw new ServiceException("官方注册源请求超时：" + e.getMessage());
        } catch (HttpClientErrorException e) {
            log.error("[OfficialRegistry] 客户端错误，status={}", e.getStatusCode(), e);
            throw new ServiceException("官方注册源请求失败（" + e.getStatusCode() + "）：" + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("[OfficialRegistry] 服务端错误，status={}", e.getStatusCode(), e);
            throw new ServiceException("官方注册源服务异常（" + e.getStatusCode() + "）：" + e.getResponseBodyAsString());
        }

        log.info("[OfficialRegistry] 拉取完成，共 {} 条", result.size());
        return result;
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /**
     * 构建带超时配置的 RestClient
     */
    private RestClient buildRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build();
    }

    /**
     * 构建分页请求 URI
     */
    private String buildUri(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return "/v0.1/servers?limit=" + PAGE_LIMIT;
        }
        return "/v0.1/servers?limit=" + PAGE_LIMIT + "&cursor=" + cursor;
    }

    /**
     * 将官方 API 响应条目映射为 DTO
     */
    private McpRegistryEntryDTO mapToDto(OfficialServerItem item) {
        McpRegistryEntryDTO dto = new McpRegistryEntryDTO();

        dto.setExternalId(item.getName());
        dto.setEntryName(item.getName());
        dto.setDisplayName(item.getTitle() != null ? item.getTitle() : item.getName());
        dto.setDescription(item.getDescription());
        dto.setSourcePlatform(McpRegistryConstants.PLATFORM_OFFICIAL);

        // 版本信息
        dto.setVersion(item.getVersion());

        // 作者/发布者
        if (item.getPublisher() != null) {
            dto.setAuthor(item.getPublisher().getName());
        }

        // 连接信息：取第一个 connection
        if (item.getConnections() != null && !item.getConnections().isEmpty()) {
            OfficialConnection conn = item.getConnections().get(0);
            String type = conn.getType();
            if (type != null) {
                // 官方类型映射：sse -> sse, stdio -> stdio, http -> streamable_http
                dto.setTransportType(mapTransportType(type));
            }
            dto.setEndpointUrl(conn.getUrl());
            dto.setCommand(conn.getCommand());
            dto.setArgs(conn.getArgs());
            dto.setEnvVars(conn.getEnv());
        }

        // 包信息
        dto.setPackages(item.getPackages());

        // 条目状态
        String status = item.getStatus();
        dto.setEntryStatus(status != null ? status : McpRegistryConstants.STATUS_ACTIVE);

        return dto;
    }

    /**
     * 将官方传输类型映射为内部标准类型
     */
    private String mapTransportType(String officialType) {
        return switch (officialType.toLowerCase()) {
            case "sse" -> "sse";
            case "stdio" -> "stdio";
            case "http", "streamable_http", "streamablehttp" -> "streamable_http";
            default -> officialType.toLowerCase();
        };
    }

    // =========================================================================
    // 官方 API 响应内部类
    // =========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OfficialServersResponse {
        private List<OfficialServerItem> servers;
        private OfficialMetadata metadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OfficialMetadata {
        private Integer count;
        private String nextCursor;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OfficialServerItem {
        /** 服务器唯一名称，如 "io.github.user/repo" */
        private String name;
        /** 显示标题 */
        private String title;
        /** 描述 */
        private String description;
        /** 版本号 */
        private String version;
        /** 状态（active / deprecated / deleted） */
        private String status;
        /** 发布者信息 */
        private OfficialPublisher publisher;
        /** 连接配置列表 */
        private List<OfficialConnection> connections;
        /** 包信息（npm/docker 等） */
        private Object packages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OfficialPublisher {
        private String name;
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OfficialConnection {
        /** 传输类型：sse / stdio / http */
        private String type;
        /** SSE/HTTP 端点 URL */
        private String url;
        /** Stdio 命令 */
        private String command;
        /** Stdio 参数列表 */
        private List<String> args;
        /** 环境变量 */
        private Map<String, String> env;
    }

}
