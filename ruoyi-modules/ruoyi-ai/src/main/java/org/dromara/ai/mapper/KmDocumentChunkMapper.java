package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.domain.KmDocumentChunk;

import java.util.List;
import java.util.Map;

/**
 * 文档切片Mapper接口
 * 注意: 由于含有 pgvector 类型字段，需要自定义 SQL
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface KmDocumentChunkMapper extends BaseMapper<KmDocumentChunk> {

        /**
         * 批量插入切片 (含向量)
         */
        @Insert("<script>" +
                        "INSERT INTO km_document_chunk (id, document_id, content, metadata, embedding, create_time) VALUES "
                        +
                        "<foreach collection='chunks' item='chunk' separator=','>" +
                        "(#{chunk.id}, #{chunk.documentId}, #{chunk.content}, #{chunk.metadata, typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}::jsonb, "
                        +
                        "#{chunk.embeddingString}::vector, #{chunk.createTime})"
                        +
                        "</foreach>" +
                        "</script>")
        int insertBatch(@Param("chunks") List<KmDocumentChunk> chunks);

        /**
         * 根据文档ID删除所有切片
         */
        @Delete("DELETE FROM km_document_chunk WHERE document_id = #{documentId}")
        int deleteByDocumentId(@Param("documentId") Long documentId);

        /**
         * 根据文档ID查询切片数量
         */
        @Select("SELECT COUNT(*) FROM km_document_chunk WHERE document_id = #{documentId}")
        int countByDocumentId(@Param("documentId") Long documentId);

        /**
         * 向量相似度检索 (余弦距离)
         * 返回: chunk_id, document_id, content, metadata, score (1 - cosine_distance)
         */
        @Select("<script>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata, " +
                        "       (1 - (c.embedding &lt;=&gt; #{queryVector}::vector)) as score " +
                        "FROM km_document_chunk c " +
                        "JOIN km_document d ON c.document_id = d.id " +
                        "<where>" +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND d.dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY c.embedding &lt;=&gt; #{queryVector}::vector " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<Map<String, Object>> vectorSearch(
                        @Param("queryVector") String queryVector,
                        @Param("datasetIds") List<Long> datasetIds,
                        @Param("topK") int topK);

        /**
         * 关键词全文检索 (支持 MySQL/PG)
         */
        @Select("<script>" +
                        "<if test='_databaseId == \"mysql\"'>" +
                        "SELECT id as chunk_id, document_id, content, metadata, " +
                        "       MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) as score " +
                        "FROM km_document_chunk " +
                        "WHERE MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) " +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND document_id IN (SELECT id FROM km_document WHERE dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "    )" +
                        "  </if>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</if>" +
                        "<if test='_databaseId != \"mysql\"'>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata, " +
                        "       ts_rank(c.content_search_vector, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|'))) as score "
                        +
                        "FROM km_document_chunk c " +
                        "JOIN km_document d ON c.document_id = d.id " +
                        "<where>" +
                        "  c.content_search_vector @@ to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')) "
                        +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND d.dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</if>" +
                        "</script>")
        List<Map<String, Object>> keywordSearch(
                        @Param("query") String query,
                        @Param("datasetIds") List<Long> datasetIds,
                        @Param("topK") int topK);

        /**
         * 关键词全文检索 (带高亮)
         * MySQL: 不支持自动高亮，降级为普通检索
         * PG: 使用 ts_headline
         */
        @Select("<script>" +
                        "<if test='_databaseId == \"mysql\"'>" +
                        "SELECT id as chunk_id, document_id, content, metadata, " +
                        "       MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) as score, " +
                        "       NULL as highlight " +
                        "FROM km_document_chunk " +
                        "WHERE MATCH(content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) " +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND document_id IN (SELECT id FROM km_document WHERE dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "    )" +
                        "  </if>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</if>" +
                        "<if test='_databaseId != \"mysql\"'>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata, " +
                        "       ts_rank(c.content_search_vector, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|'))) as score, "
                        +
                        "       ts_headline('jiebacfg', c.content, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')), "
                        +
                        "                   'StartSel=&lt;mark&gt;, StopSel=&lt;/mark&gt;, MaxWords=80, MinWords=30') as highlight "
                        +
                        "FROM km_document_chunk c " +
                        "JOIN km_document d ON c.document_id = d.id " +
                        "<where>" +
                        "  c.content_search_vector @@ to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')) "
                        +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND d.dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</if>" +
                        "</script>")
        List<Map<String, Object>> keywordSearchWithHighlight(
                        @Param("query") String query,
                        @Param("datasetIds") List<Long> datasetIds,
                        @Param("topK") int topK);

        /**
         * 根据ID列表查询切片
         */
        @Select("<script>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata " +
                        "FROM km_document_chunk c " +
                        "WHERE c.id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "  #{id}" +
                        "</foreach>" +
                        "</script>")
        List<Map<String, Object>> selectByIds(@Param("ids") List<Long> ids);
}
