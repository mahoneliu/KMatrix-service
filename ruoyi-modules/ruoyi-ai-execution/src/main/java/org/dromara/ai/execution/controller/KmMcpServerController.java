package org.dromara.ai.execution.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.domain.bo.KmMcpServerBo;
import org.dromara.ai.execution.domain.bo.McpConnectionTestBo;
import org.dromara.ai.execution.domain.vo.KmMcpServerVo;
import org.dromara.ai.execution.domain.vo.McpConnectionTestResultVo;
import org.dromara.ai.execution.mcp.service.McpClientManager;
import org.dromara.ai.execution.mcp.transport.McpTransportFactory;
import org.dromara.ai.execution.service.IKmMcpServerService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import java.util.List;

/**
 * MCP Server 配置控制器
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/mcp-server")
public class KmMcpServerController extends BaseController {

    private final IKmMcpServerService mcpServerService;
    private final McpClientManager mcpClientManager;

    /**
     * 查询 MCP Server 提供的资源列表
     */
    @SaCheckPermission("ai:mcpServer:list")
    @GetMapping("/{serverId}/resources")
    public R<Object> listResources(@PathVariable Long serverId) {
        return R.ok(mcpClientManager.listResources(serverId));
    }

    /**
     * 查询 MCP Server 列表
     */
    @SaCheckPermission("ai:mcpServer:list")
    @GetMapping("/list")
    public R<List<KmMcpServerVo>> list(KmMcpServerBo bo) {
        return R.ok(mcpServerService.queryList(bo));
    }

    /**
     * 获取 MCP Server 详细信息
     */
    @SaCheckPermission("ai:mcpServer:query")
    @GetMapping("/{serverId}")
    public R<KmMcpServerVo> getInfo(@PathVariable Long serverId) {
        return R.ok(mcpServerService.queryById(serverId));
    }

    /**
     * 新增 MCP Server
     */
    @SaCheckPermission("ai:mcpServer:add")
    @Log(title = "MCP Server 管理", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmMcpServerBo bo) {
        return toAjax(mcpServerService.insertByBo(bo));
    }

    /**
     * 修改 MCP Server
     */
    @SaCheckPermission("ai:mcpServer:edit")
    @Log(title = "MCP Server 管理", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmMcpServerBo bo) {
        return toAjax(mcpServerService.updateByBo(bo));
    }

    /**
     * 删除 MCP Server
     */
    @SaCheckPermission("ai:mcpServer:remove")
    @Log(title = "MCP Server 管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{serverIds}")
    public R<Void> remove(@PathVariable List<Long> serverIds) {
        return toAjax(mcpServerService.deleteByIds(serverIds));
    }

    /**
     * 测试 MCP Server 连接
     * <p>
     * 支持两种模式：
     * 1. 传入 serverId：从数据库加载配置后测试
     * 2. 传入 serverConfig + transportType：使用临时配置测试（Import Wizard 场景）
     */
    @SaCheckPermission("ai:mcpServer:query")
    @PostMapping("/test-connection")
    public R<McpConnectionTestResultVo> testConnection(@RequestBody McpConnectionTestBo bo) {
        long start = System.currentTimeMillis();
        McpClient client = null;
        boolean tempClient = false;

        try {
            if (bo.getServerId() != null) {
                // 模式1：已保存的 Server，走缓存
                client = mcpClientManager.getClient(bo.getServerId());
            } else if (bo.getServerConfig() != null) {
                // 模式2：临时配置，创建一次性 client，不入缓存
                String url = McpTransportFactory.extractServerUrl(bo.getServerConfig());
                if (url == null || url.isBlank()) {
                    return R.ok(McpConnectionTestResultVo.builder()
                            .success(false)
                            .tools(List.of())
                            .errorMessage("serverConfig 中缺少 url 字段")
                            .elapsedMs(System.currentTimeMillis() - start)
                            .build());
                }
                var headers = McpTransportFactory.extractServerHeaders(bo.getServerConfig());
                var transport = McpTransportFactory.createHttpTransport(url, headers, bo.getTransportType());
                if (transport == null) {
                    return R.ok(McpConnectionTestResultVo.builder()
                            .success(false)
                            .tools(List.of())
                            .errorMessage("不支持的传输类型: " + bo.getTransportType())
                            .elapsedMs(System.currentTimeMillis() - start)
                            .build());
                }
                client = DefaultMcpClient.builder()
                        .transport(transport)
                        .toolExecutionTimeout(Duration.ofSeconds(15))
                        .build();
                tempClient = true;
            } else {
                return R.ok(McpConnectionTestResultVo.builder()
                        .success(false)
                        .tools(List.of())
                        .errorMessage("请提供 serverId 或 serverConfig")
                        .elapsedMs(0L)
                        .build());
            }

            if (client == null) {
                return R.ok(McpConnectionTestResultVo.builder()
                        .success(false)
                        .tools(List.of())
                        .errorMessage("MCP Server 不可用或配置错误")
                        .elapsedMs(System.currentTimeMillis() - start)
                        .build());
            }

            List<ToolSpecification> tools = client.listTools();
            List<McpConnectionTestResultVo.McpToolVo> toolVos = tools.stream()
                    .map(t -> McpConnectionTestResultVo.McpToolVo.builder()
                            .name(t.name())
                            .description(t.description())
                            .build())
                    .collect(Collectors.toList());

            return R.ok(McpConnectionTestResultVo.builder()
                    .success(true)
                    .tools(toolVos)
                    .errorMessage(null)
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build());

        } catch (Exception e) {
            log.warn("MCP 连接测试失败: {}", e.getMessage());
            return R.ok(McpConnectionTestResultVo.builder()
                    .success(false)
                    .tools(List.of())
                    .errorMessage(e.getMessage())
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build());
        } finally {
            // 临时 client 用完即关（DefaultMcpClient 目前无 close，移除引用即可）
            if (tempClient) {
                client = null;
            }
        }
    }

}
