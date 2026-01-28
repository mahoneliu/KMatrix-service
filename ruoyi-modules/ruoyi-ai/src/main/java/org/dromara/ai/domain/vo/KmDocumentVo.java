package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDocument;

import java.io.Serializable;
import java.util.Date;

/**
 * 文档VO
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDocument.class)
public class KmDocumentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
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
}
