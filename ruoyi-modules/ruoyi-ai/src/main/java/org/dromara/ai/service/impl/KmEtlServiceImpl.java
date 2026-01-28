package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.service.IKmEtlService;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.domain.vo.SysOssVo;
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
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmEtlServiceImpl implements IKmEtlService {

    private final KmDocumentMapper documentMapper;
    private final KmDocumentChunkMapper chunkMapper;
    private final ISysOssService ossService;

    // 使用本地 ONNX 模型 (AllMiniLmL6V2)
    // 后续可以替换为 BGE-M3
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    // 文档解析器 (Tika)
    private final DocumentParser documentParser = new ApacheTikaDocumentParser();

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

            // 1. 解析文档
            String content = parseDocument(documentId);
            if (content == null || content.isBlank()) {
                updateDocumentStatus(documentId, "ERROR", "文档内容为空");
                return;
            }

            // 2. 分块
            List<String> chunks = splitText(content, 500, 50);
            if (CollUtil.isEmpty(chunks)) {
                updateDocumentStatus(documentId, "ERROR", "分块失败");
                return;
            }

            // 3. 向量化并存储
            embedAndStore(documentId, chunks);

            // 4. 更新文档状态
            KmDocument update = new KmDocument();
            update.setId(documentId);
            update.setStatus("COMPLETED");
            update.setTokenCount(content.length()); // 简化: 用字符数代替 token
            update.setChunkCount(chunks.size());
            documentMapper.updateById(update);

            log.info("Document processing completed: {}, chunks: {}", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            updateDocumentStatus(documentId, "ERROR", e.getMessage());
        }
    }

    @Override
    public String parseDocument(Long documentId) {
        KmDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return null;
        }
        try {
            // 通过 OSS ID 获取文件
            Long ossId = document.getOssId();
            if (ossId == null) {
                throw new RuntimeException("Document OSS ID is null");
            }

            SysOssVo ossVo = ossService.getById(ossId);
            if (ossVo == null) {
                throw new RuntimeException("OSS file not found: " + ossId);
            }

            // 使用 OssClient 直接获取内容，避免 URL 格式问题（如 http://http://）
            org.dromara.common.oss.core.OssClient storage = org.dromara.common.oss.factory.OssFactory
                    .instance(ossVo.getService());
            try (InputStream is = storage.getObjectContent(ossVo.getFileName())) { // 使用 fileName 而不是 full URL
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
        // 使用 LangChain4j 的分块器
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
    public void embedAndStore(Long documentId, List<String> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }

        List<KmDocumentChunk> chunkEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            // 生成向量
            float[] embedding = embeddingModel.embed(chunkText).content().vector();

            // 构建切片实体
            KmDocumentChunk chunk = new KmDocumentChunk();
            chunk.setId(IdUtil.getSnowflakeNextId());
            chunk.setDocumentId(documentId);
            chunk.setContent(chunkText);
            chunk.setEmbedding(embedding);
            // 将 float[] 转换为 PostgreSQL vector 格式字符串: [1.0,2.0,3.0]
            chunk.setEmbeddingString(java.util.Arrays.toString(embedding));
            chunk.setCreateTime(now);

            // 元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            chunk.setMetadata(metadata);

            chunkEntities.add(chunk);
        }

        // 批量插入
        chunkMapper.insertBatch(chunkEntities);
    }

    @Override
    public void deleteChunksByDocumentId(Long documentId) {
        chunkMapper.deleteByDocumentId(documentId);
    }

    private void updateDocumentStatus(Long documentId, String status, String errorMsg) {
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        documentMapper.updateById(update);
    }
}
