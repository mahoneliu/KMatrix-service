package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.domain.KmQuestionChunkMap;

import java.util.List;

/**
 * 问题与分块关联Mapper接口
 *
 * @author Mahone
 * @date 2026-02-01
 */
public interface KmQuestionChunkMapMapper extends BaseMapper<KmQuestionChunkMap> {

    /**
     * 根据问题ID查询关联的分块ID列表
     */
    @Select("SELECT chunk_id FROM km_question_chunk_map WHERE question_id = #{questionId}")
    List<Long> selectChunkIdsByQuestionId(@Param("questionId") Long questionId);

    /**
     * 根据分块ID查询关联的问题ID列表
     */
    @Select("SELECT question_id FROM km_question_chunk_map WHERE chunk_id = #{chunkId}")
    List<Long> selectQuestionIdsByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 根据问题ID删除关联
     */
    @Delete("DELETE FROM km_question_chunk_map WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 根据分块ID删除关联
     */
    @Delete("DELETE FROM km_question_chunk_map WHERE chunk_id = #{chunkId}")
    int deleteByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 批量查询问题的分段数量
     * 
     * @param questionIds 问题ID列表
     * @return Map<问题ID, 分段数量>
     */
    @Select("<script>" +
            "SELECT question_id, COUNT(*) as count " +
            "FROM km_question_chunk_map " +
            "WHERE question_id IN " +
            "<foreach collection='questionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "GROUP BY question_id" +
            "</script>")
    @MapKey("question_id")
    Map<Long, Map<String, Object>> countByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /**
     * 根据分块ID列表查询关联记录ID列表
     * 用于删除分块时获取对应的embedding记录
     */
    @Select("<script>" +
            "SELECT id FROM km_question_chunk_map " +
            "WHERE chunk_id IN " +
            "<foreach collection='chunkIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Long> selectIdsByChunkIds(@Param("chunkIds") List<Long> chunkIds);

    /**
     * 根据关联记录ID列表批量查询
     * 用于检索时获取完整的关联信息
     */
    @Select("<script>" +
            "SELECT id, question_id, chunk_id FROM km_question_chunk_map " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<KmQuestionChunkMap> selectMapsByIds(@Param("ids") List<Long> ids);

    /**
     * 根据问题ID列表查询关联记录ID列表
     * 用于删除问题时获取对应的embedding记录
     */
    @Select("<script>" +
            "SELECT id FROM km_question_chunk_map " +
            "WHERE question_id IN " +
            "<foreach collection='questionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Long> selectIdsByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
