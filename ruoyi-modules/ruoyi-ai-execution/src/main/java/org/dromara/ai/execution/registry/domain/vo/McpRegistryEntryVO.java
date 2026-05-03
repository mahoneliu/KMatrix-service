package org.dromara.ai.execution.registry.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.execution.registry.domain.KmMcpRegistryEntry;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * MCP 注册源条目视图对象
 *
 * @author Mahone
 */
@Data
@AutoMapper(target = KmMcpRegistryEntry.class)
public class McpRegistryEntryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目 ID
     */
    private Long entryId;

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
     * Stdio 启动参数列表（JSON 数组）
     */
    private String args;

    /**
     * 来源平台（official=官方注册源，smithery=Smithery）
     */
    private String sourcePlatform;

    /**
     * 是否经过 DNS 验证
     */
    private Boolean dnsVerified;

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
     * 分类标签列表（JSON 数组）
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
     * 是否已导入为 MCP Server（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean isImported;

}
