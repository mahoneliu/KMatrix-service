package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.bo.ChunkResult;
import org.dromara.ai.domain.enums.FileStoreType;
import org.dromara.ai.service.IKmChunkingConfigService;
import org.dromara.ai.service.ILocalFileService;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用文件 ETL 处理器
 * 处理 PDF、Office、纯文本类文件
 * 
 * 职责简化：仅负责文件解析和分块，不包含向量化逻辑
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericFileEtlHandler implements EtlHandler {

    private final ISysOssService ossService;
    private final ILocalFileService localFileService;
    private final ChildChunkSplitter childChunkSplitter;
    private final IKmChunkingConfigService chunkingConfigService;

    private final DocumentParser documentParser = new ApacheTikaDocumentParser();

    @Override
    public String getProcessType() {
        return DatasetProcessType.GENERIC_FILE;
    }

    @Override
    public List<ChunkResult> process(KmDocument document, KmDataset dataset) {
        log.info("GenericFileEtlHandler processing document: {}", document.getId());

        // 1. 解析文档并提取 title
        ParseResult parseResult = parseDocumentWithTitle(document);
        String content = parseResult.content;
        String title = parseResult.title;

        if (content == null || content.isBlank()) {
            throw new RuntimeException("文档内容为空");
        }

        // 2. 获取父块分块配置 (优先从实体字段读取，兼容旧 config JSON)
        int chunkSize = dataset.getMaxChunkSize() != null ? dataset.getMaxChunkSize()
                : getConfigInt(dataset, "chunkSize", 500);
        int overlap = dataset.getChunkOverlap() != null ? dataset.getChunkOverlap()
                : getConfigInt(dataset, "overlap", 50);

        // 3. 父块分块
        List<String> parentChunks = splitText(content, chunkSize, overlap);
        if (CollUtil.isEmpty(parentChunks)) {
            throw new RuntimeException("分块失败");
        }

        // 4. 获取子块分块配置
        int childChunkSize = chunkingConfigService.getChildChunkSize(dataset);
        int childOverlap = chunkingConfigService.getChildChunkOverlap(dataset);

        // 5. 构建父块+子块结果列表
        List<ChunkResult> results = new ArrayList<>();
        for (int i = 0; i < parentChunks.size(); i++) {
            String parentText = parentChunks.get(i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", parentChunks.size());
            metadata.put("documentTitle", title);

            // 对父块执行子块分割
            List<String> childTexts = childChunkSplitter.split(parentText, childChunkSize, childOverlap);

            if (childTexts.isEmpty()) {
                // 父块内容较短，标记为 STANDALONE
                results.add(ChunkResult.builder()
                        .content(parentText)
                        .title(title)
                        .metadata(metadata)
                        .chunkType(KmDocumentChunk.ChunkType.STANDALONE)
                        .build());
            } else {
                // 构建子块列表
                List<ChunkResult> children = new ArrayList<>(childTexts.size());
                for (int j = 0; j < childTexts.size(); j++) {
                    Map<String, Object> childMeta = new HashMap<>(metadata);
                    childMeta.put("childIndex", j);
                    children.add(ChunkResult.builder()
                            .content(childTexts.get(j))
                            .title(title)
                            .metadata(childMeta)
                            .chunkType(KmDocumentChunk.ChunkType.CHILD)
                            .build());
                }
                // 父块标记为 PARENT
                results.add(ChunkResult.builder()
                        .content(parentText)
                        .title(title)
                        .metadata(metadata)
                        .chunkType(KmDocumentChunk.ChunkType.PARENT)
                        .children(children)
                        .build());
            }
        }

        log.info("GenericFileEtlHandler completed: documentId={}, parentChunks={}, title={}",
                document.getId(), results.size(), title);

        return results;
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
            // 根据存储类型获取文件流
            InputStream is = getDocumentInputStream(document);

            try (is) {
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

    /**
     * 根据文档存储类型获取输入流
     */
    private InputStream getDocumentInputStream(KmDocument document) throws IOException {
        FileStoreType storeType = FileStoreType.fromValue(document.getStoreType());

        if (storeType.isOss()) {
            // 从 OSS 读取
            Long ossId = document.getOssId();
            if (ossId == null) {
                throw new RuntimeException("Document OSS ID is null");
            }

            SysOssVo ossVo = ossService.getById(ossId);
            if (ossVo == null) {
                throw new RuntimeException("OSS file not found: " + ossId);
            }

            OssClient storage = OssFactory.instance(ossVo.getService());
            return storage.getObjectContent(ossVo.getFileName());

        } else if (storeType.isLocal()) {
            // 从本地文件系统读取
            String filePath = document.getFilePath();
            if (filePath == null) {
                throw new RuntimeException("Document file path is null");
            }

            return localFileService.getFileStream(filePath);

        } else {
            throw new RuntimeException("Unsupported store type: " + storeType);
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
