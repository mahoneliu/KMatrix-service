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
        @Insert("INSERT INTO km_embedding (kb_id, source_id, source_type, embedding, text_content, create_time) " +
                        "VALUES (#{kbId}, #{sourceId}, #{sourceType}, #{embeddingString}::vector, #{textContent}, #{createTime})")
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
         * 多源向量相似度检索 (余弦距离)
         * 查询 km_embedding 表，匹配范围包括所有 SourceType
         * 使用 DISTINCT ON (source_id) 去重，每个源仅返回得分最高的一条
         *
         * @param queryVector 查询向量 (字符串格式)
         * @param kbIds       知识库ID列表
         * @param sourceTypes 源类型列表 (0=QUESTION, 1=CONTENT, 2=TITLE)
         * @param topK        返回数量
         * @param threshold   相似度阈值 (0-1)
         * @return 检索结果列表
         */
        @Select("<script>" +
                        "SELECT DISTINCT ON (source_id) " +
                        "       id, kb_id, source_id, source_type, " +
                        "       (1 - (embedding &lt;=&gt; #{queryVector}::vector)) as score " +
                        "FROM km_embedding " +
                        "<where>" +
                        "  <if test='kbIds != null and kbIds.size() > 0'>" +
                        "    AND kb_id IN " +
                        "    <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "  <if test='sourceTypes != null and sourceTypes.size() > 0'>" +
                        "    AND source_type IN " +
                        "    <foreach collection='sourceTypes' item='type' open='(' separator=',' close=')'>" +
                        "      #{type}" +
                        "    </foreach>" +
                        "  </if>" +
                        "  <if test='threshold != null'>" +
                        "    AND (1 - (embedding &lt;=&gt; #{queryVector}::vector)) &gt;= #{threshold}" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY source_id, embedding &lt;=&gt; #{queryVector}::vector " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<Map<String, Object>> multiSourceVectorSearch(
                        @Param("queryVector") String queryVector,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("sourceTypes") List<Integer> sourceTypes,
                        @Param("topK") int topK,
                        @Param("threshold") Double threshold);

        /**
         * 向量相似度检索 (不使用 DISTINCT，用于获取最相似的条目)
         */
        @Select("<script>" +
                        "SELECT id, kb_id, source_id, source_type, " +
                        "       (1 - (embedding &lt;=&gt; #{queryVector}::vector)) as score " +
                        "FROM km_embedding " +
                        "<where>" +
                        "  <if test='kbIds != null and kbIds.size() > 0'>" +
                        "    AND kb_id IN " +
                        "    <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "  <if test='sourceTypes != null and sourceTypes.size() > 0'>" +
                        "    AND source_type IN " +
                        "    <foreach collection='sourceTypes' item='type' open='(' separator=',' close=')'>" +
                        "      #{type}" +
                        "    </foreach>" +
                        "  </if>" +
                        "  <if test='threshold != null'>" +
                        "    AND (1 - (embedding &lt;=&gt; #{queryVector}::vector)) &gt;= #{threshold}" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY embedding &lt;=&gt; #{queryVector}::vector " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<Map<String, Object>> vectorSearch(
                        @Param("queryVector") String queryVector,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("sourceTypes") List<Integer> sourceTypes,
                        @Param("topK") int topK,
                        @Param("threshold") Double threshold);

        /**
         * 统一关键词全文检索
         * 这里的 score 使用 ts_rank 计算
         */
        @Select("<script>" +
                        "SELECT id, kb_id, source_id, source_type, text_content as content, " +
                        "       ts_rank(search_vector, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|'))) as score, "
                        +
                        "       ts_headline('jiebacfg', text_content, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')), "
                        +
                        "                   'StartSel=&lt;mark&gt;, StopSel=&lt;/mark&gt;, MaxWords=80, MinWords=30') as highlight "
                        +
                        "FROM km_embedding " +
                        "<where>" +
                        "  search_vector @@ to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')) "
                        +
                        "  <if test='kbIds != null and kbIds.size() > 0'>" +
                        "    AND kb_id IN " +
                        "    <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "  <if test='sourceTypes != null and sourceTypes.size() > 0'>" +
                        "    AND source_type IN " +
                        "    <foreach collection='sourceTypes' item='type' open='(' separator=',' close=')'>" +
                        "      #{type}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<Map<String, Object>> keywordSearch(
                        @Param("query") String query,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("sourceTypes") List<Integer> sourceTypes,
                        @Param("topK") int topK);
}
