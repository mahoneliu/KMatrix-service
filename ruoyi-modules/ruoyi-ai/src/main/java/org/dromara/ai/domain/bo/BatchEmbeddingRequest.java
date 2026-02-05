package org.dromara.ai.domain.bo;

import lombok.Data;
import org.dromara.ai.domain.enums.EmbeddingOption;

import java.io.Serializable;
import java.util.List;

/**
 * 批量向量化请求对象
 *
 * @author Mahone
 * @date 2026-02-05
 */
@Data
public class BatchEmbeddingRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID列表
     */
    private List<Long> documentIds;

    /**
     * 向量化选项(默认仅向量化未向量化的分块)
     */
    private EmbeddingOption option = EmbeddingOption.UNEMBEDDED_ONLY;
}
