package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 应用模型配置
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
public class AppModelConfig implements Serializable {

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户提示词模板
     */
    private String promptTemplate;

    /**
     * 无引用时的提示词
     */
    private String noRefPrompt;

    /**
     * 温度 (0.0 - 1.0)
     */
    private BigDecimal temperature;

    /**
     * 核采样 (0.0 - 1.0)
     */
    private BigDecimal topP;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 是否启用推理
     */
    private Boolean enableReasoning;
}
