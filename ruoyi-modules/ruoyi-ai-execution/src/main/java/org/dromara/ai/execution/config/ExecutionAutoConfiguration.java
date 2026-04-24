package org.dromara.ai.execution.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI 执行层模块自动配置
 *
 * @author KMatrix
 */
@AutoConfiguration
@ComponentScan({"org.dromara.ai.execution"})
public class ExecutionAutoConfiguration {
}
