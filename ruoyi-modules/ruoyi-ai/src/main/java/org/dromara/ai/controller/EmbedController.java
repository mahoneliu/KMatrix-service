package org.dromara.ai.controller;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.IdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.bo.AnonymousAuthBo;
import org.dromara.ai.domain.vo.AnonymousAuthVo;
import org.dromara.ai.domain.vo.ChatSessionTokenInfo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IChatSessionTokenService;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.service.IKmAppTokenService;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 嵌入对话脚本Controller
 * 提供可嵌入第三方页面的 JavaScript 代码和匿名认证
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class EmbedController {

    private final IKmAppTokenService appTokenService;
    private final IKmAppService appService;
    private final IChatSessionTokenService chatSessionTokenService;

    /**
     * Token 有效期（天），默认 30 天
     */
    @Value("${kmatrix.chat.anonymous-token-expire-days:30}")
    private int tokenExpireDays;

    /**
     * 匿名用户认证
     * 验证 appToken 后生成 Session Token
     */
    @PostMapping("/anonymous-auth")
    public R<AnonymousAuthVo> anonymousAuthentication(@Valid @RequestBody AnonymousAuthBo bo) {
        // 1. 验证 appToken
        Long appId = appTokenService.validateToken(bo.getAppToken(), null);
        if (appId == null) {
            return R.fail(MessageUtils.message("ai.msg.auth.invalid_app_token"));
        }

        // 2. 生成 userId (雪花 ID)
        Long userId = IdUtil.getSnowflakeNextId();

        // 3. 生成 JWT Session Token
        String sessionToken = chatSessionTokenService.generateToken(appId, bo.getAppToken(), userId);

        // 4. 计算过期时间
        long expireTime = System.currentTimeMillis() + (long) tokenExpireDays * 24 * 60 * 60 * 1000;

        // 5. 返回结果
        AnonymousAuthVo vo = new AnonymousAuthVo();
        vo.setSessionToken(sessionToken);
        vo.setUserId(userId);
        vo.setAppId(appId);
        vo.setExpireTime(expireTime);

        log.info("匿名用户认证成功: appId={}, userId={}", appId, userId);
        return R.ok(vo);
    }

    /**
     * 获取应用信息 (免登录，通过 Token)
     */
    @GetMapping("/app-info")
    public R<KmAppVo> getAppInfo(
            @RequestParam("token") String token) {
        // 1. 尝试验证 Session Token
        ChatSessionTokenInfo sessionInfo = chatSessionTokenService.validateToken(token);
        if (sessionInfo != null) {
            return R.ok(appService.queryById(sessionInfo.getAppId()));
        }

        // 2. 尝试验证 App Token
        Long appId = appTokenService.validateToken(token, null);
        if (appId == null) {
            return R.fail(MessageUtils.message("ai.msg.auth.invalid_token"));
        }
        return R.ok(appService.queryById(appId));
    }

}
