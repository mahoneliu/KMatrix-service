package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDocument;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 文档VO
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDocument.class)
public class KmDocumentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 所属数据集ID
     */
    private Long datasetId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 处理状态 (PENDING/PROCESSING/COMPLETED/ERROR)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * Token数量
     */
    private Integer tokenCount;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 启用状态 (0=禁用, 1=启用)
     */
    private Integer enabled;

    /**
     * 向量化状态 (0=未生成, 1=生成中, 2=已生成, 3=生成失败)
     */
    private Integer embeddingStatus;

    /**
     * 问题生成状态 (0=未生成, 1=生成中, 2=已生成, 3=生成失败)
     */
    private Integer questionStatus;

    /**
     * 状态追踪元数据
     */
    private Map<String, Object> statusMeta;
}
