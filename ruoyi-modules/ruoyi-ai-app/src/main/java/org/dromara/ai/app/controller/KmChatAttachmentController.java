package org.dromara.ai.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天附件上传 Controller
 * 负责在聊天发送前将附件存为临时文件，返回临时文件 ID
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Tag(name = "聊天附件管理")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/admin/chat/attachment")
public class KmChatAttachmentController {

    private final IKmFileService kmFileService;

    /**
     * 上传聊天附件（管理端）
     *
     * @param file 附件文件（图片、音频等）
     * @return 临时文件 ID 和访问 URL
     */
    @Operation(summary = "上传聊天附件", description = "将附件存为临时文件（24小时有效），返回 tempFileId 供聊天接口使用")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(
            @Parameter(description = "附件文件") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        KmTempFile tempFile = kmFileService.saveTempFile(null, file);
        Map<String, Object> result = new HashMap<>();
        result.put("ossId", tempFile.getId());
        result.put("tempFileId", tempFile.getId()); // preserve for any references
        result.put("originalFilename", tempFile.getOriginalFilename());
        result.put("fileExtension", tempFile.getFileExtension());
        result.put("fileSize", tempFile.getFileSize());
        result.put("url", tempFile.getUrl());
        result.put("fileUrl", tempFile.getUrl()); // preserve for any references
        return R.ok(result);
    }
}
