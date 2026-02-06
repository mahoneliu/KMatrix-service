package org.dromara.ai.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识库统计信息VO
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Data
public class KmStatisticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库总数
     */
    private Long totalKbs;

    /**
     * 数据集总数
     */
    private Long totalDatasets;

    /**
     * 文档总数
     */
    private Long totalDocuments;

    /**
     * 切片总数
     */
    private Long totalChunks;

    /**
     * 处理中文档数
     */
    private Long processingDocs;

    /**
     * 失败文档数
     */
    private Long errorDocs;

    /**
     * 问题总数
     */
    private Long questionCount;
}
