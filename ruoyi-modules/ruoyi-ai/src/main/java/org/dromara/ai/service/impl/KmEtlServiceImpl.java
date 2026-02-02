package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.service.IKmEtlService;
import org.dromara.ai.service.etl.DatasetProcessType;
import org.dromara.ai.service.etl.EtlHandler;
import org.dromara.ai.mapper.KmQuestionMapper;
import org.dromara.ai.mapper.KmQuestionChunkMapMapper;
import org.dromara.ai.domain.KmQuestionChunkMap;
import org.dromara.ai.domain.KmEmbedding;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ETL处理服务实现
 * 负责文档的解析、分块、向量化
 * 使用策略模式，根据数据集类型选择不同的 EtlHandler
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@Service
public class KmEtlServiceImpl implements IKmEtlService {

    private final KmDocumentMapper documentMapper;
    private final KmDocumentChunkMapper chunkMapper;
    private final org.dromara.ai.mapper.KmEmbeddingMapper embeddingMapper;
    private final KmDatasetMapper datasetMapper;
    private final ISysOssService ossService;
    private final EmbeddingModel embeddingModel;
    private final List<EtlHandler> etlHandlers;
    private final KmQuestionMapper questionMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;

    // 文档解析器 (Tika) - 保留用于兼容旧接口
    private final DocumentParser documentParser = new ApacheTikaDocumentParser();

    @Autowired
    public KmEtlServiceImpl(
            KmDocumentMapper documentMapper,
            KmDocumentChunkMapper chunkMapper,
            org.dromara.ai.mapper.KmEmbeddingMapper embeddingMapper,
            KmDatasetMapper datasetMapper,
            ISysOssService ossService,
            EmbeddingModel embeddingModel,
            List<EtlHandler> etlHandlers,
            KmQuestionMapper questionMapper,
            KmQuestionChunkMapMapper questionChunkMapMapper) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.embeddingMapper = embeddingMapper;
        this.datasetMapper = datasetMapper;
        this.ossService = ossService;
        this.embeddingModel = embeddingModel;
        this.etlHandlers = etlHandlers;
        this.questionMapper = questionMapper;
        this.questionChunkMapMapper = questionChunkMapMapper;
    }

    @Override
    @Async
    public void processDocumentAsync(Long documentId) {
        log.info("Start processing document: {}", documentId);

        KmDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            log.error("Document not found: {}", documentId);
            return;
        }

        try {
            // 更新状态为处理中
            updateDocumentStatus(documentId, "PROCESSING", null);

            // 获取数据集信息
            KmDataset dataset = datasetMapper.selectById(document.getDatasetId());
            if (dataset == null) {
                updateDocumentStatus(documentId, "ERROR", "数据集不存在");
                return;
            }

            // 获取知识库ID
            Long kbId = dataset.getKbId();
            if (kbId == null) {
                kbId = document.getKbId();
            }

            // 文件格式校验
            if (StringUtils.isNotBlank(dataset.getAllowedFileTypes())
                    && StringUtils.isNotBlank(document.getOriginalFilename())) {
                try {
                    org.dromara.ai.util.FileTypeValidator.validate(
                            document.getOriginalFilename(),
                            dataset.getAllowedFileTypes());
                } catch (Exception e) {
                    updateDocumentStatus(documentId, "ERROR", e.getMessage());
                    return;
                }
            }

            // 根据数据集处理类型选择 Handler
            String processType = dataset.getProcessType();
            if (processType == null) {
                processType = DatasetProcessType.GENERIC_FILE;
            }

            EtlHandler handler = findHandler(processType);
            if (handler == null) {
                // 兜底使用旧逻辑
                log.warn("No handler found for processType: {}, using legacy logic", processType);
                processLegacy(document, kbId);
            } else {
                // 使用 Handler 处理
                handler.process(document, dataset, kbId);
            }

            // 更新文档状态
            updateDocumentStatusCompleted(documentId);

            log.info("Document processing completed: {}", documentId);

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            updateDocumentStatus(documentId, "ERROR", e.getMessage());
        }
    }

    /**
     * 查找支持指定处理类型的 Handler
     */
    private EtlHandler findHandler(String processType) {
        for (EtlHandler handler : etlHandlers) {
            if (handler.supports(processType)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 兼容旧逻辑的处理方法
     */
    private void processLegacy(KmDocument document, Long kbId) {
        // 1. 解析文档
        String content = parseDocument(document.getId());
        if (content == null || content.isBlank()) {
            throw new RuntimeException("文档内容为空");
        }

        // 2. 分块
        List<String> chunks = splitText(content, 500, 50);
        if (CollUtil.isEmpty(chunks)) {
            throw new RuntimeException("分块失败");
        }

        // 3. 向量化并存储
        embedAndStore(document.getId(), kbId, chunks);
    }

    private void updateDocumentStatusCompleted(Long documentId) {
        // 统计 chunk 数量
        int chunkCount = chunkMapper.countByDocumentId(documentId);

        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setStatus("COMPLETED");
        update.setChunkCount(chunkCount);
        documentMapper.updateById(update);
    }

    @Override
    public String parseDocument(Long documentId) {
        KmDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return null;
        }
        try {
            Long ossId = document.getOssId();
            if (ossId == null) {
                throw new RuntimeException("Document OSS ID is null");
            }

            SysOssVo ossVo = ossService.getById(ossId);
            if (ossVo == null) {
                throw new RuntimeException("OSS file not found: " + ossId);
            }

            OssClient storage = OssFactory.instance(ossVo.getService());
            try (InputStream is = storage.getObjectContent(ossVo.getFileName())) {
                Document doc = documentParser.parse(is);
                return doc.text();
            }
        } catch (Exception e) {
            log.error("Failed to parse document: {}", documentId, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> splitText(String text, int chunkSize, int overlap) {
        var splitter = DocumentSplitters.recursive(chunkSize, overlap);
        Document doc = Document.from(text);
        List<TextSegment> segments = splitter.split(doc);

        List<String> result = new ArrayList<>();
        for (TextSegment segment : segments) {
            result.add(segment.text());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void embedAndStore(Long documentId, Long kbId, List<String> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }

        List<KmDocumentChunk> chunkEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            float[] embedding = embeddingModel.embed(chunkText).content().vector();

            KmDocumentChunk chunk = new KmDocumentChunk();
            chunk.setId(IdUtil.getSnowflakeNextId());
            chunk.setDocumentId(documentId);
            chunk.setKbId(kbId);
            chunk.setContent(chunkText);
            chunk.setEmbedding(embedding);
            chunk.setEmbeddingString(java.util.Arrays.toString(embedding));
            chunk.setCreateTime(now);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            chunk.setMetadata(metadata);

            chunkEntities.add(chunk);
        }

        chunkMapper.insertBatch(chunkEntities);

        // 同步写入 Unified Index (km_embedding)
        List<org.dromara.ai.domain.KmEmbedding> embeddings = new ArrayList<>();
        for (KmDocumentChunk chunk : chunkEntities) {
            org.dromara.ai.domain.KmEmbedding emp = new org.dromara.ai.domain.KmEmbedding();
            emp.setId(IdUtil.getSnowflakeNextId());
            emp.setKbId(chunk.getKbId());
            emp.setSourceId(chunk.getId());
            emp.setSourceType(org.dromara.ai.domain.KmEmbedding.SourceType.CONTENT);
            emp.setEmbedding(chunk.getEmbedding());
            emp.setEmbeddingString(chunk.getEmbeddingString());
            emp.setTextContent(chunk.getContent());
            emp.setCreateTime(now);
            embeddings.add(emp);
        }
        embeddingMapper.insertBatch(embeddings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChunksByDocumentId(Long documentId) {
        // 1. Get Chunk IDs
        List<KmDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KmDocumentChunk>()
                .eq(KmDocumentChunk::getDocumentId, documentId));

        if (CollUtil.isEmpty(chunks))
            return;

        List<Long> chunkIds = chunks.stream().map(KmDocumentChunk::getId).toList();

        // 2. Delete from km_embedding (SourceType=CONTENT (1))
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .in(KmEmbedding::getSourceId, chunkIds)
                .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.CONTENT));

        // 3. Find associated questions
        List<KmQuestionChunkMap> maps = questionChunkMapMapper.selectList(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .in(KmQuestionChunkMap::getChunkId, chunkIds));

        if (CollUtil.isNotEmpty(maps)) {
            List<Long> qIds = maps.stream().map(KmQuestionChunkMap::getQuestionId).toList();

            // Delete questions and their embeddings
            embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                    .in(KmEmbedding::getSourceId, qIds)
                    .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.QUESTION));

            questionMapper.deleteByIds(qIds);

            // Delete maps
            questionChunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                    .in(KmQuestionChunkMap::getChunkId, chunkIds));
        }

        // 4. Delete Chunks
        chunkMapper.deleteByIds(chunkIds);
    }

    private void updateDocumentStatus(Long documentId, String status, String errorMsg) {
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        documentMapper.updateById(update);
    }
}
