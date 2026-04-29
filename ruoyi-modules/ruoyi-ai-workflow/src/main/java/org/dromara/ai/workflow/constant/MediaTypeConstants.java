package org.dromara.ai.workflow.constant;

/**
 * 工作流媒体类型常量
 * <p>
 * 统一管理文件类型标识字符串，用于多模态文件处理、StartNode 媒体类型判断等场景。
 *
 * @author Mahone
 */
public interface MediaTypeConstants {

    // =========================================================================
    // 文件类型标识（KmWorkflowFile.type 字段值）
    // =========================================================================

    /** 图片类型 */
    String TYPE_IMAGE = "image";

    /** 音频类型 */
    String TYPE_AUDIO = "audio";

    /** 视频类型 */
    String TYPE_VIDEO = "video";

    /** PDF 类型 */
    String TYPE_PDF = "pdf";

    /** 通用文件类型 */
    String TYPE_FILE = "file";

    /** 文本类型（多模态 JSON 数组中的纯文本片段） */
    String TYPE_TEXT = "text";

    // =========================================================================
    // MIME 类型（传给大模型时使用）
    // =========================================================================

    /** 图片默认 MIME 类型 */
    String MIME_IMAGE_JPEG = "image/jpeg";

    /** 音频默认 MIME 类型 */
    String MIME_AUDIO_MPEG = "audio/mpeg";

    /** PDF MIME 类型 */
    String MIME_APPLICATION_PDF = "application/pdf";

    /** 视频默认 MIME 类型 */
    String MIME_VIDEO_MP4 = "video/mp4";

    /** PNG 图片 MIME 类型 */
    String MIME_IMAGE_PNG = "image/png";

    // =========================================================================
    // 图片扩展名列表（用于 StartNode 媒体类型判断）
    // =========================================================================

    /** 图片扩展名 */
    java.util.List<String> IMAGE_EXTENSIONS = java.util.List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 音频扩展名 */
    java.util.List<String> AUDIO_EXTENSIONS = java.util.List.of("mp3", "wav", "flac", "aac", "ogg", "m4a");

    /** 视频扩展名 */
    java.util.List<String> VIDEO_EXTENSIONS = java.util.List.of("mp4", "mov", "avi", "mkv", "webm");

    /** 视频文件扩展名正则（用于文件名匹配） */
    String VIDEO_EXTENSION_REGEX = ".*\\.(mp4|avi|mov|wmv|flv|mkv)$";
}
