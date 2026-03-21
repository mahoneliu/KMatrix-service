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
                        "INSERT INTO km_document_chunk (id, document_id, kb_id, content, metadata, title, create_time, parent_id, chunk_type) VALUES "
                        +
                        "<foreach collection='chunks' item='chunk' separator=','>" +
                        "(#{chunk.id}, #{chunk.documentId}, #{chunk.kbId}, #{chunk.content}, #{chunk.metadata, typeHandler=org.dromara.common.mybatis.handler.JsonTypeHandler}::jsonb, "
                        +
                        "#{chunk.title}, #{chunk.createTime}, #{chunk.parentId}, #{chunk.chunkType})"
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
         * 根据切片ID列表批量查询切片
         */
        @Select("<script>" +
                        "SELECT id, document_id, content, metadata, title FROM km_document_chunk " +
                        "WHERE id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "  #{id}" +
                        "</foreach>" +
                        "</script>")
        List<Map<String, Object>> selectChunksByIds(@Param("ids") List<Long> ids);


        /**
         * 根据ID列表查询切片
         */
        @Select("<script>" +
                        "SELECT c.id as chunk_id, c.document_id, c.content, c.metadata, c.title " +
                        "FROM km_document_chunk c " +
                        "WHERE c.id IN " +
                        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
                        "  #{id}" +
                        "</foreach>" +
                        "</script>")
        List<Map<String, Object>> selectByIds(@Param("ids") List<Long> ids);

        /**
         * 根据分块ID列表查询分块及文档信息 (优化性能，避免N+1查询)
         * 
         * @param chunkIds 分块ID列表
         * @return 包含分块和文档信息的Map列表
         */
        @Select("<script>" +
                        "SELECT " +
                        "  c.id, c.title, c.content, c.document_id, " +
                        "  d.original_filename as document_title " +
                        "FROM km_document_chunk c " +
                        "LEFT JOIN km_document d ON c.document_id = d.id " +
                        "WHERE c.id IN " +
                        "<foreach collection='chunkIds' item='id' open='(' separator=',' close=')'>" +
                        "#{id}" +
                        "</foreach>" +
                        "</script>")
        List<Map<String, Object>> selectChunksWithDocument(@Param("chunkIds") List<Long> chunkIds);
}
