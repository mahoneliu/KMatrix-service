package org.dromara.ai.knowledge.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.knowledge.domain.KmDataset;

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
     * 支持的文件格式 (逗号分隔)
     */
    private String allowedFileTypes;

    /**
     * 关联的工作流应用ID（从config中读取，兼容旧key appId）
     */
    public Long getWorkflowId() {
        if (config == null) return null;
        Object val = config.containsKey("workflowId") ? config.get("workflowId") : config.get("appId");
        if (val == null) return null;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    /**
     * 最大并行处理文档数（从config中读取）
     */
    public Integer getMaxConcurrency() {
        if (config == null) return 1;
        Object val = config.get("maxConcurrency");
        if (val == null) return 1;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 1; }
    }

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
