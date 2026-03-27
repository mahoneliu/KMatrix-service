package org.dromara.ai.storage.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传结果统一对象
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KmFileResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 文件路径 (通常为内部相对路径)
     */
    private String filePath;

    /**
     * 文件 Web URL (带前缀或 OSS URL)
     */
    private String url;

    /**
     * OSS 文件 ID
     */
    private Long ossId;

    /**
     * 存储类型 (1-OSS, 2-本地文件)
     */
    private Integer storeType;

    /**
     * 文件哈希 (SHA-256)
     */
    private String hashCode;
}
