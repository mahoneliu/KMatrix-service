package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
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
     * 数据集ID
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
     * 临时存储路径
     */
    private String tempPath;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 过期时间 (默认24小时后过期)
     */
    private Date expireTime;
}
