package org.dromara.ai.execution.registry.adapter.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 注册源适配器内部传输对象
 * <p>
 * 用于各平台适配器（OfficialRegistryAdapter、SmitheryRegistryAdapter 等）
 * 将外部 API 响应统一映射为此 DTO，再由 Service 层转换为 KmMcpRegistryEntry 实体。
 *
 * @author Mahone
 */
@Data
public class McpRegistryEntryDTO {

    /**
     * 外部平台的唯一标识符（如官方的 "io.github.user/repo"，Smithery 的 qualifiedName）
     */
    private String externalId;

    /**
     * 条目名称（英文标识）
     */
    private String entryName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 作者
     */
    private String author;

    /**
     * 版本号
     */
    private String version;

    /**
     * 传输类型（sse / stdio / streamable_http）
     */
    private String transportType;

    /**
     * SSE/HTTP 端点 URL
     */
    private String endpointUrl;

    /**
     * Stdio 启动命令
     */
    private String command;

    /**
     * Stdio 启动参数列表
     */
    private List<String> args;

    /**
     * 环境变量键值对
     */
    private Map<String, String> envVars;

    /**
     * 包信息（npm/docker 等），原始结构保留为 Object，由 Service 层序列化
     */
    private Object packages;

    /**
     * 是否经过 DNS 验证
     */
    private Boolean dnsVerified;

    /**
     * 来源平台标识（official / smithery）
     */
    private String sourcePlatform;

    /**
     * 条目状态（active / deprecated / deleted / offline）
     */
    private String entryStatus;

    /**
     * 社区评分（0.0~5.0）
     */
    private BigDecimal rating;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 分类标签列表
     */
    private List<String> tags;

    /**
     * 图标 URL
     */
    private String iconUrl;

    /**
     * 主页 URL
     */
    private String homepageUrl;

    /**
     * 条目在外部平台的创建时间
     */
    private LocalDateTime createTime;

    /**
     * 条目在外部平台的最后更新时间
     */
    private LocalDateTime updateTime;

}
