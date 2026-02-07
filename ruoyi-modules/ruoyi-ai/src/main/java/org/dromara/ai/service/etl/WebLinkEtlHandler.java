package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.bo.ChunkResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页链接 ETL 处理器
 * 爬取网页内容并进行分块处理
 * 
 * 职责简化：仅负责网页爬取和分块，不包含向量化逻辑
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebLinkEtlHandler implements EtlHandler {

    @Override
    public String getProcessType() {
        return DatasetProcessType.WEB_LINK;
    }

    @Override
    public List<ChunkResult> process(KmDocument document, KmDataset dataset) {
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
        List<String> textChunks = splitText(content, chunkSize, overlap);
        if (CollUtil.isEmpty(textChunks)) {
            throw new RuntimeException("分块失败");
        }

        // 5. 构建ChunkResult列表
        List<ChunkResult> results = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", textChunks.size());
            metadata.put("documentTitle", title);
            metadata.put("sourceUrl", url);

            results.add(ChunkResult.builder()
                    .content(textChunks.get(i))
                    .title(title)
                    .metadata(metadata)
                    .build());
        }

        log.info("WebLinkEtlHandler completed: documentId={}, url={}, chunks={}, title={}",
                document.getId(), url, results.size(), title);

        return results;
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
}
