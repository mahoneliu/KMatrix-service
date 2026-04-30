package org.dromara.ai.execution.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.execution.domain.KmMcpServer;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * MCP Server 视图对象 km_mcp_server
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@AutoMapper(target = KmMcpServer.class)
public class KmMcpServerVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MCP Server ID
     */
    private Long serverId;

    /**
     * MCP Server 名称
     */
    private String serverName;

    /**
     * 描述
     */
    private String description;

    /**
     * 传输类型（sse / streamable_http）
     */
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
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新时间
     */
    private Date updateTime;

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
