package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmBuiltinToolBo;
import org.dromara.ai.domain.vo.KmBuiltinToolVo;
import org.dromara.ai.service.IKmBuiltinToolService;
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
 * 内置 Python 工具控制器
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/builtin-tool")
public class KmBuiltinToolController extends BaseController {

    private final IKmBuiltinToolService builtinToolService;

    /**
     * 查询内置工具列表
     */
    @SaCheckPermission("ai:builtinTool:list")
    @GetMapping("/list")
    public R<List<KmBuiltinToolVo>> list(KmBuiltinToolBo bo) {
        return R.ok(builtinToolService.queryList(bo));
    }

    /**
     * 获取内置工具详细信息
     */
    @SaCheckPermission("ai:builtinTool:query")
    @GetMapping("/{toolId}")
    public R<KmBuiltinToolVo> getInfo(@PathVariable Long toolId) {
        return R.ok(builtinToolService.queryById(toolId));
    }

    /**
     * 新增内置工具
     */
    @SaCheckPermission("ai:builtinTool:add")
    @Log(title = "内置工具管理", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmBuiltinToolBo bo) {
        return toAjax(builtinToolService.insertByBo(bo));
    }

    /**
     * 修改内置工具
     */
    @SaCheckPermission("ai:builtinTool:edit")
    @Log(title = "内置工具管理", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmBuiltinToolBo bo) {
        return toAjax(builtinToolService.updateByBo(bo));
    }

    /**
     * 删除内置工具
     */
    @SaCheckPermission("ai:builtinTool:remove")
    @Log(title = "内置工具管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{toolIds}")
    public R<Void> remove(@PathVariable List<Long> toolIds) {
        return toAjax(builtinToolService.deleteByIds(toolIds));
    }

}
