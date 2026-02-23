package org.dromara.ai.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmModelProviderBo;
import org.dromara.ai.domain.vo.KmModelProviderVo;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
    @SaCheckPermission("ai:provider:query")
    @GetMapping("/{providerId}")
    public R<KmModelProviderVo> getInfo(@PathVariable Long providerId) {
        return R.ok(providerService.queryById(providerId));
    }

    /**
     * 修改供应商
     */
    @SaCheckPermission("ai:provider:edit")
    @Log(title = "供应商管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(
            @Validated(EditGroup.class) @RequestBody KmModelProviderBo bo) {
        return toAjax(providerService.updateByBo(bo));
    }
}
