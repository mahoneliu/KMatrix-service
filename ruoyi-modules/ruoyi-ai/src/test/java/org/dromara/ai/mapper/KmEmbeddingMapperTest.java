package org.dromara.ai.mapper;

import org.dromara.ai.BaseContainersTest;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class KmEmbeddingMapperTest extends BaseContainersTest {

    @Autowired
    private KmEmbeddingMapper embeddingMapper;

    @Autowired
    private KmDocumentMapper documentMapper;

    @Autowired
    private KmDocumentChunkMapper chunkMapper;

    private Long kbId = 1L;

    @BeforeEach
    void setUpData() {
        // Create sample document
        KmDocument document = new KmDocument();
        document.setKbId(kbId);
        document.setOriginalFilename("test_doc.txt");
        document.setTitle("Test Document");
        documentMapper.insert(document);

        // Create sample chunk
        KmDocumentChunk chunk = new KmDocumentChunk();
        chunk.setKbId(kbId);
        chunk.setDocumentId(document.getId());
        chunk.setContent("这是一个关于科亿知识库的测试文档内容。它支持大模型和向量检索。");
        chunk.setChunkType(KmDocumentChunk.ChunkType.STANDALONE);
        chunkMapper.insert(chunk);

        // Create embedding for the chunk
        KmEmbedding embedding = new KmEmbedding();
        embedding.setKbId(kbId);
        embedding.setSourceId(chunk.getId());
        embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
        embedding.setTextContent(chunk.getContent());
        embedding.setEmbedding(new float[]{0.1f, 0.2f}); // Mock vector
        embedding.setEmbeddingString("[0.1, 0.2]");
        embedding.setCreateTime(LocalDateTime.now());
        embeddingMapper.insertOne(embedding);
    }

    @Test
    void keywordSearch_HighRecall_ShouldFindContentWithSingleCharMatch() {
        // Test high recall: even if we search for a specific character, it should find it 
        // because of the (.). -> \1 pattern used in the mapper
        List<Map<String, Object>> results = embeddingMapper.keywordSearch("科亿", Collections.singletonList(kbId), 10);

        assertThat(results).isNotEmpty();
        Map<String, Object> firstResult = results.get(0);
        assertThat(firstResult.get("content").toString()).contains("科亿");
        assertThat(firstResult.get("highlight").toString()).contains("<mark>");
    }

    @Test
    void keywordSearch_NoMatch_ShouldReturnEmpty() {
        List<Map<String, Object>> results = embeddingMapper.keywordSearch("不存在的内容", Collections.singletonList(kbId), 10);
        assertThat(results).isEmpty();
    }
}
