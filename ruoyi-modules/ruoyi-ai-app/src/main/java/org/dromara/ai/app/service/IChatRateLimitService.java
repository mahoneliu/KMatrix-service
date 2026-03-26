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
}
