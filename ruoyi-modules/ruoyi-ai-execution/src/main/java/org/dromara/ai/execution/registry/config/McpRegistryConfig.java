package org.dromara.ai.execution.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * MCP 注册源相关配置
 *
 * @author Mahone
 */
@Configuration
public class McpRegistryConfig {

    /**
     * 注册 ThreadPoolTaskScheduler Bean，用于 MCP 注册源定时同步
     */
    @Bean
    public ThreadPoolTaskScheduler registryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("mcp-registry-sync-");
        scheduler.initialize();
        return scheduler;
    }
}
