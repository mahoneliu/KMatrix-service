package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.ai.util.StatusMetaUtils;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用文件 ETL 处理器
 * 处理 PDF、Office、纯文本类文件
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericFileEtlHandler implements EtlHandler {

    private final KmDocumentChunkMapper chunkMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final ISysOssService ossService;
    private final EmbeddingModel embeddingModel;

    private final DocumentParser documentParser = new ApacheTikaDocumentParser();

    @Override
    public String getProcessType() {
        return DatasetProcessType.GENERIC_FILE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(KmDocument document, KmDataset dataset, Long kbId) {
        log.info("GenericFileEtlHandler processing document: {}", document.getId());

        // 1. 解析文档并提取 title
        ParseResult parseResult = parseDocumentWithTitle(document);
        String content = parseResult.content;
        String title = parseResult.title;

        if (content == null || content.isBlank()) {
            throw new RuntimeException("文档内容为空");
        }

        // 2. 获取分块配置 (优先从实体字段读取，兼容旧 config JSON)
        int chunkSize = dataset.getMaxChunkSize() != null ? dataset.getMaxChunkSize()
                : getConfigInt(dataset, "chunkSize", 500);
        int overlap = dataset.getChunkOverlap() != null ? dataset.getChunkOverlap()
                : getConfigInt(dataset, "overlap", 50);

        // 3. 分块
        List<String> chunks = splitText(content, chunkSize, overlap);
        if (CollUtil.isEmpty(chunks)) {
            throw new RuntimeException("分块失败");
        }

        // 4. 向量化并存储到 km_document_chunk 和 km_embedding
        embedAndStore(document.getId(), kbId, chunks, title);

        // 5. 为文档 title 生成向量 (文档级别,只存一次)
        if (title != null && !title.isBlank()) {
            embedTitleForDocument(document.getId(), kbId, title);
        }

        log.info("GenericFileEtlHandler completed: documentId={}, chunks={}, title={}",
                document.getId(), chunks.size(), title);
    }

    /**
     * 解析结果
     */
    private static class ParseResult {
        String content;
        String title;

        ParseResult(String content, String title) {
            this.content = content;
            this.title = title;
        }
    }

    /**
     * 解析文档并提取 title
     */
    private ParseResult parseDocumentWithTitle(KmDocument document) {
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
                // 使用 Tika 解析文档
                Document doc = documentParser.parse(is);
                String content = doc.text();

                // 提取 title (使用文件名,移除扩展名)
                String title = document.getOriginalFilename();
                if (title != null && title.contains(".")) {
                    title = title.substring(0, title.lastIndexOf('.'));
                }

                return new ParseResult(content, title);
            }
        } catch (Exception e) {
            log.error("Failed to parse document: {}", document.getId(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        var splitter = DocumentSplitters.recursive(chunkSize, overlap);
        Document doc = Document.from(text);
        List<TextSegment> segments = splitter.split(doc);

        List<String> result = new ArrayList<>();
        for (TextSegment segment : segments) {
            result.add(segment.text());
        }
        return result;
    }

    private void embedAndStore(Long documentId, Long kbId, List<String> chunks, String title) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }

        List<KmDocumentChunk> chunkEntities = new ArrayList<>();
        List<KmEmbedding> embeddings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            // 生成向量
            float[] vector = embeddingModel.embed(chunkText).content().vector();
            String vectorString = Arrays.toString(vector);

            // 构建切片实体
            Long chunkId = IdUtil.getSnowflakeNextId();
            KmDocumentChunk chunk = new KmDocumentChunk();
            chunk.setId(chunkId);
            chunk.setDocumentId(documentId);
            chunk.setKbId(kbId);
            chunk.setContent(chunkText);
            // chunk.setTitle(title);
            // chunk.setEmbedding(vector);
            // chunk.setEmbeddingString(vectorString);
            chunk.setCreateTime(now);

            // 元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            chunk.setMetadata(metadata);

            chunk.setEmbeddingStatus(2); // 2 = 已生成
            chunk.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_SUCCESS));

            chunkEntities.add(chunk);

            // 构建 embedding 实体 (存储到 km_embedding)
            KmEmbedding embedding = new KmEmbedding();
            embedding.setId(IdUtil.getSnowflakeNextId());
            embedding.setKbId(kbId);
            embedding.setSourceId(chunkId);
            embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
            embedding.setEmbedding(vector);
            embedding.setEmbeddingString(vectorString);
            embedding.setTextContent(chunkText);
            embedding.setCreateTime(now);

            embeddings.add(embedding);
        }

        // 批量插入
        chunkMapper.insertBatch(chunkEntities);
        embeddingMapper.insertBatch(embeddings);
    }

    /**
     * 为文档 title 生成向量 (文档级别,只存一次)
     */
    private void embedTitleForDocument(Long documentId, Long kbId, String title) {
        try {
            // 生成 title 向量
            float[] titleVector = embeddingModel.embed(title).content().vector();
            String vectorString = Arrays.toString(titleVector);

            // 构建 embedding 实体
            KmEmbedding titleEmbedding = new KmEmbedding();
            titleEmbedding.setId(IdUtil.getSnowflakeNextId());
            titleEmbedding.setKbId(kbId);
            titleEmbedding.setSourceId(documentId); // 注意: 这里是 documentId 而非 chunkId
            titleEmbedding.setSourceType(KmEmbedding.SourceType.TITLE);
            titleEmbedding.setEmbedding(titleVector);
            titleEmbedding.setEmbeddingString(vectorString);
            titleEmbedding.setTextContent(title);
            titleEmbedding.setCreateTime(LocalDateTime.now());

            // 插入数据库
            embeddingMapper.insert(titleEmbedding);

            log.info("Title vectorized for document: {}, title: {}", documentId, title);
        } catch (Exception e) {
            log.error("Failed to vectorize title for document: {}", documentId, e);
            // 不抛出异常,避免影响整体流程
        }
    }

    private int getConfigInt(KmDataset dataset, String key, int defaultValue) {
        if (dataset.getConfig() == null) {
            return defaultValue;
        }
        Object value = dataset.getConfig().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
