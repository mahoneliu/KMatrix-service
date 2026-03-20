package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmMcpServerBo;
import org.dromara.ai.domain.vo.KmMcpServerVo;
import org.dromara.ai.service.IKmMcpServerService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP Server 配置控制器
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/mcp-server")
public class KmMcpServerController extends BaseController {

    private final IKmMcpServerService mcpServerService;

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

}
