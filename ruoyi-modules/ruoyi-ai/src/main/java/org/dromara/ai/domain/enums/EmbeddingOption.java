package org.dromara.ai.domain.enums;

/**
 * 向量化选项枚举
 *
 * @author Mahone
 * @date 2026-02-05
 */
public enum EmbeddingOption {
    /**
     * 仅向量化未向量化的分块
     */
    UNEMBEDDED_ONLY,

    /**
     * 向量化所有分块(包括已向量化的)
     */
    ALL
}
