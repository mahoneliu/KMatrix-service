package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.ai.service.IKmDocumentService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/document")
public class KmDocumentController extends BaseController {

    private final IKmDocumentService documentService;

    /**
     * 上传文档到数据集
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<KmDocumentVo> upload(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @RequestParam("file") MultipartFile file) {
        return R.ok(documentService.uploadDocument(datasetId, file));
    }

    /**
     * 批量上传文档到数据集
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/uploadBatch")
    public R<List<KmDocumentVo>> uploadBatch(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @RequestParam("files") MultipartFile[] files) {
        return R.ok(documentService.uploadDocuments(datasetId, files));
    }

    /**
     * 查询数据集下的文档列表
     */
    @GetMapping("/listByDataset/{datasetId}")
    public R<List<KmDocumentVo>> listByDataset(
            @NotNull(message = "数据集ID不能为空") @PathVariable Long datasetId) {
        return R.ok(documentService.listByDatasetId(datasetId));
    }

    /**
     * 获取文档详细信息
     */
    @GetMapping("/{id}")
    public R<KmDocumentVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(documentService.queryById(id));
    }

    /**
     * 删除文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(documentService.deleteById(id));
    }

    /**
     * 重新处理文档 (重新触发ETL)
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PostMapping("/reprocess/{id}")
    public R<Void> reprocess(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(documentService.reprocessDocument(id));
    }

    /**
     * 创建在线文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/createOnlineDoc")
    public R<KmDocumentVo> createOnlineDoc(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @NotNull(message = "标题不能为空") @RequestParam String title,
            @NotNull(message = "内容不能为空") @RequestParam String content) {
        return R.ok(documentService.createOnlineDocument(datasetId, title, content));
    }

    /**
     * 创建网页链接文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/createWebLink")
    public R<KmDocumentVo> createWebLink(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @NotNull(message = "URL不能为空") @RequestParam String url) {
        return R.ok(documentService.createWebLinkDocument(datasetId, url));
    }
}
