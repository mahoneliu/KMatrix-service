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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页链接 ETL 处理器
 * 爬取网页内容并进行向量化处理
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebLinkEtlHandler implements EtlHandler {

    private final EmbeddingModel embeddingModel;
    private final KmDocumentChunkMapper chunkMapper;
    private final KmEmbeddingMapper embeddingMapper;

    @Override
    public String getProcessType() {
        return DatasetProcessType.WEB_LINK;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(KmDocument document, KmDataset dataset, Long kbId) {
        log.info("WebLinkEtlHandler processing document: {}", document.getId());

        // 1. 从 document.url 获取网页链接
        String url = document.getUrl();
        if (url == null || url.isBlank()) {
            throw new RuntimeException("网页链接为空");
        }

        // 2. 爬取网页内容
        WebPageContent webContent = crawlWebPage(url);
        String content = webContent.content;
        String title = webContent.title;

        if (content == null || content.isBlank()) {
            throw new RuntimeException("网页内容为空");
        }

        // 3. 获取分块配置
        int chunkSize = dataset.getMaxChunkSize() != null ? dataset.getMaxChunkSize() : 500;
        int overlap = dataset.getChunkOverlap() != null ? dataset.getChunkOverlap() : 50;

        // 4. 分块
        List<String> chunks = splitText(content, chunkSize, overlap);
        if (CollUtil.isEmpty(chunks)) {
            throw new RuntimeException("分块失败");
        }

        // 5. 向量化并存储
        embedAndStore(document.getId(), kbId, chunks, title);

        // 6. 为文档 title 生成向量
        if (title != null && !title.isBlank()) {
            embedTitleForDocument(document.getId(), kbId, title);
        }

        log.info("WebLinkEtlHandler completed: documentId={}, url={}, chunks={}, title={}",
                document.getId(), url, chunks.size(), title);
    }

    /**
     * 网页内容
     */
    private static class WebPageContent {
        String title;
        String content;

        WebPageContent(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    /**
     * 爬取网页内容
     */
    private WebPageContent crawlWebPage(String url) {
        try {
            log.info("Crawling web page: {}", url);

            // 使用 Jsoup 爬取网页
            org.jsoup.nodes.Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 提取 title
            String title = doc.title();
            if (title == null || title.isBlank()) {
                title = url;
            }

            // 提取正文内容 (移除 script, style 等标签)
            doc.select("script, style, nav, footer, header").remove();
            Element body = doc.body();
            String content = body != null ? body.text() : "";

            return new WebPageContent(title, content);
        } catch (IOException e) {
            log.error("Failed to crawl web page: {}", url, e);
            throw new RuntimeException("网页爬取失败: " + e.getMessage());
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

            float[] vector = embeddingModel.embed(chunkText).content().vector();
            String vectorString = Arrays.toString(vector);

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

            log.info("Title vectorized for web link: {}, title: {}", documentId, title);
        } catch (Exception e) {
            log.error("Failed to vectorize title for document: {}", documentId, e);
        }
    }
}
