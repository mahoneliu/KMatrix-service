package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI应用类型
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum AiAppType {
    /**
     * 固定模板
     */
    FIXED_TEMPLATE("1", "固定模板"),
    /**
     * 自定义工作流
     */
    CUSTOM_WORKFLOW("2", "自定义工作流"),
    /**
     * 智能体
     */
    AGENT("3", "智能体");

    private final String code;
    private final String info;
}
