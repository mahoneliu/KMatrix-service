package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.bo.ChunkResult;
import org.dromara.ai.service.IKmChunkingConfigService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线文档 ETL 处理器
 * 处理用户通过富文本编辑器输入的在线文档
 * 
 * 职责简化：仅负责内容解析和分块，不包含向量化逻辑
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineDocEtlHandler implements EtlHandler {

    private final ChildChunkSplitter childChunkSplitter;
    private final IKmChunkingConfigService chunkingConfigService;

    @Override
    public String getProcessType() {
        return DatasetProcessType.ONLINE_DOC;
    }

    @Override
    public List<ChunkResult> process(KmDocument document, KmDataset dataset) {
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

        // 4. 获取父块分块配置
        int chunkSize = dataset.getMaxChunkSize() != null ? dataset.getMaxChunkSize() : 500;
        int overlap = dataset.getChunkOverlap() != null ? dataset.getChunkOverlap() : 50;

        // 5. 父块分块
        List<String> parentChunks = splitText(plainText, chunkSize, overlap);
        if (CollUtil.isEmpty(parentChunks)) {
            throw new RuntimeException("分块失败");
        }

        // 6. 子块配置
        int childChunkSize = chunkingConfigService.getChildChunkSize(dataset);
        int childOverlap = chunkingConfigService.getChildChunkOverlap(dataset);

        // 7. 构建父块+子块结果列表
        List<ChunkResult> results = new ArrayList<>();
        for (int i = 0; i < parentChunks.size(); i++) {
            String parentText = parentChunks.get(i);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", parentChunks.size());
            metadata.put("documentTitle", title);

            List<String> childTexts = childChunkSplitter.split(parentText, childChunkSize, childOverlap);

            if (childTexts.isEmpty()) {
                results.add(ChunkResult.builder()
                        .content(parentText).title(title).metadata(metadata)
                        .chunkType(KmDocumentChunk.ChunkType.STANDALONE).build());
            } else {
                List<ChunkResult> children = new ArrayList<>(childTexts.size());
                for (int j = 0; j < childTexts.size(); j++) {
                    Map<String, Object> childMeta = new HashMap<>(metadata);
                    childMeta.put("childIndex", j);
                    children.add(ChunkResult.builder()
                            .content(childTexts.get(j)).title(title).metadata(childMeta)
                            .chunkType(KmDocumentChunk.ChunkType.CHILD).build());
                }
                results.add(ChunkResult.builder()
                        .content(parentText).title(title).metadata(metadata)
                        .chunkType(KmDocumentChunk.ChunkType.PARENT).children(children).build());
            }
        }

        log.info("OnlineDocEtlHandler completed: documentId={}, parentChunks={}, title={}",
                document.getId(), results.size(), title);

        return results;
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
}
