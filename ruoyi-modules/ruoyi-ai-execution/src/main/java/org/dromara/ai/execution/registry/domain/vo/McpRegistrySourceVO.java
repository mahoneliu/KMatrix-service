package org.dromara.ai.execution.registry.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MCP 注册源配置视图对象
 *
 * @author Mahone
 */
@Data
@AutoMapper(target = KmMcpRegistrySource.class)
public class McpRegistrySourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 注册源 ID
     */
    private Long sourceId;

    /**
     * 注册源名称
     */
    private String sourceName;

    /**
     * 注册源类型（official=官方，community=社区）
     */
    private String sourceType;

    /**
     * 平台标识（official=官方注册源，smithery=Smithery 社区市场）
     */
    private String platform;

    /**
     * 注册源 API 基础 URL
     */
    private String apiBaseUrl;

    /**
     * 同步间隔（秒），最小 3600（1小时），最大 604800（7天）
     */
    private Integer syncInterval;

    /**
     * 是否启用（1=启用，0=禁用）
     */
    private String isEnabled;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 最后同步条目数量
     */
    private Integer lastSyncCount;

    /**
     * 最后同步状态（success=成功，failed=失败，running=同步中）
     */
    private String lastSyncStatus;

    /**
     * 最后同步错误信息
     */
    private String lastSyncError;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
