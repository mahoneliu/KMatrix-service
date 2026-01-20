package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmDatabaseMetaBo;
import org.dromara.ai.domain.vo.KmDatabaseMetaVo;
import org.dromara.ai.service.IKmDatabaseMetaService;
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
 * 数据库元数据控制器
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/database-meta")
public class KmDatabaseMetaController extends BaseController {

    private final IKmDatabaseMetaService databaseMetaService;

    /**
     * 查询元数据列表
     */
    @SaCheckPermission("ai:datasource:list")
    @GetMapping("/list")
    public R<List<KmDatabaseMetaVo>> list(KmDatabaseMetaBo bo) {
        return R.ok(databaseMetaService.queryList(bo));
    }

    /**
     * 根据数据源ID查询元数据
     */
    @SaCheckPermission("ai:datasource:query")
    @GetMapping("/by-datasource/{dataSourceId}")
    public R<List<KmDatabaseMetaVo>> getByDataSource(@PathVariable Long dataSourceId) {
        return R.ok(databaseMetaService.queryByDataSourceId(dataSourceId));
    }

    /**
     * 获取元数据详细信息
     */
    @SaCheckPermission("ai:datasource:query")
    @GetMapping("/{metaId}")
    public R<KmDatabaseMetaVo> getInfo(@PathVariable Long metaId) {
        return R.ok(databaseMetaService.queryById(metaId));
    }

    /**
     * 新增元数据
     */
    @SaCheckPermission("ai:datasource:add")
    @Log(title = "数据库元数据", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmDatabaseMetaBo bo) {
        return toAjax(databaseMetaService.insertByBo(bo));
    }

    /**
     * 修改元数据
     */
    @SaCheckPermission("ai:datasource:edit")
    @Log(title = "数据库元数据", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmDatabaseMetaBo bo) {
        return toAjax(databaseMetaService.updateByBo(bo));
    }

    /**
     * 删除元数据
     */
    @SaCheckPermission("ai:datasource:remove")
    @Log(title = "数据库元数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{metaIds}")
    public R<Void> remove(@PathVariable List<Long> metaIds) {
        return toAjax(databaseMetaService.deleteByIds(metaIds));
    }

    /**
     * 解析DDL并保存元数据
     */
    @SaCheckPermission("ai:datasource:add")
    @Log(title = "数据库元数据", businessType = BusinessType.INSERT)
    @PostMapping("/parse-ddl")
    public R<List<KmDatabaseMetaVo>> parseDdl(@RequestParam Long dataSourceId, @RequestBody String ddlContent) {
        return R.ok(databaseMetaService.parseDdlAndSave(dataSourceId, ddlContent));
    }

    /**
     * 从数据库同步元数据
     */
    @SaCheckPermission("ai:datasource:add")
    @Log(title = "数据库元数据", businessType = BusinessType.INSERT)
    @PostMapping("/syncFromDatabase/{dataSourceId}")
    public R<List<KmDatabaseMetaVo>> syncFromDatabase(
            @PathVariable Long dataSourceId,
            @RequestBody(required = false) List<String> tableNames) {
        return R.ok(databaseMetaService.syncFromDatabase(dataSourceId, tableNames));
    }

}
