package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 文档对象 km_document
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_document")
public class KmDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
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
     * 文件路径 (OSS ID或URL)
     */
    private String filePath;

    /**
     * OSS 文件ID
     */
    private Long ossId;

    /**
     * 文件类型 (PDF, TXT, DOCX等)
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
     * 文件哈希 (用于去重)
     */
    private String hashCode;

    /**
     * 所属知识库ID (冗余字段，便于查询)
     */
    private Long kbId;

    /**
     * 文档标题 (用于向量化和检索显示)
     */
    private String title;

    /**
     * 在线文档内容 (富文本HTML,用于 ONLINE_DOC 类型)
     */
    private String content;

    /**
     * 网页链接 URL (用于 WEB_LINK 类型)
     */
    private String url;
}
