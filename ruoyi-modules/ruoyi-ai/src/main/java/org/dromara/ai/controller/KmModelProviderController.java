package org.dromara.ai.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.bo.KmModelProviderBo;
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
    public R<List<KmModelProvider>> list(KmModelProviderBo bo) {
        return R.ok(providerService.queryList(bo));
    }
}
