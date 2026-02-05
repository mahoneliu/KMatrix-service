package org.dromara.ai.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量问题生成请求
 *
 * @author Mahone
 * @date 2026-02-03
 */
@Data
public class BatchGenerateQuestionsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID列表
     */
    private List<Long> documentIds;

    /**
     * 模型ID（可选）
     */
    private Long modelId;

    /**
     * 提示词模板 (可选)
     */
    private String prompt;

    /**
     * 温度参数 (可选)
     */
    private Double temperature;

    /**
     * 最大Token数 (可选)
     */
    private Integer maxTokens;
}
