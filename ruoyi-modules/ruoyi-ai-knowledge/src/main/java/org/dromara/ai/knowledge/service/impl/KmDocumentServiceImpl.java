package org.dromara.ai.knowledge.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.domain.bo.*;
import org.dromara.ai.api.enums.EmbeddingOption;
import org.dromara.ai.knowledge.domain.vo.ChunkPreviewVo;
import org.dromara.ai.knowledge.domain.vo.KmDocumentVo;
import org.dromara.ai.knowledge.domain.vo.TempFileVo;
import org.dromara.ai.knowledge.util.StatusMetaUtils;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.ai.knowledge.service.*;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 文档服务实现
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmDocumentServiceImpl implements IKmDocumentService {

    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final IKmFileService kmFileService;
    private final IKmEtlService etlService;
    private final IKmQuestionService questionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo uploadDocument(Long datasetId, MultipartFile file) {
        return uploadDocument(datasetId, file, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo uploadDocument(Long datasetId, MultipartFile file,
            List<ChunkResult> chunks) {
        try {
            // 保存文档记录
            KmDocument document = saveDocumentRecord(datasetId, file);
            Long docId = document.getId();

            // 获取知识库ID
            KmDataset dataset = datasetMapper.selectById(datasetId);
            Long kbId = dataset.getKbId();

            // 异步处理:确保事务提交后再执行
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processDocumentAfterCommit(docId, kbId, chunks);
                }
            });

            return documentMapper.selectVoById(docId);
        } catch (IOException e) {
            log.error("Failed to upload document", e);
            throw new RuntimeException(MessageUtils.message("ai.msg.file.upload_failed", e.getMessage()));
        }
    }

    /**
     * 保存文档记录到数据库
     */
    private KmDocument saveDocumentRecord(Long datasetId, MultipartFile file) throws IOException {
        // 1. 调用底层文件服务处理上传
        KmFileResult result = kmFileService.upload(file);
        String hash = result.getHashCode();

        // 2. 检查是否已存在相同哈希的文档
        KmDocument existingDoc = documentMapper.selectOne(
                new LambdaQueryWrapper<KmDocument>()
                        .eq(KmDocument::getDatasetId, datasetId)
                        .eq(KmDocument::getHashCode, hash));
        if (existingDoc != null) {
            log.info("Document already exists with hash: {}", hash);
            return existingDoc;
        }

        // 3. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found"));
        }

        // 4. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId());
        document.setOriginalFilename(result.getOriginalFilename());
        document.setOssId(result.getOssId());
        document.setFilePath(result.getFilePath());
        document.setStoreType(result.getStoreType());
        document.setFileType(FileUtil.extName(result.getOriginalFilename()));
        document.setFileSize(result.getFileSize());
        document.setEmbeddingStatus(1); // 1 = 生成中
        document.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                StatusMetaUtils.STATUS_PENDING));
        document.setHashCode(hash);

        documentMapper.insert(document);

        return document;
    }

    /**
     * 文档上传后的异步处理
     * 如果提供了自定义分块,则直接向量化;否则调用ETL处理
     */
    private void processDocumentAfterCommit(Long documentId, Long kbId,
            List<ChunkResult> chunks) {
        try {
            etlService.processDocumentAsync(documentId, chunks);
        } catch (Exception e) {
            log.error("Failed to process document after commit: {}", documentId, e);

            // 更新文档状态为失败
            KmDocument doc = new KmDocument();
            doc.setId(documentId);
            doc.setEmbeddingStatus(3); // 3 = 失败
            doc.setErrorMsg(e.getMessage());
            doc.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_FAILED));
            documentMapper.updateById(doc);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmDocumentVo> uploadDocuments(Long datasetId, MultipartFile[] files) {
        List<KmDocumentVo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadDocument(datasetId, file));
        }
        return results;
    }

    @Override
    public List<KmDocumentVo> listByDatasetId(Long datasetId) {
        LambdaQueryWrapper<KmDocument> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KmDocument::getDatasetId, datasetId);
        lqw.orderByDesc(KmDocument::getCreateTime);
        return documentMapper.selectVoList(lqw);
    }

    @Override
    public List<KmDocumentVo> listByKbId(Long kbId) {
        LambdaQueryWrapper<KmDocument> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KmDocument::getKbId, kbId);
        lqw.orderByDesc(KmDocument::getCreateTime);
        return documentMapper.selectVoList(lqw);
    }

    @Override
    public KmDocumentVo queryById(Long id) {
        return documentMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        KmDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            return true;
        }

        // 1. 删除关联的切片 (EtlService 已实现孤立问题清理)
        etlService.deleteChunksByDocumentId(id);

        // 2. 删除文件 (本地或 OSS)
        deleteFile(doc);

        // 3. 删除文档记录
        return documentMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo createOnlineDocument(Long datasetId, String title, String content, String fileType) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found"));
        }
        if (StringUtils.isBlank(fileType)) {
            fileType = "html";
        }
        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setTitle(title);
        document.setContent(content);
        document.setOriginalFilename(title); // 使用 title 作为文件名
        document.setFileType(fileType);
        document.setEmbeddingStatus(1); // 1 = 生成中
        document.setStatusMeta(
                StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));

        documentMapper.insert(document);

        // 2. 异步触发 ETL 处理
        Long docId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processDocumentAsync(docId, null);
            }
        });

        return documentMapper.selectVoById(document.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo createWebLinkDocument(Long datasetId, String url) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found"));
        }

        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setUrl(url);
        document.setOriginalFilename(url); // 使用 URL 作为文件名
        document.setFileType("url");
        document.setEmbeddingStatus(1); // 1 = 生成中
        document.setStatusMeta(
                StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));

        documentMapper.insert(document);

        // 2. 异步触发 ETL 处理
        Long docId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processDocumentAsync(docId, null);
            }
        });

        return documentMapper.selectVoById(document.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmDocumentVo> batchCreateWebLinkDocument(Long datasetId, List<String> urls) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found"));
        }

        List<KmDocument> documents = new java.util.ArrayList<>();

        // 1. 批量创建文档记录
        for (String url : urls) {
            KmDocument document = new KmDocument();
            document.setDatasetId(datasetId);
            document.setKbId(dataset.getKbId()); // 设置知识库ID
            document.setUrl(url);
            document.setOriginalFilename(url); // 使用 URL 作为文件名
            document.setFileType("url");
            document.setEmbeddingStatus(1); // 1 = 生成中
            document.setStatusMeta(
                    StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                            StatusMetaUtils.STATUS_PENDING));

            documents.add(document);
        }

        documentMapper.insertBatch(documents);

        List<Long> docIds = documents.stream().map(KmDocument::getId).collect(java.util.stream.Collectors.toList());

        // 2. 异步触发 ETL 处理
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long docId : docIds) {
                    etlService.processDocumentAsync(docId, null);
                }
            }
        });

        return documentMapper
                .selectVoList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KmDocument>()
                        .in(KmDocument::getId, docIds));
    }

    @Override
    public TableDataInfo<KmDocumentVo> pageList(KmDocumentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmDocument> lqw = new LambdaQueryWrapper<>();
        // 必填条件: 数据集ID
        lqw.eq(bo.getDatasetId() != null, KmDocument::getDatasetId, bo.getDatasetId());
        // 可选筛选条件
        lqw.eq(bo.getEnabled() != null, KmDocument::getEnabled, bo.getEnabled());
        lqw.eq(bo.getEmbeddingStatus() != null, KmDocument::getEmbeddingStatus, bo.getEmbeddingStatus());
        lqw.eq(bo.getQuestionStatus() != null, KmDocument::getQuestionStatus, bo.getQuestionStatus());
        // 关键词搜索
        lqw.like(StringUtils.isNotBlank(bo.getKeyword()), KmDocument::getOriginalFilename, bo.getKeyword());

        Page<KmDocumentVo> page = documentMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableDocument(Long id, boolean enabled) {
        KmDocument document = new KmDocument();
        document.setId(id);
        document.setEnabled(enabled ? 1 : 0);
        return documentMapper.updateById(document) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnable(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        KmDocument document = new KmDocument();
        document.setEnabled(enabled ? 1 : 0);
        return documentMapper.update(document,
                new LambdaQueryWrapper<KmDocument>().in(KmDocument::getId, ids)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        List<KmDocument> docs = documentMapper.selectByIds(ids);

        for (KmDocument doc : docs) {
            // 1. 删除关联切片 (含孤立问题)
            etlService.deleteChunksByDocumentId(doc.getId());
            // 2. 删除文件
            deleteFile(doc);
        }

        // 3. 批量删除文档
        return documentMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据存储类型删除文件
     */
    private void deleteFile(KmDocument doc) {
        kmFileService.deleteFile(doc.getStoreType(), doc.getOssId(), doc.getFilePath());
    }

    @Override
    public Boolean updateDocumentName(Long id, String name) {
        if (id == null || StringUtils.isBlank(name)) {
            return false;
        }
        KmDocument document = new KmDocument();
        document.setId(id);
        document.setOriginalFilename(name);
        return documentMapper.updateById(document) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEmbedding(List<Long> documentIds, EmbeddingOption option) {
        if (documentIds == null || documentIds.isEmpty()) {
            return false;
        }
        for (Long documentId : documentIds) {
            embeddingDocument(documentId, option);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean embeddingDocument(Long documentId, EmbeddingOption option) {
        KmDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            return false;
        }
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setEmbeddingStatus(1); // 1 = 生成中

        Map<String, Object> meta = doc.getStatusMeta();
        update.setStatusMeta(
                StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_STARTED));

        documentMapper.updateById(update);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processEmbeddingAsync(documentId, option);
            }
        });
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchGenerateQuestions(List<Long> documentIds, Long modelId, String prompt, Double temperature,
            Integer maxTokens) {
        if (documentIds == null || documentIds.isEmpty()) {
            return false;
        }
        for (Long documentId : documentIds) {
            KmDocument update = new KmDocument();
            update.setId(documentId);
            update.setQuestionStatus(1); // 1 = 生成中

            KmDocument exist = documentMapper.selectById(documentId);
            Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
            update.setStatusMeta(StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_GENERATE_QUESTION,
                    StatusMetaUtils.STATUS_STARTED));

            documentMapper.updateById(update);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    questionService.processGenerateQuestionsAsync(documentId, modelId, prompt, temperature, maxTokens);
                }
            });
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TempFileVo uploadTempFile(Long datasetId, MultipartFile file) {
        KmTempFile tempFile = kmFileService.saveTempFile(datasetId, file);
        return convertToTempFileVo(tempFile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TempFileVo> uploadTempFiles(Long datasetId, MultipartFile[] files) {
        List<TempFileVo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadTempFile(datasetId, file));
        }
        return results;
    }

    @Override
    public List<ChunkPreviewVo> previewChunks(ChunkPreviewBo bo) {
        try {
            KmTempFile tempFile = kmFileService.getTempFile(bo.getTempFileId());
            if (tempFile == null) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.temp_file_not_found"));
            }
            String tempPath = tempFile.getFilePath();

            String content = parseFileContent(tempPath);
            if (content == null || content.isBlank()) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.content_empty"));
            }

            List<String> chunks;
            if ("AUTO".equals(bo.getChunkStrategy())) {
                int chunkSize = bo.getChunkSize() != null ? bo.getChunkSize() : 500;
                int overlap = bo.getOverlap() != null ? bo.getOverlap() : 50;
                chunks = splitTextRecursive(content, chunkSize, overlap);
            } else if ("CUSTOM".equals(bo.getChunkStrategy())) {
                if (bo.getSeparators() == null || bo.getSeparators().isEmpty()) {
                    throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.custom_chunk_separator_required"));
                }
                String separator = bo.getSeparators().get(0);
                chunks = splitByCustomSeparator(content, separator);
            } else {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.unsupported_chunk_strategy", bo.getChunkStrategy()));
            }

            List<ChunkPreviewVo> result = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkPreviewVo vo = new ChunkPreviewVo();
                vo.setChunkId("chunk_" + i);
                vo.setContent(chunks.get(i));
                vo.setIndex(i);
                result.add(vo);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to preview chunks", e);
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.chunk_preview_failed", e.getMessage()));
        }
    }

    @Override
    public Map<Long, List<ChunkPreviewVo>> batchPreviewChunks(BatchChunkPreviewBo bo) {
        Map<Long, List<ChunkPreviewVo>> resultMap = new HashMap<>();
        for (Long tempFileId : bo.getTempFileIds()) {
            try {
                ChunkPreviewBo singleBo = new ChunkPreviewBo();
                singleBo.setTempFileId(tempFileId);
                singleBo.setChunkStrategy(bo.getChunkStrategy());
                singleBo.setSeparators(bo.getSeparators());
                singleBo.setChunkSize(bo.getChunkSize());
                singleBo.setOverlap(bo.getOverlap());

                List<ChunkPreviewVo> chunks = previewChunks(singleBo);
                resultMap.put(tempFileId, chunks);
            } catch (Exception e) {
                log.error("Failed to preview chunks for tempFileId: {}", tempFileId, e);
                resultMap.put(tempFileId, new ArrayList<>());
            }
        }
        return resultMap;
    }

    private String parseFileContent(String filePath) {
        try {
            File file = new File(filePath);
            try (FileInputStream fis = new FileInputStream(file)) {
                DocumentParser parser = new ApacheTikaDocumentParser();
                Document doc = parser.parse(fis);
                return doc.text();
            }
        } catch (Exception e) {
            log.error("Failed to parse file: {}", filePath, e);
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.file_parse_failed", e.getMessage()));
        }
    }

    private List<String> splitTextRecursive(String text, int chunkSize, int overlap) {
        var splitter = DocumentSplitters.recursive(chunkSize, overlap);
        Document doc = Document.from(text);
        List<TextSegment> segments = splitter.split(doc);

        List<String> result = new ArrayList<>();
        for (TextSegment segment : segments) {
            result.add(segment.text());
        }
        return result;
    }

    private List<String> splitByCustomSeparator(String text, String separator) {
        String actualSeparator = separator;
        if ("回车".equals(separator) || "\\n".equals(separator)) {
            actualSeparator = "\n";
        } else if ("空格".equals(separator)) {
            actualSeparator = " ";
        } else if ("句号".equals(separator)) {
            actualSeparator = "。";
        }

        String[] parts = text.split(java.util.regex.Pattern.quote(actualSeparator));
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo submitChunks(ChunkSubmitBo bo) {
        try {
            KmDataset dataset = datasetMapper.selectById(bo.getDatasetId());
            if (dataset == null) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found"));
            }

            KmTempFile tempRecord = kmFileService.getTempFile(bo.getTempFileId());
            if (tempRecord == null) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.temp_file_not_found"));
            }
            String tempPath = tempRecord.getFilePath();

            File tempFile = new File(tempPath);
            if (!tempFile.exists()) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.temp_file_not_found"));
            }

            String filename = tempFile.getName();
            if (filename.contains("_") && filename.matches("^\\d+_.*")) {
                filename = filename.substring(filename.indexOf("_") + 1);
            }
            String fileExtension = FileUtil.extName(filename);

            KmFileResult result = kmFileService.upload(tempFile);

            KmDocument document = new KmDocument();
            document.setDatasetId(bo.getDatasetId());
            document.setKbId(dataset.getKbId());
            document.setOriginalFilename(filename);
            document.setOssId(result.getOssId());
            document.setFilePath(result.getFilePath());
            document.setStoreType(result.getStoreType());
            document.setFileType(fileExtension);
            document.setFileSize(result.getFileSize());
            document.setEmbeddingStatus(1);
            document.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_PENDING));
            document.setQuestionStatus(0);
            document.setChunkCount(bo.getChunks().size());

            documentMapper.insert(document);

            Long docId = document.getId();
            Long kbId = dataset.getKbId();
            List<ChunkResult> chunkResults = new ArrayList<>();
            for (int i = 0; i < bo.getChunks().size(); i++) {
                ChunkSubmitBo.ChunkItem item = bo.getChunks().get(i);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunkIndex", i);
                metadata.put("totalChunks", bo.getChunks().size());
                metadata.put("customChunk", true);

                chunkResults.add(ChunkResult.builder()
                        .content(item.getContent())
                        .title(item.getTitle())
                        .metadata(metadata)
                        .build());
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processDocumentAfterCommit(docId, kbId, chunkResults);
                }
            });

            return documentMapper.selectVoById(docId);
        } catch (Exception e) {
            log.error("Failed to submit chunks", e);
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.chunk_submit_failed", e.getMessage()));
        }
    }

    @Override
    public void downloadDocument(Long id, HttpServletResponse response) {
        KmDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.document_not_found"));
        }
        kmFileService.download(doc.getStoreType(), doc.getOssId(), doc.getFilePath(), doc.getOriginalFilename(), response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean retryWorkflowDocument(Long documentId) {
        KmDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.document_not_found"));
        }
        // 只有处于失败状态(3)的文档才允许重跑
        if (doc.getEmbeddingStatus() == null || doc.getEmbeddingStatus() != 3) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.document_not_failed"));
        }

        // 1. 清理该文档已产生的切片（防止重复向量化）
        etlService.deleteChunksByDocumentId(documentId);

        // 2. 将状态重置为 0（待处理），清空错误信息
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setEmbeddingStatus(0);
        update.setErrorMsg(null);
        update.setStatusMeta(StatusMetaUtils.updateStateTime(
                doc.getStatusMeta(), StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));
        return documentMapper.updateById(update) > 0;
    }

    private TempFileVo convertToTempFileVo(KmTempFile tempFile) {
        TempFileVo vo = new TempFileVo();
        vo.setId(tempFile.getId());
        vo.setDatasetId(tempFile.getDatasetId());
        vo.setOriginalFilename(tempFile.getOriginalFilename());
        vo.setFileExtension(tempFile.getFileExtension());
        vo.setFileSize(tempFile.getFileSize());
        vo.setTempPath(tempFile.getFilePath()); // VO 保持 tempPath 命名
        return vo;
    }
}

