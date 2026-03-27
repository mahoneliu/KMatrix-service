package org.dromara.ai.storage.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工作流内部流转的标准文件对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KmWorkflowFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 文件类型: image, audio, video, document, etc.
     */
    private String type;

    /**
     * 文件原始名称
     */
    private String name;

    /**
     * OSS 访问 ID
     */
    private Long ossId;

    /**
     * 直接访问 URL
     */
    private String url;

    /**
     * 临时文件 ID (如果是刚上传的)
     */
    private Long tempFileId;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件后缀
     */
    private String extension;
}
