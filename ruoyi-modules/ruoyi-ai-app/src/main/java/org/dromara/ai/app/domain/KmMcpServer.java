package org.dromara.ai.app.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP Server 配置对象 km_mcp_server
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_mcp_server", autoResultMap = true)
public class KmMcpServer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * MCP Server ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "server_id")
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
     * Server 配置 (JSON)，包含 url、transport 等
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String serverConfig;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志
     */
    private String delFlag;

}
