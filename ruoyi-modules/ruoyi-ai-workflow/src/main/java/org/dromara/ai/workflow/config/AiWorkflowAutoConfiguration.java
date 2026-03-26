package org.dromara.ai.workflow.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI 工作流模块自动配置
 *
 * @author Mahone
 */
@AutoConfiguration
@ComponentScan({"org.dromara.ai.workflow"})
public class AiWorkflowAutoConfiguration {
}
