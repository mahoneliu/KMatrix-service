package org.dromara.ai.execution.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.execution.domain.KmMcpServer;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * MCP Server 业务对象 km_mcp_server
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmMcpServer.class, reverseConvertGenerate = false)
public class KmMcpServerBo extends BaseEntity {

    /**
     * MCP Server ID
     */
    private Long serverId;

    /**
     * MCP Server 名称
     */
    @NotBlank(message = "MCP Server 名称不能为空")
    @Size(max = 64, message = "MCP Server 名称长度不能超过64个字符")
    private String serverName;

    /**
     * 描述
     */
    @Size(max = 128, message = "描述长度不能超过128个字符")
    private String description;

    /**
     * 传输类型（sse / streamable_http）
     */
    @NotBlank(message = "传输类型不能为空")
    private String transportType;

    /**
     * Server 配置 (JSON)
     */
    private String serverConfig;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 来源注册源 ID（关联 km_mcp_registry_source，手工导入时为 null）
     */
    private Long sourceRegistryId;

    /**
     * 来源注册源条目 ID（关联 km_mcp_registry_entry，手工导入时为 null）
     */
    private Long sourceEntryId;

    /**
     * 导入来源（manual=手工添加，registry=从注册源导入）
     */
    private String importSource;

}
