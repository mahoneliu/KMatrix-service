package org.dromara.ai.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 工作流调度器配置
 * 独立启用 Spring @Scheduled，不依赖 snail-job 开关
 *
 * @author Mahone
 * @date 2026-04-06
 */
@Configuration
@EnableScheduling
public class WorkflowSchedulerConfig {
}
