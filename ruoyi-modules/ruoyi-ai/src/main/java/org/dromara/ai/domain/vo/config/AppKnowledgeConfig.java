package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 应用知识库配置
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
public class AppKnowledgeConfig implements Serializable {

    /**
     * 检索Top N
     */
    private Integer topK;

    /**
     * 相似度阈值 (0.0 - 1.0)
     */
    private BigDecimal similarityThreshold;

    /**
     * 检索模式 (embedding, fulltext, hybrid)
     */
    private String searchMode;

    /**
     * 单段落最大字符数
     */
    private Integer maxParagraphChar;

    /**
     * 无引用时是否返回
     */
    private Boolean returnDirectlyWhenNoRef;

    /**
     * 是否显示引用来源
     */
    private Boolean showCitation;
}
