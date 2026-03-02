package org.dromara.ai.controller;

import org.dromara.common.core.utils.MessageUtils;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmAppTokenBo;
import org.dromara.ai.domain.vo.KmAppTokenVo;
import org.dromara.ai.service.IKmAppTokenService;
import org.dromara.common.core.annotation.DemoBlock;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App嵌入Token Controller
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/app-token")
public class KmAppTokenController extends BaseController {

    private final IKmAppTokenService appTokenService;

    /**
     * 根据应用ID查询Token列表
     */
    @SaCheckPermission("ai:app:edit")
    @GetMapping("/list/{appId}")
    public R<List<KmAppTokenVo>> list(@PathVariable Long appId) {
        return R.ok(appTokenService.queryByAppId(appId));
    }

    /**
     * 根据ID查询Token详情
     */
    @SaCheckPermission("ai:app:edit")
    @GetMapping("/{tokenId}")
    public R<KmAppTokenVo> getInfo(@PathVariable Long tokenId) {
        return R.ok(appTokenService.queryById(tokenId));
    }

    /**
     * 生成新Token
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "生成App嵌入Token", businessType = BusinessType.INSERT)
    @PostMapping
    public R<KmAppTokenVo> generate(@Valid @RequestBody KmAppTokenBo bo) {
        return R.ok(appTokenService.generateToken(bo));
    }

    /**
     * 更新Token
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "更新App嵌入Token", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@Valid @RequestBody KmAppTokenBo bo) {
        return toAjax(appTokenService.updateToken(bo));
    }

    /**
     * 删除Token
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "删除App嵌入Token", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tokenId}")
    public R<Void> delete(@PathVariable Long tokenId) {
        return toAjax(appTokenService.deleteToken(tokenId));
    }

    /**
     * 刷新Token（重新生成token值）
     */
    @DemoBlock
    @SaCheckPermission("ai:app:edit")
    @Log(title = "刷新App嵌入Token", businessType = BusinessType.UPDATE)
    @PutMapping("/refresh/{tokenId}")
    public R<KmAppTokenVo> refresh(@PathVariable Long tokenId) {
        KmAppTokenVo result = appTokenService.refreshToken(tokenId);
        if (result == null) {
            return R.fail(MessageUtils.message("ai.msg.auth.token_not_found"));
        }
        return R.ok(result);
    }
}
