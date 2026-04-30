package org.dromara.ai.execution.domain.bo;

import lombok.Data;

/**
 * MCP 连接测试业务对象
 *
 * @author Kiro
 */
@Data
public class McpConnectionTestBo {

    /**
     * 已保存的 MCP Server ID（与 serverConfig 二选一）
     */
    private Long serverId;

    /**
     * 临时配置 JSON（Import Wizard 中使用，与 serverId 二选一）
     */
    private String serverConfig;

    /**
     * 传输类型（临时配置时必填）
     */
    private String transportType;

}
