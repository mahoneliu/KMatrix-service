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
     * 对话
     */
    CHAT("1", "对话"),
    /**
     * 工作流
     */
    WORKFLOW("2", "工作流"),
    /**
     * 智能体
     */
    AGENT("3", "智能体");

    private final String code;
    private final String info;
}
