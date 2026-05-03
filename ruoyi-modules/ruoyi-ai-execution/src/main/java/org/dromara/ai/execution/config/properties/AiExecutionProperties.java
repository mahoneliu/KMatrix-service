package org.dromara.ai.execution.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 执行层配置属性
 *
 * @author KMatrix
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.execution")
public class AiExecutionProperties {

    /**
     * Python 可执行文件路径 (可选)
     * 如果未指定，系统将尝试自动探测。
     */
    private String pythonPath;

}
