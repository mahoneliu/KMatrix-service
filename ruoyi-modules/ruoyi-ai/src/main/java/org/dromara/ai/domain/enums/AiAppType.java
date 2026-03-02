package org.dromara.ai.domain.enums;

import org.dromara.common.core.utils.MessageUtils;

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
    FIXED_TEMPLATE("1", MessageUtils.message("ai.enum.app_type.fixed_template")),
    /**
     * 自定义工作流
     */
    CUSTOM_WORKFLOW("2", MessageUtils.message("ai.enum.app_type.custom_workflow")),
    /**
     * 智能体
     */
    AGENT("3", "智能体");

    private final String code;
    private final String info;
}
