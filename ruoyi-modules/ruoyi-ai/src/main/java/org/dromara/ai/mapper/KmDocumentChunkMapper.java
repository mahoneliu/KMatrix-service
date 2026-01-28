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
         * 关键词全文检索 (PostgreSQL ts_vector)
         * 使用 plainto_tsquery 进行简单分词搜索
         */
        @Select("<script>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata, " +
                        "       ts_rank(to_tsvector('simple', c.content), plainto_tsquery('simple', #{query})) as score "
                        +
                        "FROM km_document_chunk c " +
                        "JOIN km_document d ON c.document_id = d.id " +
                        "<where>" +
                        "  to_tsvector('simple', c.content) @@ plainto_tsquery('simple', #{query}) " +
                        "  <if test='datasetIds != null and datasetIds.size() > 0'>" +
                        "    AND d.dataset_id IN " +
                        "    <foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<Map<String, Object>> keywordSearch(
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
