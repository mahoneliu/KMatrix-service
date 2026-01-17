package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmModelBo;
import org.dromara.ai.domain.vo.KmModelVo;
import org.dromara.ai.service.IKmModelService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI模型配置控制器
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/model")
public class KmModelController extends BaseController {

    private final IKmModelService modelService;

    /**
     * 查询模型列表
     */
    @SaCheckPermission("ai:model:list")
    @GetMapping("/list")
    public R<List<KmModelVo>> list(KmModelBo bo) {
        return R.ok(modelService.queryList(bo));
    }

    /**
     * 获取模型详细信息
     */
    @SaCheckPermission("ai:model:query")
    @GetMapping("/{modelId}")
    public R<KmModelVo> getInfo(@PathVariable Long modelId) {
        return R.ok(modelService.queryById(modelId));
    }

    /**
     * 新增模型
     */
    @SaCheckPermission("ai:model:add")
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmModelBo bo) {
        return toAjax(modelService.insertByBo(bo));
    }

    /**
     * 修改模型
     */
    @SaCheckPermission("ai:model:edit")
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmModelBo bo) {
        return toAjax(modelService.updateByBo(bo));
    }

    /**
     * 删除模型
     */
    /**
     * 删除模型
     */
    @SaCheckPermission("ai:model:remove")
    @Log(title = "模型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public R<Void> remove(@PathVariable List<Long> modelIds) {
        return toAjax(modelService.deleteByIds(modelIds));
    }

    /**
     * 测试模型连接
     */
    @SaCheckPermission("ai:model:query")
    @PostMapping("/test-connection")
    public R<String> testConnection(@RequestBody KmModelBo bo) {
        String result = modelService.testConnection(bo);
        if (result.startsWith("连接成功") || result.contains("成功")) {
            return R.ok(result);
        }
        return R.fail(result);
    }
}
