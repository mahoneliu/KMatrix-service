package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.domain.KmDocument;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档业务对象
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDocument.class, reverseConvertGenerate = false)
public class KmDocumentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 数据集ID（必填）
     */
    @NotNull(message = "{ai.val.dataset.id_required}")
    private Long datasetId;

    /**
     * 启用状态筛选 (0=禁用, 1=启用)
     */
    private Integer enabled;

    /**
     * 向量化状态筛选 (0=未生成, 1=生成中, 2=已生成, 3=失败)
     */
    private Integer embeddingStatus;

    /**
     * 问题生成状态筛选 (0=未生成, 1=生成中, 2=已生成, 3=失败)
     */
    private Integer questionStatus;

    /**
     * 关键词搜索（匹配文件名）
     */
    private String keyword;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * URL
     */
    private String url;
}
