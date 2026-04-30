package org.dromara.ai.registry.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.registry.domain.bo.McpServerManualBo;
import org.dromara.ai.registry.service.McpRegistryService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP Server 注册源扩展控制器
 * <p>
 * 提供手工添加 MCP Server 的接口，与注册源导入功能共用同一 Service。
 *
 * @author Mahone
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/km/mcp/server")
public class McpServerRegistryController extends BaseController {

    private final McpRegistryService registryService;

    /**
     * 手工添加 MCP Server
     * <p>
     * 支持 SSE、streamable_http 和 stdio 三种传输协议。
     */
    @SaCheckPermission("ai:mcpServer:add")
    @Log(title = "MCP Server 手工添加", businessType = BusinessType.INSERT)
    @PostMapping("/manual")
    public R<Object> addManualServer(@Validated @RequestBody McpServerManualBo bo) {
        return R.ok(registryService.addManualServer(bo));
    }
}
