package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.ai.domain.KmDocumentChunk;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档分块新增/编辑请求体
 *
 * @author Mahone
 */
@Data
@AutoMapper(target = KmDocumentChunk.class, reverseConvertGenerate = false)
public class KmDocumentChunkBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分块ID（编辑时必填）
     */
    private Long id;

    /**
     * 文档ID（新增时必填）
     */
    private Long documentId;

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 分块标题
     */
    private String title;

    /**
     * 分块内容
     */
    @NotBlank(message = "{ai.val.chunk.content_required}")
    private String content;

    /**
     * 启用状态筛选 (0=禁用, 1=启用)
     */
    private Integer enabled;

    /**
     * 向量化状态筛选
     */
    private Integer embeddingStatus;

    /**
     * 问题生成状态筛选
     */
    private Integer questionStatus;

    /**
     * 关键词搜索
     */
    private String keyword;
}
