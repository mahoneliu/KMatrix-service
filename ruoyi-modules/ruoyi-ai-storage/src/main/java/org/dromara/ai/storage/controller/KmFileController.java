package org.dromara.ai.storage.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.io.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.storage.service.ILocalFileService;
import org.dromara.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * AI 统一文件存储 Controller
 * <p>
 * 提供文件上传入口（需鉴权）和本地文件预览/下载入口（公开，免登录）。
 * 文件存储位置由 ai.file-store.type 配置决定（1-OSS / 2-本地）。
 *
 * @author Mahone
 * @date 2026-04-11
 */
@Tag(name = "AI 统一文件存储")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/storage/file")
public class KmFileController {

    private final IKmFileService kmFileService;
    private final ILocalFileService localFileService;

    /**
     * 上传文件（需登录）
     * 根据当前存储配置自动路由到 OSS 或本地磁盘。
     *
     * @param file 要上传的文件
     * @return 文件上传结果，包含 url、filePath、storeType 等字段
     */
    @Operation(summary = "上传文件", description = "上传文件至 AI 存储模块，根据配置自动路由到 OSS 或本地存储，返回访问 URL")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(
            @Parameter(description = "要上传的文件") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        KmFileResult result = kmFileService.upload(file);
        return R.ok(Map.of(
                "url", result.getUrl() != null ? result.getUrl() : "",
                "filePath", result.getFilePath() != null ? result.getFilePath() : "",
                "originalFilename", result.getOriginalFilename() != null ? result.getOriginalFilename() : "",
                "fileExtension", result.getFileExtension() != null ? result.getFileExtension() : "",
                "fileSize", result.getFileSize() != null ? result.getFileSize() : 0L,
                "storeType", result.getStoreType() != null ? result.getStoreType() : 0,
                "ossId", result.getOssId() != null ? result.getOssId() : ""
        ));
    }

    /**
     * 预览/访问本地存储文件（免登录）
     * <p>
     * 当 storeType=2（本地存储）时，ai.file-store.localPrefix 配置的 URL 前缀会指向此路由。
     * 此接口根据路径提取文件相对路径并输出文件流；OSS 场景下使用方无需调用此接口。
     *
     * @param request  HTTP 请求（用于提取文件相对路径）
     * @param response HTTP 响应（用于输出文件流）
     */
    @SaIgnore
    @Operation(summary = "访问本地存储文件（公开）", description = "通过相对路径访问本地存储文件，OSS 场景无需调用")
    @GetMapping("/**")
    public void serveLocalFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 提取 /ai/storage/file/ 后面的相对路径
        String prefix = "/ai/storage/file/";
        String uri = request.getRequestURI();
        String decodedUri = java.net.URLDecoder.decode(uri, java.nio.charset.StandardCharsets.UTF_8);
        int idx = decodedUri.indexOf(prefix);
        if (idx < 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法路径");
            return;
        }
        String relativePath = decodedUri.substring(idx + prefix.length());
        if (relativePath.isBlank() || relativePath.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法路径");
            return;
        }

        // 根据扩展名推断 Content-Type
        String ext = FileUtil.extName(relativePath).toLowerCase();
        String contentType = switch (ext) {
            case "png"  -> MediaType.IMAGE_PNG_VALUE;
            case "gif"  -> MediaType.IMAGE_GIF_VALUE;
            case "webp" -> "image/webp";
            case "svg"  -> "image/svg+xml";
            case "pdf"  -> MediaType.APPLICATION_PDF_VALUE;
            default     -> MediaType.IMAGE_JPEG_VALUE;
        };
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream is = localFileService.getFileStream(relativePath);
             OutputStream os = response.getOutputStream()) {
            if (is == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
                return;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }
}
