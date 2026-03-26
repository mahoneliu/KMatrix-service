package org.dromara.ai.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.vo.ChatRateLimitConfigVo;
import org.dromara.ai.app.service.IChatRateLimitService;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 聊天频率限制服务实现
 * 使用 Redis 原子计数实现分钟/小时/天三维度的请求次数和 Token 消耗限流。
 * 配置优先级：用户级别 > 系统默认（sys_config: chat.rate.limit.default）。
 *
 * @author Mahone
 * @date 2026-03-23
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatRateLimitServiceImpl implements IChatRateLimitService {

    /** sys_config 中系统默认限流配置的 Key */
    private static final String SYS_CONFIG_RATE_LIMIT_KEY = "chat.rate.limit.default";

    /** Redis Key 前缀 */
    private static final String REDIS_KEY_PREFIX = "chat:rate:";
    private static final String REDIS_KEY_SEP = ":";

    /** 时间窗口标识 */
    private static final String WINDOW_MINUTE = "minute";
    private static final String WINDOW_HOUR = "hour";
    private static final String WINDOW_DAY = "day";

    /** Redis Key 计数类型后缀 */
    private static final String SUFFIX_REQUESTS = ":req";
    private static final String SUFFIX_TOKENS = ":tok";

    private final ObjectMapper objectMapper;

    // 懒加载，避免循环依赖
    private ISysConfigService getConfigService() {
        return SpringUtils.getBean(ISysConfigService.class);
    }

    private ISysUserService getUserService() {
        return SpringUtils.getBean(ISysUserService.class);
    }

    /**
     * 检查请求次数限制（预检，超限直接抛异常）
     *
     * @param userId 用户标识
     */
    @Override
    public void checkRequestLimit(String userId) {
        ChatRateLimitConfigVo config = resolveConfig(userId);
        if (config == null) {
            return;
        }

        checkWindow(userId, WINDOW_MINUTE, getMinuteTtl(), config.getMinute(), true);
        checkWindow(userId, WINDOW_HOUR, getHourTtl(), config.getHour(), true);
        checkWindow(userId, WINDOW_DAY, getDayTtl(), config.getDay(), true);
    }

    /**
     * 检查 Token 限制（预检，超限直接抛异常）
     *
     * @param userId 用户标识
     */
    @Override
    public void checkTokenLimit(String userId) {
        ChatRateLimitConfigVo config = resolveConfig(userId);
        if (config == null) {
            return;
        }

        checkWindowTokens(userId, WINDOW_MINUTE, config.getMinute());
        checkWindowTokens(userId, WINDOW_HOUR, config.getHour());
        checkWindowTokens(userId, WINDOW_DAY, config.getDay());
    }

    /**
     * 本次请求完成后记录 Token 消耗，并原子累加至 Redis
     *
     * @param userId         用户标识
     * @param tokensConsumed 本次消耗的 token 数
     */
    @Override
    public void recordTokenUsage(String userId, long tokensConsumed) {
        if (tokensConsumed <= 0 || StrUtil.isBlank(userId)) {
            return;
        }

        try {
            incrWithExpireIfAbsent(buildKey(userId, WINDOW_MINUTE, SUFFIX_TOKENS), tokensConsumed, getMinuteTtl());
            incrWithExpireIfAbsent(buildKey(userId, WINDOW_HOUR, SUFFIX_TOKENS), tokensConsumed, getHourTtl());
            incrWithExpireIfAbsent(buildKey(userId, WINDOW_DAY, SUFFIX_TOKENS), tokensConsumed, getDayTtl());
        } catch (Exception e) {
            log.warn("记录 Token 使用量失败，userId={}, tokens={}, err={}", userId, tokensConsumed, e.getMessage());
        }
    }

    // =================== Private Helpers ===================

    /**
     * 获取当前用户的有效限流配置（用户级 > 系统默认）
     */
    private ChatRateLimitConfigVo resolveConfig(String userId) {
        // 1. 先尝试从用户数据库获取用户级别配置
        try {
            if (StrUtil.isNotBlank(userId) && isNumeric(userId)) {
                var userVo = getUserService().selectUserById(Long.parseLong(userId));
                if (userVo != null && StrUtil.isNotBlank(userVo.getRateLimitConfig())) {
                    ChatRateLimitConfigVo config = objectMapper.readValue(userVo.getRateLimitConfig(),
                            ChatRateLimitConfigVo.class);
                    if (config != null) {
                        return config;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取用户限流配置失败，降级为系统默认值，userId={}, err={}", userId, e.getMessage());
        }

        // 2. 降级到系统配置
        try {
            String sysConfigJson = getConfigService().selectConfigByKey(SYS_CONFIG_RATE_LIMIT_KEY);
            if (StrUtil.isNotBlank(sysConfigJson)) {
                return objectMapper.readValue(sysConfigJson, ChatRateLimitConfigVo.class);
            }
        } catch (Exception e) {
            log.warn("读取系统默认限流配置失败，跳过限流，err={}", e.getMessage());
        }

        return null;
    }

    /**
     * 校验单个时间窗口的请求次数，超限则抛出 ServiceException。
     * 同时自增请求计数器。
     */
    private void checkWindow(String userId, String window, Duration ttl,
                              ChatRateLimitConfigVo.LimitQuota quota, boolean incrOnCheck) {
        if (quota == null || quota.getRequests() == null) {
            return;
        }

        String key = buildKey(userId, window, SUFFIX_REQUESTS);
        long current = incrWithExpireIfAbsent(key, 1, ttl);

        if (current > quota.getRequests()) {
            String msg = MessageUtils.message("ai.msg.rate_limit.request_exceeded", toWindowLabel(window));
            throw new ServiceException(msg, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 校验单个时间窗口的 Token 是否超限（不自增，仅读取当前计数进行比较）
     */
    private void checkWindowTokens(String userId, String window, ChatRateLimitConfigVo.LimitQuota quota) {
        if (quota == null || quota.getTokens() == null) {
            return;
        }

        String key = buildKey(userId, window, SUFFIX_TOKENS);
        Object currentObj = RedisUtils.getCacheObject(key);
        long used = cn.hutool.core.convert.Convert.toLong(currentObj, 0L);

        if (used >= quota.getTokens()) {
            String msg = MessageUtils.message("ai.msg.rate_limit.token_exceeded", toWindowLabel(window));
            throw new ServiceException(msg, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /**
     * 对 Redis 中的 Key 做原子自增，如果 Key 不存在则设置过期时间
     *
     * @return 自增后的当前值
     */
    private long incrWithExpireIfAbsent(String key, long delta, Duration ttl) {
        // 利用 Redisson 的 RAtomicLong 实现原子操作
        var atomicLong = RedisUtils.getClient().getAtomicLong(key);
        long newVal = atomicLong.addAndGet(delta);

        // 如果是第一次写入（刚好等于 delta），说明 key 是新建的，设置 TTL
        if (newVal == delta) {
            atomicLong.expire(ttl);
        }
        return newVal;
    }

    private String buildKey(String userId, String window, String suffix) {
        return REDIS_KEY_PREFIX + userId + REDIS_KEY_SEP + window + suffix;
    }

    private Duration getMinuteTtl() {
        return Duration.ofMinutes(1);
    }

    private Duration getHourTtl() {
        return Duration.ofHours(1);
    }

    private Duration getDayTtl() {
        return Duration.ofDays(1);
    }

    private String toWindowLabel(String window) {
        return switch (window) {
            case WINDOW_MINUTE -> MessageUtils.message("ai.msg.rate_limit.window.minute");
            case WINDOW_HOUR -> MessageUtils.message("ai.msg.rate_limit.window.hour");
            case WINDOW_DAY -> MessageUtils.message("ai.msg.rate_limit.window.day");
            default -> window;
        };
    }

    private boolean isNumeric(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
