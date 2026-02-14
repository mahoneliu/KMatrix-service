package org.dromara.ai.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmModelProviderBo;
import org.dromara.ai.domain.vo.KmModelProviderVo;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型供应商控制器
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/provider")
public class KmModelProviderController extends BaseController {

    private final IKmModelProviderService providerService;

    /**
     * 查询供应商列表
     */
    @GetMapping("/list")
    public R<List<KmModelProviderVo>> list(KmModelProviderBo bo) {
        return R.ok(providerService.queryList(bo));
    }

    /**
     * 获取供应商详细信息
     */
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:provider:query")
    @GetMapping("/{providerId}")
    public R<KmModelProviderVo> getInfo(@org.springframework.web.bind.annotation.PathVariable Long providerId) {
        return R.ok(providerService.queryById(providerId));
    }

    /**
     * 修改供应商
     */
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:provider:edit")
    @org.dromara.common.log.annotation.Log(title = "供应商管理", businessType = org.dromara.common.log.enums.BusinessType.UPDATE)
    @org.springframework.web.bind.annotation.PutMapping
    public R<Void> edit(
            @org.springframework.validation.annotation.Validated(org.dromara.common.core.validate.EditGroup.class) @org.springframework.web.bind.annotation.RequestBody KmModelProviderBo bo) {
        return toAjax(providerService.updateByBo(bo));
    }
}
