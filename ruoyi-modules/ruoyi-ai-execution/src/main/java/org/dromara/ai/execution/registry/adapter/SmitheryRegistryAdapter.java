package org.dromara.ai.execution.registry.adapter;

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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Smithery 社区市场注册源适配器
 * <p>
 * 对接 api.smithery.ai，使用页码分页（page/pageSize）拉取全量服务器列表。
 * API 文档：https://smithery.ai/docs/api-reference/servers/list-all-servers
 *
 * @author Mahone
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmitheryRegistryAdapter implements RegistrySourceAdapter {

    /** 每页拉取数量 */
    private static final int PAGE_SIZE = 100;

    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    /** 读取超时（毫秒） */
    private static final int READ_TIMEOUT_MS = 30_000;

    // =========================================================================
    // RegistrySourceAdapter 实现
    // =========================================================================

    @Override
    public String getPlatform() {
        return McpRegistryConstants.PLATFORM_SMITHERY;
    }

    @Override
    public List<McpRegistryEntryDTO> fetchAll(KmMcpRegistrySource source) {
        RestClient restClient = buildRestClient(source.getApiBaseUrl(), source.getApiKey());
        List<McpRegistryEntryDTO> result = new ArrayList<>();
        int page = 1;

        try {
            while (true) {
                String uri = "/v1/servers?page=" + page + "&pageSize=" + PAGE_SIZE;
                log.debug("[SmitheryRegistry] 拉取第 {} 页", page);

                SmitheryServersResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(SmitheryServersResponse.class);

                if (response == null || response.getServers() == null || response.getServers().isEmpty()) {
                    break;
                }

                for (SmitheryServerItem item : response.getServers()) {
                    McpRegistryEntryDTO dto = mapToDto(item);
                    result.add(dto);
                }

                // 判断是否还有下一页
                if (!hasNextPage(response, page)) {
                    break;
                }
                page++;
            }

        } catch (ResourceAccessException e) {
            log.error("[SmitheryRegistry] 请求超时，url={}", source.getApiBaseUrl(), e);
            throw new ServiceException("Smithery 注册源请求超时：" + e.getMessage());
        } catch (HttpClientErrorException e) {
            log.error("[SmitheryRegistry] 客户端错误，status={}", e.getStatusCode(), e);
            throw new ServiceException("Smithery 注册源请求失败（" + e.getStatusCode() + "）：" + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("[SmitheryRegistry] 服务端错误，status={}", e.getStatusCode(), e);
            throw new ServiceException("Smithery 注册源服务异常（" + e.getStatusCode() + "）：" + e.getResponseBodyAsString());
        }

        log.info("[SmitheryRegistry] 拉取完成，共 {} 条", result.size());
        return result;
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    /**
     * 构建带超时配置和可选 Authorization 头的 RestClient
     */
    private RestClient buildRestClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory);

        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        return builder.build();
    }

    /**
     * 判断是否还有下一页
     */
    private boolean hasNextPage(SmitheryServersResponse response, int currentPage) {
        if (response.getPagination() == null) {
            // 无分页信息时，若本页数量等于 PAGE_SIZE 则继续
            return response.getServers() != null && response.getServers().size() >= PAGE_SIZE;
        }
        SmitheryPagination pagination = response.getPagination();
        if (pagination.getTotalPages() != null) {
            return currentPage < pagination.getTotalPages();
        }
        if (pagination.getTotalCount() != null && pagination.getPageSize() != null) {
            return (long) currentPage * pagination.getPageSize() < pagination.getTotalCount();
        }
        return response.getServers() != null && response.getServers().size() >= PAGE_SIZE;
    }

    /**
     * 将 Smithery API 响应条目映射为 DTO
     */
    private McpRegistryEntryDTO mapToDto(SmitheryServerItem item) {
        McpRegistryEntryDTO dto = new McpRegistryEntryDTO();

        dto.setExternalId(item.getQualifiedName());
        dto.setEntryName(item.getQualifiedName());
        dto.setDisplayName(item.getDisplayName());
        dto.setDescription(item.getDescription());
        dto.setSourcePlatform(McpRegistryConstants.PLATFORM_SMITHERY);
        dto.setIconUrl(item.getIconUrl());
        dto.setHomepageUrl(item.getHomepage());
        dto.setUseCount(item.getUseCount());

        // 条目状态：Smithery 无明确状态字段，默认 active
        dto.setEntryStatus(McpRegistryConstants.STATUS_ACTIVE);

        // 创建时间
        if (item.getCreatedAt() != null) {
            dto.setCreateTime(parseDateTime(item.getCreatedAt()));
        }

        return dto;
    }

    /**
     * 解析 ISO 8601 时间字符串为 LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (!StringUtils.hasText(dateTimeStr)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeStr).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("[SmitheryRegistry] 时间格式解析失败：{}", dateTimeStr);
            return null;
        }
    }

    // =========================================================================
    // Smithery API 响应内部类
    // =========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SmitheryServersResponse {
        private List<SmitheryServerItem> servers;
        private SmitheryPagination pagination;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SmitheryPagination {
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private Long totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class SmitheryServerItem {
        /** 唯一限定名，如 "smithery/hello-world" */
        private String qualifiedName;
        /** 显示名称 */
        private String displayName;
        /** 描述 */
        private String description;
        /** 主页 URL */
        private String homepage;
        /** 图标 URL */
        private String iconUrl;
        /** 使用次数 */
        private Integer useCount;
        /** 是否已部署 */
        private Boolean isDeployed;
        /** 创建时间（ISO 8601 字符串） */
        private String createdAt;
        /** 是否经过验证 */
        private Boolean verified;
    }

}
