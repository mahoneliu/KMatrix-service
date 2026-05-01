package org.dromara.ai.execution.registry.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;
import org.dromara.ai.execution.registry.mapper.McpRegistrySourceMapper;
import org.dromara.ai.execution.registry.service.McpRegistryService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * MCP 注册源定时同步调度器
 * <p>
 * 基于 Spring {@link ThreadPoolTaskScheduler} 实现动态调度，支持运行时注册/取消定时任务。
 * 系统启动时自动加载所有启用的注册源并触发一次全量同步。
 *
 * @author Mahone
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrySyncScheduler {

    private final McpRegistrySourceMapper sourceMapper;
    private final McpRegistryService registryService;

    /** 存储每个注册源的定时任务 Future，key = sourceId */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /** 使用 ThreadPoolTaskScheduler 实现动态调度 */
    private final ThreadPoolTaskScheduler taskScheduler;


    /**
     * 系统启动时加载所有启用注册源并注册定时任务，同时触发一次全量同步
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        List<KmMcpRegistrySource> sources = sourceMapper.selectEnabledSources();
        log.info("[McpRegistry] 系统启动，加载 {} 个启用注册源", sources.size());
        for (KmMcpRegistrySource source : sources) {
            // 异步触发一次全量同步（不阻塞启动）
            taskScheduler.execute(() -> {
                try {
                    registryService.syncSource(source.getSourceId());
                } catch (Exception e) {
                    log.error("[McpRegistry] 启动同步失败: sourceId={}", source.getSourceId(), e);
                }
            });
            // 注册定时任务
            reschedule(source);
        }
    }

    /**
     * 动态注册/更新某注册源的定时任务
     *
     * @param source 注册源配置
     */
    public void reschedule(KmMcpRegistrySource source) {
        cancel(source.getSourceId());
        if (!"1".equals(source.getIsEnabled())) {
            return;
        }
        long intervalMs = (long) source.getSyncInterval() * 1000;
        ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
            () -> {
                try {
                    registryService.syncSource(source.getSourceId());
                } catch (Exception e) {
                    log.error("[McpRegistry] 定时同步失败: sourceId={}", source.getSourceId(), e);
                }
            },
            intervalMs
        );
        scheduledTasks.put(source.getSourceId(), future);
        log.info("[McpRegistry] 注册定时任务: sourceId={}, interval={}s", source.getSourceId(), source.getSyncInterval());
    }

    /**
     * 取消某注册源的定时任务
     *
     * @param sourceId 注册源 ID
     */
    public void cancel(Long sourceId) {
        ScheduledFuture<?> existing = scheduledTasks.remove(sourceId);
        if (existing != null) {
            existing.cancel(false);
            log.info("[McpRegistry] 取消定时任务: sourceId={}", sourceId);
        }
    }

    /**
     * 监听注册源变更事件，同步更新定时任务状态
     */
    @EventListener
    public void onSourceChange(org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent event) {
        if (event.getType() == org.dromara.ai.execution.registry.event.McpRegistrySourceChangeEvent.ChangeType.DELETED) {
            cancel(event.getSource().getSourceId());
        } else {
            reschedule(event.getSource());
        }
    }
}
