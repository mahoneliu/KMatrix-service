package org.dromara.ai.service.etl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 子块分割工具组件
 * 封装子块二次分割逻辑，基于 LangChain4j DocumentSplitters.recursive 实现。
 * 各 ETL Handler 和 EmbeddingService 统一调用此组件，避免重复实现。
 *
 * @author Mahone
 * @date 2026-02-27
 */
@Slf4j
@Component
public class ChildChunkSplitter {

    /**
     * 对父块内容执行子块分割
     *
     * @param parentContent  父块文本内容
     * @param childChunkSize 子块大小（字符数）
     * @param childOverlap   子块重叠大小（字符数）
     * @return 子块文本列表；若父块内容不超过 childChunkSize，返回空列表表示无需生成子块
     */
    public List<String> split(String parentContent, int childChunkSize, int childOverlap) {
        if (parentContent == null || parentContent.isBlank()) {
            return List.of();
        }

        // 父块内容不超过子块大小时，不生成子块（当前块应标记为 STANDALONE）
        if (parentContent.length() <= childChunkSize) {
            return List.of();
        }

        var splitter = DocumentSplitters.recursive(childChunkSize, childOverlap);
        Document doc = Document.from(parentContent);
        List<TextSegment> segments = splitter.split(doc);

        List<String> result = new ArrayList<>(segments.size());
        for (TextSegment segment : segments) {
            String text = segment.text();
            if (text != null && !text.isBlank()) {
                result.add(text);
            }
        }

        log.debug("ChildChunkSplitter: parentLen={}, childChunkSize={}, overlap={}, childCount={}",
                parentContent.length(), childChunkSize, childOverlap, result.size());

        return result;
    }
}
