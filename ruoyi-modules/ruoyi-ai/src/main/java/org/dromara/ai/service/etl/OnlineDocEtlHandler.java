package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import dev.langchain4j.data.document.Document;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线文档 ETL 处理器
 * 处理用户通过富文本编辑器输入的在线文档
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineDocEtlHandler implements EtlHandler {

    private final EmbeddingModel embeddingModel;
    private final KmDocumentChunkMapper chunkMapper;
    private final KmEmbeddingMapper embeddingMapper;

    @Override
    public String getProcessType() {
        return DatasetProcessType.ONLINE_DOC;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(KmDocument document, KmDataset dataset, Long kbId) {
        log.info("OnlineDocEtlHandler processing document: {}", document.getId());

        // 1. 从 document.content 读取富文本内容
        String content = document.getContent();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("在线文档内容为空");
        }

        // 2. 获取 title
        String title = document.getTitle();
        if (title == null || title.isBlank()) {
            title = "未命名文档";
        }

        // 3. 移除 HTML 标签 (简单处理,保留文本内容)
        String plainText = stripHtmlTags(content);

        // 4. 获取分块配置
        int chunkSize = dataset.getMaxChunkSize() != null ? dataset.getMaxChunkSize() : 500;
        int overlap = dataset.getChunkOverlap() != null ? dataset.getChunkOverlap() : 50;

        // 5. 分块
        List<String> chunks = splitText(plainText, chunkSize, overlap);
        if (CollUtil.isEmpty(chunks)) {
            throw new RuntimeException("分块失败");
        }

        // 6. 向量化并存储
        embedAndStore(document.getId(), kbId, chunks, title);

        // 7. 为文档 title 生成向量
        if (title != null && !title.isBlank()) {
            embedTitleForDocument(document.getId(), kbId, title);
        }

        log.info("OnlineDocEtlHandler completed: documentId={}, chunks={}, title={}",
                document.getId(), chunks.size(), title);
    }

    /**
     * 移除 HTML 标签 (简单实现)
     */
    private String stripHtmlTags(String html) {
        if (html == null) {
            return "";
        }
        // 简单的 HTML 标签移除 (实际项目中可使用 Jsoup)
        return html.replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .trim();
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
            chunk.setTitle(title);
            chunk.setEmbedding(vector);
            chunk.setEmbeddingString(vectorString);
            chunk.setCreateTime(now);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            chunk.setMetadata(metadata);

            chunkEntities.add(chunk);

            // 构建 embedding 实体
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

        chunkMapper.insertBatch(chunkEntities);
        embeddingMapper.insertBatch(embeddings);
    }

    private void embedTitleForDocument(Long documentId, Long kbId, String title) {
        try {
            float[] titleVector = embeddingModel.embed(title).content().vector();
            String vectorString = Arrays.toString(titleVector);

            KmEmbedding titleEmbedding = new KmEmbedding();
            titleEmbedding.setId(IdUtil.getSnowflakeNextId());
            titleEmbedding.setKbId(kbId);
            titleEmbedding.setSourceId(documentId);
            titleEmbedding.setSourceType(KmEmbedding.SourceType.TITLE);
            titleEmbedding.setEmbedding(titleVector);
            titleEmbedding.setEmbeddingString(vectorString);
            titleEmbedding.setTextContent(title);
            titleEmbedding.setCreateTime(LocalDateTime.now());

            embeddingMapper.insert(titleEmbedding);

            log.info("Title vectorized for online doc: {}, title: {}", documentId, title);
        } catch (Exception e) {
            log.error("Failed to vectorize title for document: {}", documentId, e);
        }
    }
}
