package org.dromara.ai.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 本地文件信息 VO
 *
 * @author AI Assistant
 * @date 2026-02-06
 */
@Data
public class LocalFileVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件ID (对应 km_document.id)
     */
    private Long fileId;

    /**
     * 文件名 (存储的文件名,带UUID前缀)
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件相对路径 (相对于配置的 localPath)
     */
    private String filePath;

    /**
     * 文件绝对路径
     */
    private String absolutePath;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 访问 URL (用于前端显示,实际为相对路径)
     */
    private String url;
}
