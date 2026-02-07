package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.BatchChunkPreviewBo;
import org.dromara.ai.domain.bo.BatchEmbeddingRequest;
import org.dromara.ai.domain.bo.BatchGenerateQuestionsRequest;
import org.dromara.ai.domain.bo.ChunkPreviewBo;
import org.dromara.ai.domain.bo.ChunkSubmitBo;
import org.dromara.ai.domain.bo.KmDocumentBo;
import org.dromara.ai.domain.enums.EmbeddingOption;
import org.dromara.ai.domain.vo.ChunkPreviewVo;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.ai.domain.vo.TempFileVo;
import org.dromara.ai.service.IKmDocumentService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
     * 查询知识库下的所有文档列表
     */
    @GetMapping("/listByKb/{kbId}")
    public R<List<KmDocumentVo>> listByKb(
            @NotNull(message = "知识库ID不能为空") @PathVariable Long kbId) {
        return R.ok(documentService.listByKbId(kbId));
    }

    /**
     * 分页查询文档列表
     */
    @GetMapping("/list")
    public TableDataInfo<KmDocumentVo> list(KmDocumentBo bo, PageQuery pageQuery) {
        return documentService.pageList(bo, pageQuery);
    }

    /**
     * 获取文档详细信息
     */
    @GetMapping("/{id:\\d+}")
    public R<KmDocumentVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(documentService.queryById(id));
    }

    /**
     * 删除文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id:\\d+}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(documentService.deleteById(id));
    }

    /**
     * 重新处理文档 (重新触发ETL)
     */
    // @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    // @PostMapping("/reprocess/{id:\\d+}")
    // public R<Void> reprocess(@NotNull(message = "主键不能为空") @PathVariable Long id)
    // {
    // return toAjax(documentService.reprocessEmbeddingDocument(id));
    // }

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

    /**
     * 启用文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping("/enable/{id:\\d+}")
    public R<Void> enable(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(documentService.enableDocument(id, true));
    }

    /**
     * 禁用文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping("/disable/{id:\\d+}")
    public R<Void> disable(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(documentService.enableDocument(id, false));
    }

    /**
     * 批量启用文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping("/batchEnable")
    public R<Void> batchEnable(@RequestBody List<Long> ids) {
        return toAjax(documentService.batchEnable(ids, true));
    }

    /**
     * 批量禁用文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping("/batchDisable")
    public R<Void> batchDisable(@RequestBody List<Long> ids) {
        return toAjax(documentService.batchEnable(ids, false));
    }

    /**
     * 批量删除文档
     */
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/batchDelete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        return toAjax(documentService.batchDelete(ids));
    }

    /**
     * 更新文档名称
     */
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping("/{id:\\d+}")
    public R<Void> updateDocumentName(
            @NotNull(message = "主键不能为空") @PathVariable Long id,
            @NotNull(message = "名称不能为空") @RequestParam String originalFilename) {
        return toAjax(documentService.updateDocumentName(id, originalFilename));
    }

    /**
     * 批量向量化生成
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/batchEmbedding")
    public R<Void> batchEmbedding(@RequestBody BatchEmbeddingRequest request) {
        return toAjax(documentService.batchEmbedding(
                request.getDocumentIds(),
                request.getOption() != null ? request.getOption() : EmbeddingOption.UNEMBEDDED_ONLY));
    }

    /**
     * 单个文档向量化
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/embedding/{id:\\d+}")
    public R<Void> embedding(
            @NotNull(message = "主键不能为空") @PathVariable Long id,
            @RequestParam(defaultValue = "UNEMBEDDED_ONLY") EmbeddingOption option) {
        return toAjax(documentService.embeddingDocument(id, option));
    }

    /**
     * 批量问题生成
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/batchGenerateQuestions")
    public R<Void> batchGenerateQuestions(@RequestBody BatchGenerateQuestionsRequest request) {
        return toAjax(documentService.batchGenerateQuestions(request.getDocumentIds(), request.getModelId(),
                request.getPrompt(), request.getTemperature(), request.getMaxTokens()));
    }

    /**
     * 上传临时文件 (分块预览流程第一步)
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/uploadTemp")
    public R<TempFileVo> uploadTemp(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @RequestParam("file") MultipartFile file) {
        return R.ok(documentService.uploadTempFile(datasetId, file));
    }

    /**
     * 批量上传临时文件 (分块预览流程第一步 - 批量版本)
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/uploadTempBatch")
    public R<List<TempFileVo>> uploadTempBatch(
            @NotNull(message = "数据集ID不能为空") @RequestParam Long datasetId,
            @RequestParam("files") MultipartFile[] files) {
        return R.ok(documentService.uploadTempFiles(datasetId, files));
    }

    /**
     * 预览分块 (分块预览流程第二步)
     */
    @PostMapping("/previewChunks")
    public R<List<ChunkPreviewVo>> previewChunks(@RequestBody ChunkPreviewBo bo) {
        return R.ok(documentService.previewChunks(bo));
    }

    /**
     * 批量预览分块 (分块预览流程第二步 - 批量版本)
     */
    @PostMapping("/batchPreviewChunks")
    public R<Map<Long, List<ChunkPreviewVo>>> batchPreviewChunks(@RequestBody BatchChunkPreviewBo bo) {
        return R.ok(documentService.batchPreviewChunks(bo));
    }

    /**
     * 提交分块并入库 (分块预览流程第三步)
     */
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/submitChunks")
    public R<KmDocumentVo> submitChunks(@RequestBody ChunkSubmitBo bo) {
        return R.ok(documentService.submitChunks(bo));
    }
}
