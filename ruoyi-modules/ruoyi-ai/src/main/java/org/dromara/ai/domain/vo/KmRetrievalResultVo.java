package org.dromara.ai.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 检索结果VO
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
public class KmRetrievalResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 切片ID
     */
    private Long chunkId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 相似度分数 (0-1)
     */
    private Double score;

    /**
     * 重排序后的分数
     */
    private Double rerankScore;

    /**
     * 切片元数据
     */
    private Object metadata;
}
