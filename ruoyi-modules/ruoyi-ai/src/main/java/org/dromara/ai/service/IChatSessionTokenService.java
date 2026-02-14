package org.dromara.ai.service;

import org.dromara.ai.domain.vo.ChatSessionTokenInfo;

/**
 * 对话 Session Token 服务接口
 * 用于匿名用户的 JWT Token 生成和解析
 *
 * @author Mahone
 * @date 2026-01-27
 */
public interface IChatSessionTokenService {

    /**
     * 生成匿名用户 Session Token
     *
     * @param appId    应用 ID
     * @param appToken 应用 Token
     * @param userId   用户 ID (雪花算法生成)
     * @return JWT Token 字符串
     */
    String generateToken(Long appId, String appToken, Long userId);

    /**
     * 解析 Session Token
     *
     * @param sessionToken JWT Token 字符串
     * @return Token 信息，解析失败返回 null
     */
    ChatSessionTokenInfo parseToken(String sessionToken);

    /**
     * 验证 Session Token 有效性
     * 会同时验证 appToken 是否仍然有效
     *
     * @param sessionToken JWT Token 字符串
     * @return 验证通过返回 Token 信息，失败返回 null
     */
    ChatSessionTokenInfo validateToken(String sessionToken);
}
