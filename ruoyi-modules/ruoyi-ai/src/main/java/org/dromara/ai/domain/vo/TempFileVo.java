package org.dromara.ai.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 临时文件信息VO
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
public class TempFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 临时文件ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

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
     * 临时文件路径
     */
    private String tempPath;

    /**
     * 数据集ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long datasetId;
}
