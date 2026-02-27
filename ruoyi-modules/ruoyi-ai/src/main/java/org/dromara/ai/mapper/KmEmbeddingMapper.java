package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.domain.KmEmbedding;

import java.util.List;
import java.util.Map;

/**
 * 统一向量存储Mapper接口
 *
 * @author Mahone
 * @date 2026-02-01
 */
public interface KmEmbeddingMapper extends BaseMapper<KmEmbedding> {

        /**
         * 批量插入向量
         */
        /**
         * 批量插入向量
         */
        @Insert("<script>" +
                        "INSERT INTO km_embedding (id, kb_id, source_id, source_type, embedding, text_content, create_time) VALUES "
                        +
                        "<foreach collection='embeddings' item='e' separator=','>" +
                        "(#{e.id}, #{e.kbId}, #{e.sourceId}, #{e.sourceType}, #{e.embeddingString}::vector, #{e.textContent}, #{e.createTime})"
                        +
                        "</foreach>" +
                        "</script>")
        int insertBatch(@Param("embeddings") List<KmEmbedding> embeddings);

        /**
         * 单条插入向量
         */
        @Insert("INSERT INTO km_embedding (id, kb_id, source_id, source_type, embedding, text_content, create_time) " +
                        "VALUES (#{id}, #{kbId}, #{sourceId}, #{sourceType}, #{embeddingString}::vector, #{textContent}, #{createTime})")
        int insertOne(KmEmbedding embedding);

        /**
         * 根据源ID和源类型删除向量
         */
        @Delete("DELETE FROM km_embedding WHERE source_id = #{sourceId} AND source_type = #{sourceType}")
        int deleteBySource(@Param("sourceId") Long sourceId, @Param("sourceType") Integer sourceType);

        /**
         * 根据知识库ID删除所有向量
         */
        @Delete("DELETE FROM km_embedding WHERE kb_id = #{kbId}")
        int deleteByKbId(@Param("kbId") Long kbId);

        /**
         * 多表关联向量检索 (一次性获取所有数据)
         * 使用 CTE 和 JOIN 优化性能，避免 N+1 查询问题
         * 注：始终查询所有源类型 (CONTENT, QUESTION, TITLE)，无需额外筛选参数
         * 
         * @param queryVector 查询向量字符串
         * @param kbIds       知识库ID列表
         * @param topK        返回数量
         * @param threshold   相似度阈值
         * @return 包含 chunk、document、question 信息的完整结果
         */
        @Select("<script>" +
        // Step 1: 向量检索获取基础匹配结果
                        "WITH base_matches AS ( " +
                        "  SELECT " +
                        "    id, kb_id, source_id, source_type, " +
                        "    (1 - (embedding &lt;=&gt; #{queryVector}::vector)) as score " +
                        "  FROM km_embedding " +
                        "  <where>" +
                        "    <if test='kbIds != null and kbIds.size() > 0'>" +
                        "      AND kb_id IN " +
                        "      <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "        #{id}" +
                        "      </foreach>" +
                        "    </if>" +
                        "    <if test='threshold != null'>" +
                        "      AND (1 - (embedding &lt;=&gt; #{queryVector}::vector)) &gt;= #{threshold}" +
                        "    </if>" +
                        "  </where>" +
                        "  ORDER BY embedding &lt;=&gt; #{queryVector}::vector " +
                        "  LIMIT #{topK} " +
                        "), " +
                        // Step 2: 一次性 JOIN 所有需要的表，使用 CASE WHEN 处理不同类型
                        "title_first_chunks AS ( " +
                        "  SELECT DISTINCT ON (bm.source_id) " +
                        "    bm.source_id, dc.id as first_chunk_id " +
                        "  FROM base_matches bm " +
                        "  JOIN km_document_chunk dc ON bm.source_id = dc.document_id " +
                        "  WHERE bm.source_type = 2 " +
                        "  ORDER BY bm.source_id, dc.id ASC " +
                        "), " +
                        "enriched_matches AS ( " +
                        "  SELECT " +
                        "    bm.score, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN qcm.chunk_id " +
                        "      WHEN bm.source_type = 1 THEN bm.source_id " +
                        "      WHEN bm.source_type = 2 THEN tfc.first_chunk_id " +
                        "      WHEN bm.source_type = 3 THEN bm.source_id " +
                        "    END as chunk_id, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN qcm.question_id " +
                        "      ELSE NULL " +
                        "    END as question_id, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN 'QUESTION' " +
                        "      WHEN bm.source_type = 1 THEN 'CONTENT' " +
                        "      WHEN bm.source_type = 2 THEN 'TITLE' " +
                        "      WHEN bm.source_type = 3 THEN 'CONTENT' " +
                        "    END as source_type_label " +
                        "  FROM base_matches bm " +
                        "  LEFT JOIN km_question_chunk_map qcm ON bm.source_type = 0 AND bm.source_id = qcm.id " +
                        "  LEFT JOIN title_first_chunks tfc ON bm.source_type = 2 AND bm.source_id = tfc.source_id " +
                        ") " +
                        // Step 3: 最终查询，关联 chunk 和 document 数据，处理父子分块向上溯源
                        "SELECT " +
                        "  em.chunk_id, " +
                        "  em.score, " +
                        "  em.source_type_label, " +
                        "  em.question_id, " +
                        "  COALESCE(parent.content, dc.content) as content, " +
                        "  COALESCE(parent.title, dc.title, d.original_filename) as chunk_title, " +
                        "  COALESCE(parent.metadata, dc.metadata) as metadata, " +
                        "  dc.document_id, " +
                        "  d.original_filename as document_name " +
                        "FROM enriched_matches em " +
                        "JOIN km_document_chunk dc ON em.chunk_id = dc.id " +
                        "LEFT JOIN km_document_chunk parent ON dc.parent_id = parent.id " +
                        "JOIN km_document d ON dc.document_id = d.id " +
                        "ORDER BY em.score DESC " +
                        "</script>")
        List<Map<String, Object>> vectorSearch(
                        @Param("queryVector") String queryVector,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("topK") int topK,
                        @Param("threshold") Double threshold);

        /**
         * 多表关联关键词检索 (一次性获取所有数据)
         * 使用 CTE 和 JOIN 优化性能，避免 N+1 查询问题
         * 注：始终查询所有源类型 (CONTENT, QUESTION, TITLE)，无需额外筛选参数
         * 
         * @param query 查询文本
         * @param kbIds 知识库ID列表
         * @param topK  返回数量
         * @return 包含 chunk、document、question 信息的完整结果
         */
        @Select("<script>" +
        // Step 1: 关键词检索获取基础匹配结果
                        "WITH base_matches AS ( " +
                        "  SELECT " +
                        "    id, kb_id, source_id, source_type, " +
                        "    ts_rank(search_vector, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|'))) as score, "
                        +
                        "    ts_headline('jiebacfg', text_content, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')), "
                        +
                        "      'StartSel=&lt;mark&gt;, StopSel=&lt;/mark&gt;, MaxWords=80, MinWords=30') as highlight "
                        +
                        "  FROM km_embedding " +
                        "  <where>" +
                        "    search_vector @@ to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')) "
                        +
                        "    <if test='kbIds != null and kbIds.size() > 0'>" +
                        "      AND kb_id IN " +
                        "      <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "        #{id}" +
                        "      </foreach>" +
                        "    </if>" +

                        "  </where>" +
                        "  ORDER BY score DESC " +
                        "  LIMIT #{topK} " +
                        "), " +
                        // Step 2: 一次性 JOIN 所有需要的表,使用 CASE WHEN 处理不同类型
                        "title_first_chunks AS ( " +
                        "  SELECT DISTINCT ON (bm.source_id) " +
                        "    bm.source_id, dc.id as first_chunk_id " +
                        "  FROM base_matches bm " +
                        "  JOIN km_document_chunk dc ON bm.source_id = dc.document_id " +
                        "  WHERE bm.source_type = 2 " +
                        "  ORDER BY bm.source_id, dc.id ASC " +
                        "), " +
                        "enriched_matches AS ( " +
                        "  SELECT " +
                        "    bm.score, " +
                        "    bm.highlight, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN qcm.chunk_id " +
                        "      WHEN bm.source_type = 1 THEN bm.source_id " +
                        "      WHEN bm.source_type = 2 THEN tfc.first_chunk_id " +
                        "      WHEN bm.source_type = 3 THEN bm.source_id " +
                        "    END as chunk_id, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN qcm.question_id " +
                        "      ELSE NULL " +
                        "    END as question_id, " +
                        "    CASE " +
                        "      WHEN bm.source_type = 0 THEN 'QUESTION' " +
                        "      WHEN bm.source_type = 1 THEN 'CONTENT' " +
                        "      WHEN bm.source_type = 2 THEN 'TITLE' " +
                        "      WHEN bm.source_type = 3 THEN 'CONTENT' " +
                        "    END as source_type_label " +
                        "  FROM base_matches bm " +
                        "  LEFT JOIN km_question_chunk_map qcm ON bm.source_type = 0 AND bm.source_id = qcm.id " +
                        "  LEFT JOIN title_first_chunks tfc ON bm.source_type = 2 AND bm.source_id = tfc.source_id " +
                        ") " +
                        // Step 3: 最终查询,关联 chunk 和 document 数据，处理父子分块向上溯源
                        "SELECT " +
                        "  em.chunk_id, " +
                        "  em.score, " +
                        "  em.source_type_label, " +
                        "  em.question_id, " +
                        "  em.highlight, " +
                        "  COALESCE(parent.content, dc.content) as content, " +
                        "  COALESCE(parent.title, dc.title, d.original_filename) as chunk_title, " +
                        "  COALESCE(parent.metadata, dc.metadata) as metadata, " +
                        "  dc.document_id, " +
                        "  d.original_filename as document_name " +
                        "FROM enriched_matches em " +
                        "JOIN km_document_chunk dc ON em.chunk_id = dc.id " +
                        "LEFT JOIN km_document_chunk parent ON dc.parent_id = parent.id " +
                        "JOIN km_document d ON dc.document_id = d.id " +
                        "ORDER BY em.score DESC " +
                        "</script>")
        List<Map<String, Object>> keywordSearch(
                        @Param("query") String query,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("topK") int topK);
}
