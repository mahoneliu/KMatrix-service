package org.dromara.ai.knowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.knowledge.domain.KmDataset;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据集Bo
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDataset.class, reverseConvertGenerate = false)
public class KmDatasetBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    private Long id;

    /**
     * 所属知识库ID
     */
    @NotNull(message = "{ai.val.kb.id_required}")
    private Long kbId;

    /**
     * 数据集名称
     */
    @NotBlank(message = "{ai.val.dataset.name_required}")
    private String name;

    /**
     * ETL配置 (JSON)
     */
    private Map<String, Object> config;

    /**
     * 处理类型: GENERIC_FILE, QA_PAIR, ONLINE_DOC, WEB_LINK
     */
    private String processType;

    /**
     * 是否系统预设数据集
     */
    private Boolean isSystem;

    /**
     * 数据来源类型: FILE_UPLOAD, TEXT_INPUT, WEB_CRAWL
     */
    private String sourceType;

    /**
     * 支持的文件格式 (逗号分隔)
     */
    private String allowedFileTypes;

    /**
     * 关联的工作流应用ID（仅工作流处理类型使用，存入config）
     */
    private Long workflowId;

    /**
     * 最大并行处理文档数（仅工作流处理类型使用，存入config）
     */
    private Integer maxConcurrency;

    /**
     * 最小分块大小 (字符数)
     */
    private Integer minChunkSize;

    /**
     * 最大分块大小 (字符数)
     */
    private Integer maxChunkSize;

    /**
     * 分块重叠大小 (字符数)
     */
    private Integer chunkOverlap;

    /**
     * 子块大小 (字符数)
     */
    private Integer childChunkSize;

    /**
     * 子块重叠大小 (字符数)
     */
    private Integer childChunkOverlap;
}
