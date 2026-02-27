package org.dromara.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.bo.*;
import org.dromara.ai.domain.enums.EmbeddingOption;
import org.dromara.ai.domain.vo.ChunkPreviewVo;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.ai.domain.vo.TempFileVo;
import org.dromara.ai.util.StatusMetaUtils;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.enums.FileStoreType;
import org.dromara.ai.domain.vo.LocalFileVo;
import org.dromara.ai.service.IKmDocumentService;
import org.dromara.ai.service.IKmEtlService;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.ai.service.IKmTempFileService;
import org.dromara.ai.service.ILocalFileService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.domain.vo.SysOssVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

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
    private final ISysOssService ossService;
    private final ILocalFileService localFileService;
    private final KmAiProperties aiProperties;
    private final IKmEtlService etlService;
    private final IKmQuestionService questionService;
    private final IKmTempFileService tempFileService;

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
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存文档记录到数据库
     */
    private KmDocument saveDocumentRecord(Long datasetId, MultipartFile file) throws IOException {
        // 1. 计算文件哈希 (用于去重)
        String hash = DigestUtil.sha256Hex(file.getInputStream());

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
            throw new RuntimeException("数据集不存在");
        }

        // 4. 根据配置选择存储方式
        FileStoreType storeType = FileStoreType.fromValue(aiProperties.getFileStore().getType());
        Long ossId = null;
        String filePath = null;
        Long fileSize = 0L;

        if (storeType.isOss()) {
            // OSS 存储
            SysOssVo ossVo = ossService.upload(file);
            ossId = ossVo.getOssId();
            filePath = ossVo.getUrl();
            fileSize = file.getSize();
            log.info("文件上传到 OSS: {}", filePath);
        } else if (storeType.isLocal()) {
            // 本地存储
            LocalFileVo localFileVo = localFileService.upload(file);
            filePath = localFileVo.getFilePath();
            fileSize = localFileVo.getFileSize();
            log.info("文件保存到本地: {}", filePath);
        } else {
            throw new RuntimeException("不支持的存储类型: " + storeType);
        }

        // 5. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId());
        document.setOriginalFilename(file.getOriginalFilename());
        document.setOssId(ossId);
        document.setFilePath(filePath);
        document.setStoreType(storeType.getValue());
        document.setFileType(FileUtil.extName(file.getOriginalFilename()));
        document.setFileSize(fileSize);
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

    // @Override
    // public Boolean reprocessEmbeddingDocument(Long id) {
    // // 更新状态为 PENDING
    // KmDocument doc = new KmDocument();
    // doc.setId(id);
    // doc.setEmbeddingStatus(1); // 1 = 生成中

    // // 更新状态元数据
    // KmDocument exist = documentMapper.selectById(id);
    // Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
    // doc.setStatusMeta(
    // StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_EMBEDDING,
    // StatusMetaUtils.STATUS_PENDING));
    // doc.setErrorMsg(null);
    // documentMapper.updateById(doc);

    // // 删除旧的切片
    // etlService.deleteChunksByDocumentId(id);

    // // 重新触发 ETL
    // etlService.processDocumentAsync(id, null);
    // return true;
    // }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo createOnlineDocument(Long datasetId, String title, String content) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException("数据集不存在");
        }

        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setTitle(title);
        document.setContent(content);
        document.setOriginalFilename(title); // 使用 title 作为文件名
        document.setFileType("html");
        document.setFileType("html");
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
            throw new RuntimeException("数据集不存在");
        }

        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setUrl(url);
        document.setOriginalFilename(url); // 使用 URL 作为文件名
        document.setFileType("url");
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
            throw new RuntimeException("数据集不存在");
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

        List<KmDocument> docs = documentMapper.selectBatchIds(ids);

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
        try {
            FileStoreType storeType = FileStoreType.fromValue(doc.getStoreType());
            if (storeType.isLocal()) {
                if (StringUtils.isNotBlank(doc.getFilePath())) {
                    localFileService.delete(doc.getFilePath());
                }
            } else if (storeType.isOss()) {
                if (doc.getOssId() != null) {
                    ossService.deleteWithValidByIds(List.of(doc.getOssId()), true);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete file for document: {}", doc.getId(), e);
            // 文件删除失败不应阻止文档记录删除，记录日志即可
        }
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
        // 遍历每个文档,重新触发向量化流程
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
        // 更新向量化状态为"生成中"
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setEmbeddingStatus(1); // 1 = 生成中
        update.setEmbeddingStatus(1); // 1 = 生成中

        // 更新状态元数据
        Map<String, Object> meta = doc.getStatusMeta();
        update.setStatusMeta(
                StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_STARTED));

        documentMapper.updateById(update);

        // 异步执行向量化 (事务提交后执行)
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
        // 遍历每个文档，为其所有切片生成问题
        for (Long documentId : documentIds) {
            // 更新问题生成状态为"生成中"
            KmDocument update = new KmDocument();
            update.setId(documentId);
            update.setQuestionStatus(1); // 1 = 生成中
            update.setQuestionStatus(1); // 1 = 生成中

            // 更新状态元数据
            KmDocument exist = documentMapper.selectById(documentId);
            Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
            update.setStatusMeta(StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_GENERATE_QUESTION,
                    StatusMetaUtils.STATUS_STARTED));

            documentMapper.updateById(update);

            // 异步生成问题 (事务提交后执行)
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
        return tempFileService.saveTempFile(datasetId, file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TempFileVo> uploadTempFiles(Long datasetId, MultipartFile[] files) {
        List<TempFileVo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(tempFileService.saveTempFile(datasetId, file));
        }
        return results;
    }

    @Override
    public List<ChunkPreviewVo> previewChunks(ChunkPreviewBo bo) {
        try {
            // 1. 获取临时文件路径
            String tempPath = tempFileService.getTempFilePath(bo.getTempFileId());
            if (tempPath == null) {
                throw new RuntimeException("临时文件不存在");
            }

            // 2. 解析文件内容
            String content = parseFileContent(tempPath);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("文件内容为空");
            }

            // 3. 根据策略进行分块
            List<String> chunks;
            if ("AUTO".equals(bo.getChunkStrategy())) {
                // 自动分块: 使用默认配置
                int chunkSize = bo.getChunkSize() != null ? bo.getChunkSize() : 500;
                int overlap = bo.getOverlap() != null ? bo.getOverlap() : 50;
                chunks = splitTextRecursive(content, chunkSize, overlap);
            } else if ("CUSTOM".equals(bo.getChunkStrategy())) {
                // 自定义分块: 根据分隔符分割
                if (bo.getSeparators() == null || bo.getSeparators().isEmpty()) {
                    throw new RuntimeException("自定义分块需要指定分隔符");
                }
                String separator = bo.getSeparators().get(0); // 使用第一个分隔符
                chunks = splitByCustomSeparator(content, separator);
            } else {
                throw new RuntimeException("不支持的分块策略: " + bo.getChunkStrategy());
            }

            // 4. 转换为预览VO
            List<ChunkPreviewVo> result = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkPreviewVo vo = new ChunkPreviewVo();
                vo.setChunkId("chunk_" + i);
                vo.setContent(chunks.get(i));
                vo.setIndex(i);
                result.add(vo);
            }

            log.info("Generated {} chunks for tempFileId: {}", result.size(), bo.getTempFileId());
            return result;
        } catch (Exception e) {
            log.error("Failed to preview chunks", e);
            throw new RuntimeException("分块预览失败: " + e.getMessage());
        }
    }

    @Override
    public Map<Long, List<ChunkPreviewVo>> batchPreviewChunks(BatchChunkPreviewBo bo) {
        Map<Long, List<ChunkPreviewVo>> resultMap = new HashMap<>();

        for (Long tempFileId : bo.getTempFileIds()) {
            try {
                // 为每个文件创建单独的预览请求
                ChunkPreviewBo singleBo = new ChunkPreviewBo();
                singleBo.setTempFileId(tempFileId);
                singleBo.setChunkStrategy(bo.getChunkStrategy());
                singleBo.setSeparators(bo.getSeparators());
                singleBo.setChunkSize(bo.getChunkSize());
                singleBo.setOverlap(bo.getOverlap());

                // 调用单文件预览方法
                List<ChunkPreviewVo> chunks = previewChunks(singleBo);
                resultMap.put(tempFileId, chunks);

                log.info("Batch preview: generated {} chunks for tempFileId: {}", chunks.size(), tempFileId);
            } catch (Exception e) {
                log.error("Failed to preview chunks for tempFileId: {}", tempFileId, e);
                // 单个文件失败不影响其他文件,记录空列表
                resultMap.put(tempFileId, new ArrayList<>());
            }
        }

        log.info("Batch preview completed for {} files", bo.getTempFileIds().size());
        return resultMap;
    }

    /**
     * 解析文件内容
     */
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
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 递归分块
     */
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

    /**
     * 根据自定义分隔符分块
     */
    private List<String> splitByCustomSeparator(String text, String separator) {
        String actualSeparator = separator;
        if ("回车".equals(separator) || "\\n".equals(separator)) {
            actualSeparator = "\n";
        } else if ("空格".equals(separator)) {
            actualSeparator = " ";
        } else if ("句号".equals(separator)) {
            actualSeparator = "。";
        }

        String[] parts = text.split(actualSeparator);
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
            // 1. 获取数据集信息
            KmDataset dataset = datasetMapper.selectById(bo.getDatasetId());
            if (dataset == null) {
                throw new RuntimeException("数据集不存在");
            }

            // 2. 获取临时文件信息
            String tempPath = tempFileService.getTempFilePath(bo.getTempFileId());
            if (tempPath == null) {
                throw new RuntimeException("临时文件不存在");
            }

            File tempFile = new File(tempPath);
            if (!tempFile.exists()) {
                throw new RuntimeException("临时文件不存在: " + tempPath);
            }

            String filename = tempFile.getName();
            // 移除时间戳前缀
            if (filename.contains("_")) {
                filename = filename.substring(filename.indexOf("_") + 1);
            }
            String fileExtension = FileUtil.extName(filename);

            // 3. 保存文件到OSS或本地存储
            FileStoreType storeType = FileStoreType.fromValue(aiProperties.getFileStore().getType());
            Long ossId = null;
            String filePath = null;
            Long fileSize = tempFile.length();

            if (storeType.isOss()) {
                // OSS 存储 - 将临时文件上传到OSS
                SysOssVo ossVo = ossService.upload(tempFile);
                ossId = ossVo.getOssId();
                filePath = ossVo.getUrl();
                log.info("临时文件上传到 OSS: {}", filePath);
            } else if (storeType.isLocal()) {
                // 本地存储 - 将临时文件复制到本地存储目录
                LocalFileVo localFileVo = localFileService.upload(tempFile);
                filePath = localFileVo.getFilePath();
                fileSize = localFileVo.getFileSize();
                log.info("临时文件保存到本地: {}", filePath);
            } else {
                throw new RuntimeException("不支持的存储类型: " + storeType);
            }

            // 4. 创建文档记录
            KmDocument document = new KmDocument();
            document.setDatasetId(bo.getDatasetId());
            document.setKbId(dataset.getKbId());
            document.setOriginalFilename(filename);
            document.setOssId(ossId);
            document.setFilePath(filePath);
            document.setStoreType(storeType.getValue());
            document.setFileType(fileExtension);
            document.setFileSize(fileSize);
            document.setEmbeddingStatus(1); // 1 = 待向量化
            document.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_PENDING));
            document.setQuestionStatus(0); // 无需问题生成
            document.setChunkCount(bo.getChunks().size()); // 预设分块数量

            documentMapper.insert(document);

            // 5. 转换ChunkItem为ChunkResult
            Long docId = document.getId();
            Long kbId = dataset.getKbId();
            List<ChunkResult> chunkResults = new ArrayList<>();
            for (int i = 0; i < bo.getChunks().size(); i++) {
                ChunkSubmitBo.ChunkItem item = bo.getChunks().get(i);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunkIndex", i);
                metadata.put("totalChunks", bo.getChunks().size());
                metadata.put("customChunk", true); // 标记为用户自定义分块

                chunkResults.add(ChunkResult.builder()
                        .content(item.getContent())
                        .title(item.getTitle())
                        .metadata(metadata)
                        .build());
            }

            // 6. 异步触发向量化处理 (确保事务提交后再执行)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processDocumentAfterCommit(docId, kbId, chunkResults);
                }
            });

            // 7. 返回文档信息
            return documentMapper.selectVoById(docId);
        } catch (Exception e) {
            log.error("Failed to submit chunks", e);
            throw new RuntimeException("分块提交失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadDocument(Long id, HttpServletResponse response) {
        KmDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }

        try {
            FileStoreType storeType = FileStoreType.fromValue(doc.getStoreType());
            if (storeType.isOss()) {
                if (doc.getOssId() != null) {
                    ossService.download(doc.getOssId(), response);
                } else {
                    throw new RuntimeException("OSS文件ID丢失");
                }
            } else if (storeType.isLocal()) {
                String filePath = doc.getFilePath();
                if (StringUtils.isBlank(filePath)) {
                    throw new RuntimeException("文件路径丢失");
                }

                // 本地文件下载
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                String fileName = URLEncoder.encode(doc.getOriginalFilename(), StandardCharsets.UTF_8).replaceAll("\\+",
                        "%20");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                try (InputStream is = localFileService.getFileStream(filePath);
                        OutputStream os = response.getOutputStream()) {
                    if (is == null) {
                        throw new RuntimeException("文件流获取失败");
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                throw new RuntimeException("不支持的存储类型: " + storeType);
            }
        } catch (Exception e) {
            log.error("Failed to download document: {}", id, e);
            throw new RuntimeException("下载失败: " + e.getMessage());
        }
    }

}
