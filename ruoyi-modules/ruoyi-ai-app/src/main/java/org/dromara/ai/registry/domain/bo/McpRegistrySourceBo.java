package org.dromara.ai.registry.domain.bo;

import lombok.Data;

/**
 * MCP 注册源更新请求对象
 *
 * @author Mahone
 */
@Data
public class McpRegistrySourceBo {

    /**
     * 注册源 ID
     */
    private Long sourceId;

    /**
     * 注册源名称
     */
    private String sourceName;

    /**
     * 同步间隔（秒），最小 3600（1小时），最大 604800（7天）
     */
    private Integer syncInterval;

    /**
     * 是否启用（1=启用，0=禁用）
     */
    private String isEnabled;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 备注
     */
    private String remark;

}
