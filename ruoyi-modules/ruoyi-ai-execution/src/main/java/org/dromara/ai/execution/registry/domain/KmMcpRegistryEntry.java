package org.dromara.ai.execution.registry.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MCP 注册源条目缓存对象 km_mcp_registry_entry
 * <p>
 * 注意：此表无审计字段（create_by/update_by），create_time/update_time 来自外部平台，
 * 因此不继承 BaseEntity，直接实现 Serializable。
 *
 * @author Mahone
 */
@Data
@TableName(value = "km_mcp_registry_entry")
public class KmMcpRegistryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目 ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "entry_id")
    private Long entryId;

    /**
     * 所属注册源 ID，关联 km_mcp_registry_source
     */
    private Long sourceId;

    /**
     * 外部平台的唯一标识符
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
     * 传输类型（sse=SSE，stdio=标准输入输出，streamable_http=流式 HTTP）
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
     * Stdio 启动参数列表（JSON 数组，JSONB 字段用 String 存储）
     */
    private String args;

    /**
     * 环境变量（JSON 对象，JSONB 字段用 String 存储）
     */
    private String envVars;

    /**
     * 包信息，如 npm/docker 等（JSON，JSONB 字段用 String 存储）
     */
    private String packages;

    /**
     * 是否经过 DNS 验证
     */
    private Boolean dnsVerified;

    /**
     * 来源平台（official=官方注册源，smithery=Smithery）
     */
    private String sourcePlatform;

    /**
     * 条目状态（active=活跃，deprecated=已废弃，deleted=已删除，offline=已下线）
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
     * 分类标签列表（JSON 数组，JSONB 字段用 String 存储）
     */
    private String tags;

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
