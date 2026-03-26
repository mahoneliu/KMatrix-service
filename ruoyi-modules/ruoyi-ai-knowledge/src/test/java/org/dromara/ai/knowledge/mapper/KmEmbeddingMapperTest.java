package org.dromara.ai.knowledge.mapper;

import org.dromara.ai.knowledge.BaseContainersTest;
import org.dromara.ai.knowledge.TestApplication;
import org.dromara.ai.knowledge.domain.KmEmbedding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
@ActiveProfiles("test")
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = "mybatis-plus.mapperPackage=org.dromara.ai.knowledge.mapper")
public class KmEmbeddingMapperTest extends BaseContainersTest {

    @Autowired
    private KmEmbeddingMapper kmEmbeddingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupSchema() {
        // 核心扩展
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        
        // 基础表结构（最小化实现，仅用于测试 Mapper SQL）
        jdbcTemplate.execute("DROP TABLE IF EXISTS km_embedding CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS km_document_chunk CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS km_document CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS km_question_chunk_map CASCADE");

        jdbcTemplate.execute("CREATE TABLE km_document (id BIGINT PRIMARY KEY, original_filename VARCHAR(255))");
        jdbcTemplate.execute("CREATE TABLE km_document_chunk (id BIGINT PRIMARY KEY, document_id BIGINT, parent_id BIGINT, content TEXT, title TEXT, metadata JSONB)");
        jdbcTemplate.execute("CREATE TABLE km_embedding (" +
                "id BIGINT PRIMARY KEY, " +
                "kb_id BIGINT, " +
                "source_id BIGINT, " +
                "source_type INT, " +
                "embedding vector(3), " +
                "text_content TEXT, " +
                "create_time TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE TABLE km_question_chunk_map (id BIGINT PRIMARY KEY, question_id BIGINT, chunk_id BIGINT)");
    }

    @Test
    void testInsertAndVectorSearch() {
        // 1. 准备基础数据
        jdbcTemplate.execute("INSERT INTO km_document (id, original_filename) VALUES (1, 'test.pdf')");
        jdbcTemplate.execute("INSERT INTO km_document_chunk (id, document_id, content) VALUES (100, 1, 'Hello World Content')");
        
        // 2. 插入向量
        KmEmbedding embedding = new KmEmbedding();
        embedding.setId(1L);
        embedding.setKbId(10L);
        embedding.setSourceId(100L);
        embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
        embedding.setEmbeddingString("[1.0, 1.0, 1.0]");
        embedding.setTextContent("Hello World");
        embedding.setCreateTime(LocalDateTime.now());

        int result = kmEmbeddingMapper.insertOne(embedding);
        assertEquals(1, result);

        // 3. 执行向量检索 (验证 SQL 语法和基础逻辑)
        List<Map<String, Object>> searchResults = kmEmbeddingMapper.vectorSearch(
                "[1.0, 1.0, 1.1]", 
                List.of(10L), 
                10, 
                0.0
        );

        // 4. 验证结果
        assertTrue(searchResults.size() > 0);
        assertEquals("Hello World Content", searchResults.get(0).get("content"));
        assertEquals("test.pdf", searchResults.get(0).get("document_name"));
    }
}
