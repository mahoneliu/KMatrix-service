package org.dromara.ai.execution.registry.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.registry.domain.bo.McpImportBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySearchBo;
import org.dromara.ai.execution.registry.domain.bo.McpRegistrySourceBo;
import org.dromara.ai.execution.registry.domain.vo.McpRegistryEntryVO;
import org.dromara.ai.execution.registry.domain.vo.McpRegistrySourceVO;
import org.dromara.ai.execution.registry.domain.vo.SyncResultVO;
import org.dromara.ai.execution.registry.service.McpRegistryService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP 注册源集成控制器
 * <p>
 * 提供注册源管理、条目搜索、条目导入等 REST 接口。
 *
 * @author Mahone
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/km/mcp/registry")
public class McpRegistryController extends BaseController {

    private final McpRegistryService registryService;

    // =========================================================================
    // 注册源管理
    // =========================================================================

    /**
     * 列出所有注册源配置
     */
    @SaCheckPermission("ai:mcpRegistry:list")
    @GetMapping("/sources")
    public R<List<McpRegistrySourceVO>> listSources() {
        return R.ok(registryService.listSources());
    }

    /**
     * 更新注册源配置（启用/禁用、同步间隔等）
     */
    @SaCheckPermission("ai:mcpRegistry:edit")
    @Log(title = "MCP 注册源管理", businessType = BusinessType.UPDATE)
    @PutMapping("/sources/{id}")
    public R<Void> updateSource(@PathVariable Long id, @RequestBody McpRegistrySourceBo bo) {
        bo.setSourceId(id);
        registryService.updateSource(bo);
        return R.ok();
    }

    /**
     * 删除注册源及其缓存条目
     */
    @SaCheckPermission("ai:mcpRegistry:remove")
    @Log(title = "MCP 注册源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/sources/{id}")
    public R<Void> deleteSource(@PathVariable Long id) {
        registryService.deleteSource(id);
        return R.ok();
    }

    /**
     * 手动触发指定注册源的全量同步
     */
    @SaCheckPermission("ai:mcpRegistry:sync")
    @Log(title = "MCP 注册源管理", businessType = BusinessType.OTHER)
    @PostMapping("/sources/{id}/sync")
    public R<SyncResultVO> syncSource(@PathVariable Long id) {
        SyncResultVO result = registryService.syncSource(id);
        return R.ok(result);
    }

    // =========================================================================
    // 注册源条目搜索
    // =========================================================================

    /**
     * 搜索注册源条目（支持关键词、来源平台、标签筛选，分页返回）
     */
    @SaCheckPermission("ai:mcpRegistry:list")
    @GetMapping("/entries")
    public TableDataInfo<McpRegistryEntryVO> searchEntries(McpRegistrySearchBo bo) {
        return registryService.searchEntries(bo);
    }

    /**
     * 获取注册源条目详情
     */
    @SaCheckPermission("ai:mcpRegistry:list")
    @GetMapping("/entries/{id}")
    public R<McpRegistryEntryVO> getEntryDetail(@PathVariable Long id) {
        return R.ok(registryService.getEntryDetail(id));
    }

    /**
     * 从注册源条目导入为 MCP Server 配置
     */
    @SaCheckPermission("ai:mcpServer:add")
    @Log(title = "MCP 注册源导入", businessType = BusinessType.INSERT)
    @PostMapping("/entries/{id}/import")
    public R<Object> importEntry(@PathVariable Long id, @RequestBody McpImportBo bo) {
        bo.setEntryId(id);
        return R.ok(registryService.importEntry(id, bo));
    }
}
