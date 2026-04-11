package org.dromara.ai.app.service;

/**
 * 聊天频率限制服务接口
 * 负责：1. 获取有效限流配置 2. 校验频次与Token  3. 记录本次消耗
 *
 * @author Mahone
 * @date 2026-03-23
 */
public interface IChatRateLimitService {

    /**
     * 检查该用户本次请求是否超出频率限制（仅检查请求次数）
     * 如果超限则抛出 ServiceException
     *
     * @param userId 用户标识（可为任意唯一标识，session token user 或 userId）
     */
    void checkRequestLimit(String userId);

    /**
     * 在请求完成后，记录本次对话消耗的 Token 并检查 Token 是否已超限
     * Token 超限仅在下次请求时触发拦截（异步/事后结算）
     *
     * @param userId         用户标识
     * @param tokensConsumed 本次消耗的 token 数量
     */
    void recordTokenUsage(String userId, long tokensConsumed);

    /**
     * 检查该用户 Token 是否已超出限制（同步检查，用于请求前判断）
     * 如果超限则抛出 ServiceException
     *
     * @param userId 用户标识
     */
    void checkTokenLimit(String userId);

    /**
     * 为「App Token 直连」匿名访客构建专属限流标识。
     * 格式：app-anon:{appId}:{clientIp}
     * <p>
     * 当客户端跳过 Session Token 换取流程，直接以 App Token 发起对话时，
     * 应使用此 Key 进行限流，避免将消耗归因到应用创建者账号上。
     *
     * @param appId    应用 ID
     * @param clientIp 客户端 IP（已从请求头/连接地址中解析好）
     * @return 限流 Key
     */
    String buildAnonRateLimitKey(Long appId, String clientIp);
}
