package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmDataSourceBo;
import org.dromara.ai.domain.vo.KmDataSourceVo;
import org.dromara.ai.service.IKmDataSourceService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.core.annotation.DemoBlock;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源配置控制器
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/datasource")
public class KmDataSourceController extends BaseController {

    private final IKmDataSourceService dataSourceService;

    /**
     * 查询数据源列表
     */
    @SaCheckPermission("ai:datasource:list")
    @GetMapping("/list")
    public R<List<KmDataSourceVo>> list(KmDataSourceBo bo) {
        return R.ok(dataSourceService.queryList(bo));
    }

    /**
     * 获取数据源详细信息
     */
    @SaCheckPermission("ai:datasource:query")
    @GetMapping("/{dataSourceId}")
    public R<KmDataSourceVo> getInfo(@PathVariable Long dataSourceId) {
        return R.ok(dataSourceService.queryById(dataSourceId));
    }

    /**
     * 新增数据源
     */
    @SaCheckPermission("ai:datasource:add")
    @Log(title = "数据源管理", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmDataSourceBo bo) {
        return toAjax(dataSourceService.insertByBo(bo));
    }

    /**
     * 修改数据源
     */
    @DemoBlock
    @SaCheckPermission("ai:datasource:edit")
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmDataSourceBo bo) {
        return toAjax(dataSourceService.updateByBo(bo));
    }

    /**
     * 删除数据源
     */
    @DemoBlock
    @SaCheckPermission("ai:datasource:remove")
    @Log(title = "数据源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dataSourceIds}")
    public R<Void> remove(@PathVariable List<Long> dataSourceIds) {
        return toAjax(dataSourceService.deleteByIds(dataSourceIds));
    }

    /**
     * 测试数据源连接
     */
    @SaCheckPermission("ai:datasource:query")
    @PostMapping("/test/{dataSourceId}")
    public R<Void> testConnection(@PathVariable Long dataSourceId) {
        return toAjax(dataSourceService.testConnection(dataSourceId));
    }

    /**
     * 获取可用的动态数据源列表
     */
    @SaCheckPermission("ai:datasource:list")
    @GetMapping("/dynamic-keys")
    public R<List<String>> getDynamicDataSourceKeys() {
        return R.ok(dataSourceService.getDynamicDataSourceKeys());
    }

}
