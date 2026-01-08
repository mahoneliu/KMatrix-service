package org.dromara.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI模块配置属性
 *
 * @author Mahone
 * @date 2026-01-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class KmAiProperties {

    /**
     * 是否开启对话日志
     */
    private boolean logChat = false;

}
