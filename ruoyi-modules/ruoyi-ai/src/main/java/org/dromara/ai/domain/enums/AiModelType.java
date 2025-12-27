package org.dromara.ai.domain.enums;

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
     * 语言模型
     */
    LLM("1", "语言模型"),
    /**
     * 向量模型
     */
    EMBEDDING("2", "向量模型"),
    /**
     * 多路召回
     */
    RERANK("3", "多路召回"),
    /**
     * 语音模型
     */
    AUDIO("4", "语音模型"),
    /**
     * 图像模型
     */
    IMAGE("5", "图像模型"),
    /**
     * 视频模型
     */
    VIDEO("6", "视频模型");

    private final String code;
    private final String info;
}
