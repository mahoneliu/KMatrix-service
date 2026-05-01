package org.dromara.ai.execution.registry.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * MCP 注册源配置对象 km_mcp_registry_source
 *
 * @author Mahone
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_mcp_registry_source")
public class KmMcpRegistrySource extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 注册源 ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "source_id")
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
     * API 密钥（加密存储，部分平台需要）
     */
    private String apiKey;

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
     * 删除标志（0=未删除，2=已删除）
     */
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
