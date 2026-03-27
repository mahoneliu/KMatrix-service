package org.dromara.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.RequestState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 请求状态管理器
 * 用于管理所有活跃的流式请求状态，并定期清理过期的请求状态
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Slf4j
@Component
public class RequestStateManager {

    /**
     * 请求状态存储
     */
    private final ConcurrentHashMap<String, RequestState> requestStates = new ConcurrentHashMap<>();

    /**
     * 请求状态 TTL（生存时间），单位：分钟
     */
    private static final long REQUEST_STATE_TTL_MINUTES = 5;

    /**
     * 清理任务执行间隔，单位：分钟
     */
    private static final long CLEANUP_INTERVAL_MINUTES = 1;

    /**
     * 定时清理任务执行器
     */
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * 构造函数，初始化定时清理任务
     */
    public RequestStateManager() {
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "RequestStateCleanupThread");
            t.setDaemon(true);
            return t;
        });

        // 启动定时清理任务
        startCleanupTask();
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredStates,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    /**
     * 清理过期的请求状态
     */
    private void cleanupExpiredStates() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ttlThreshold = now.minusMinutes(REQUEST_STATE_TTL_MINUTES);

            requestStates.entrySet().removeIf(entry -> {
                RequestState state = entry.getValue();
                if (state.getCreatedAt().isBefore(ttlThreshold)) {
                    log.debug("Cleaning up expired request state: {}", entry.getKey());
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            log.error("Error during cleanup of expired request states", e);
        }
    }

    /**
     * 添加请求状态
     *
     * @param requestId 请求ID
     * @param state 请求状态
     */
    public void put(String requestId, RequestState state) {
        requestStates.put(requestId, state);
        log.debug("Added request state: {}", requestId);
    }

    /**
     * 获取请求状态
     *
     * @param requestId 请求ID
     * @return 请求状态，如果不存在则返回 null
     */
    public RequestState get(String requestId) {
        return requestStates.get(requestId);
    }

    /**
     * 移除请求状态
     *
     * @param requestId 请求ID
     * @return 被移除的请求状态，如果不存在则返回 null
     */
    public RequestState remove(String requestId) {
        RequestState state = requestStates.remove(requestId);
        if (state != null) {
            log.debug("Removed request state: {}", requestId);
        }
        return state;
    }

    /**
     * 获取所有请求状态
     *
     * @return 所有请求状态的集合
     */
    public Collection<RequestState> getAll() {
        return requestStates.values();
    }

    /**
     * 检查请求状态是否存在
     *
     * @param requestId 请求ID
     * @return 是否存在
     */
    public boolean exists(String requestId) {
        return requestStates.containsKey(requestId);
    }

    /**
     * 获取当前管理的请求状态数量
     *
     * @return 请求状态数量
     */
    public int size() {
        return requestStates.size();
    }

    /**
     * 清空所有请求状态
     */
    public void clear() {
        requestStates.clear();
        log.debug("Cleared all request states");
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
    }
}
