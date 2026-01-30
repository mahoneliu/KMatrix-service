package org.dromara.ai.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 检索请求Bo
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
public class KmRetrievalBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 查询文本
     */
    private String query;

    /**
     * 知识库ID列表 (可选，不传则搜索全部)
     */
    private List<Long> kbIds;

    /**
     * 数据集ID列表 (可选)
     */
    private List<Long> datasetIds;

    /**
     * 返回结果数量 (默认 5)
     */
    private Integer topK = 5;

    /**
     * 相似度阈值 (0-1, 默认 0.5)
     */
    private Double threshold = 0.5;

    /**
     * 检索模式: VECTOR / KEYWORD / HYBRID
     */
    private String mode = "VECTOR";

    /**
     * 是否启用 Rerank
     */
    private Boolean enableRerank = false;

    /**
     * 是否启用关键词高亮 (默认 false，仅全文检索场景使用)
     */
    private Boolean enableHighlight = false;
}
