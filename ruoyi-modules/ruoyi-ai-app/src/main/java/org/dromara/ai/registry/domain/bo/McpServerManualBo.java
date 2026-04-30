package org.dromara.ai.registry.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 手工导入 MCP Server 请求对象
 *
 * @author Mahone
 */
@Data
public class McpServerManualBo {

    /**
     * MCP Server 名称（必填）
     */
    @NotBlank(message = "MCP Server 名称不能为空")
    private String serverName;

    /**
     * 传输类型（sse / stdio，必填）
     */
    @NotBlank(message = "传输类型不能为空")
    private String transportType;

    /**
     * SSE/HTTP 端点 URL（SSE 协议时必填）
     */
    private String endpointUrl;

    /**
     * Stdio 启动命令（Stdio 协议时必填）
     */
    private String command;

    /**
     * Stdio 启动参数列表（Stdio 协议时可选）
     */
    private List<String> args;

    /**
     * 环境变量（可选）
     */
    private Map<String, String> envVars;

    /**
     * 描述（可选）
     */
    private String description;

}
