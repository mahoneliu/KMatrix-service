package org.dromara.ai.api.enums;

import org.dromara.common.core.utils.MessageUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI模型类型
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum AiModelType {
    /**
     * 多模态模型
     */
    MULTI_MODAL("0", MessageUtils.message("ai.enum.model_type.multi_modal")),
    /**
     * 语言模型
     */
    LLM("1", MessageUtils.message("ai.enum.model_type.llm")),
    /**
     * 向量模型
     */
    EMBEDDING("2", MessageUtils.message("ai.enum.model_type.embedding")),
    /**
     * 多路召回
     */
    RERANK("3", MessageUtils.message("ai.enum.model_type.rerank")),
    /**
     * 语音模型
     */
    AUDIO("4", MessageUtils.message("ai.enum.model_type.audio")),
    /**
     * 图像模型
     */
    IMAGE("5", MessageUtils.message("ai.enum.model_type.image")),
    /**
     * 视频模型
     */
    VIDEO("6", MessageUtils.message("ai.enum.model_type.video"));

    private final String code;
    private final String info;
}
