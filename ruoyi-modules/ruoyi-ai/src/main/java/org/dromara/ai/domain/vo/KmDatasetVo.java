package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDataset;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 数据集VO
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDataset.class)
public class KmDatasetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 类型 (FILE/WEB/MANUAL)
     */
    private String type;

    /**
     * ETL配置 (JSON)
     */
    private Map<String, Object> config;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 文档数量
     */
    private Integer documentCount;

    /**
     * 处理类型
     */
    private String processType;

    /**
     * 是否系统预设数据集
     */
    private Boolean isSystem;

    /**
     * 数据来源类型
     */
    private String sourceType;

    /**
     * 最小分块大小
     */
    private Integer minChunkSize;

    /**
     * 最大分块大小
     */
    private Integer maxChunkSize;

    /**
     * 分块重叠大小
     */
    private Integer chunkOverlap;

    /**
     * 支持的文件格式 (逗号分隔)
     */
    private String allowedFileTypes;
}
