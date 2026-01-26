package org.dromara.ai.service.impl;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.vo.ChatSessionTokenInfo;
import org.dromara.ai.enums.ChatUserType;
import org.dromara.ai.service.IChatSessionTokenService;
import org.dromara.ai.service.IKmAppTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 对话 Session Token 服务实现
 * 使用 Hutool JWT 实现，复用 Sa-Token 的密钥配置
 *
 * @author Mahone
 * @date 2026-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionTokenServiceImpl implements IChatSessionTokenService {

    private final IKmAppTokenService appTokenService;

    /**
     * JWT 密钥，复用 Sa-Token 配置
     */
    @Value("${sa-token.jwt-secret-key:kmatrix-default-secret-key}")
    private String jwtSecretKey;

    /**
     * Token 有效期（天），默认 30 天
     */
    @Value("${kmatrix.chat.anonymous-token-expire-days:30}")
    private int tokenExpireDays;

    // JWT 声明名称常量
    private static final String CLAIM_APP_ID = "app_id";
    private static final String CLAIM_APP_TOKEN = "app_token";
    private static final String CLAIM_USER_ID = "user_id";
    private static final String CLAIM_USER_TYPE = "user_type";

    @Override
    public String generateToken(Long appId, String appToken, Long userId) {
        long expireTime = System.currentTimeMillis() + (long) tokenExpireDays * 24 * 60 * 60 * 1000;

        return JWT.create()
                .setPayload(CLAIM_APP_ID, appId)
                .setPayload(CLAIM_APP_TOKEN, appToken)
                .setPayload(CLAIM_USER_ID, userId)
                .setPayload(CLAIM_USER_TYPE, ChatUserType.ANONYMOUS_USER.getCode())
                .setIssuedAt(new Date())
                .setExpiresAt(new Date(expireTime))
                .setKey(jwtSecretKey.getBytes())
                .sign();
    }

    @Override
    public ChatSessionTokenInfo parseToken(String sessionToken) {
        try {
            JWT jwt = JWTUtil.parseToken(sessionToken);

            // 验证签名
            if (!jwt.setKey(jwtSecretKey.getBytes()).verify()) {
                log.warn("Session Token 签名验证失败");
                return null;
            }

            // 验证是否过期
            try {
                JWTValidator.of(jwt).validateDate();
            } catch (Exception e) {
                log.warn("Session Token 已过期: {}", e.getMessage());
                return null;
            }

            ChatSessionTokenInfo info = new ChatSessionTokenInfo();
            info.setAppId(Long.parseLong(jwt.getPayload(CLAIM_APP_ID).toString()));
            info.setAppToken((String) jwt.getPayload(CLAIM_APP_TOKEN));
            info.setUserId(Long.parseLong(jwt.getPayload(CLAIM_USER_ID).toString()));
            info.setUserType(ChatUserType.fromCode((String) jwt.getPayload(CLAIM_USER_TYPE)));

            Object expObj = jwt.getPayload(JWT.EXPIRES_AT);
            if (expObj != null) {
                info.setExpireTime(Long.parseLong(expObj.toString()) * 1000);
            }

            return info;
        } catch (Exception e) {
            log.error("解析 Session Token 失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ChatSessionTokenInfo validateToken(String sessionToken) {
        ChatSessionTokenInfo info = parseToken(sessionToken);
        if (info == null) {
            return null;
        }

        // 二次验证：检查 appToken 是否仍然有效
        Long validAppId = appTokenService.validateToken(info.getAppToken(), null);
        if (validAppId == null || !validAppId.equals(info.getAppId())) {
            log.warn("Session Token 关联的 App Token 已失效或不匹配");
            return null;
        }

        return info;
    }
}
