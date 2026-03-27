package org.dromara.ai.storage.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 临时文件对象 km_temp_file
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
@TableName("km_temp_file")
public class KmTempFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 临时文件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据集ID (可选，非数据集关联附件场景为空)
     */
    private Long datasetId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件路径 (OSS URL 或本地路径)
     */
    private String filePath;

    /**
     * 文件 Web URL (不持久化到数据库)
     */
    @TableField(exist = false)
    private String url;

    /**
     * OSS 文件 ID
     */
    private Long ossId;

    /**
     * 存储类型 (1-OSS, 2-本地)
     */
    private Integer storeType;

    /**
     * 文件哈希
     */
    private String hashCode;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 过期时间 (默认24小时后过期)
     */
    private Date expireTime;
}
