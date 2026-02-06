package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.ai.domain.vo.KmQuestionVo;

import java.util.List;

/**
 * 问题Mapper接口
 *
 * @author Mahone
 * @date 2026-02-01
 */
public interface KmQuestionMapper extends BaseMapper<KmQuestion> {

        /**
         * 根据知识库ID查询问题列表
         */
        @Select("SELECT * FROM km_question WHERE kb_id = #{kbId} AND del_flag = '0' ORDER BY create_time DESC")
        List<KmQuestion> selectByKbId(@Param("kbId") Long kbId);

        /**
         * 根据知识库ID删除所有问题 (逻辑删除)
         */
        @Delete("UPDATE km_question SET del_flag = '1' WHERE kb_id = #{kbId}")
        int deleteByKbId(@Param("kbId") Long kbId);

        /**
         * 更新命中次数
         */
        @org.apache.ibatis.annotations.Update("UPDATE km_question SET hit_num = hit_num + 1 WHERE id = #{id}")
        int incrementHitNum(@Param("id") Long id);

        /**
         * 关键词全文检索 (支持 PostgreSQL)
         */
        @Select("<script>" +
                        "SELECT id as question_id, kb_id, content, hit_num, " +
                        "       ts_rank(content_search_vector, to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|'))) as score "
                        +
                        "FROM km_question " +
                        "<where>" +
                        "  content_search_vector @@ to_tsquery('jiebacfg', replace(plainto_tsquery('jiebacfg', #{query}::text)::text, '&amp;', '|')) "
                        +
                        "  AND del_flag = '0' " +
                        "  <if test='kbIds != null and kbIds.size() > 0'>" +
                        "    AND kb_id IN " +
                        "    <foreach collection='kbIds' item='id' open='(' separator=',' close=')'>" +
                        "      #{id}" +
                        "    </foreach>" +
                        "  </if>" +
                        "</where>" +
                        "ORDER BY score DESC " +
                        "LIMIT #{topK}" +
                        "</script>")
        List<java.util.Map<String, Object>> keywordSearch(
                        @Param("query") String query,
                        @Param("kbIds") List<Long> kbIds,
                        @Param("topK") int topK);

        /**
         * 分页查询问题列表（含分段数量）- 单次JOIN查询优化
         * 
         * @param kbId    知识库ID（可选）
         * @param content 问题内容（模糊匹配，可选）
         * @param offset  偏移量
         * @param limit   每页数量
         * @return 包含问题信息和分段数量的Map列表
         */
        @Select("<script>" +
                        "SELECT " +
                        "  q.id, q.kb_id, q.content, q.hit_num, q.source_type, q.create_time, q.update_time, " +
                        "  COALESCE(COUNT(m.chunk_id), 0) as chunk_count " +
                        "FROM km_question q " +
                        "LEFT JOIN km_question_chunk_map m ON q.id = m.question_id " +
                        "<where>" +
                        "  <if test='kbId != null'>" +
                        "    q.kb_id = #{kbId}" +
                        "  </if>" +
                        "  <if test='content != null and content != \"\"'>" +
                        "    AND q.content LIKE CONCAT('%', #{content}, '%')" +
                        "  </if>" +
                        "</where>" +
                        "GROUP BY q.id, q.kb_id, q.content, q.hit_num, q.source_type, q.create_time " +
                        "ORDER BY q.create_time DESC " +
                        "LIMIT #{limit} OFFSET #{offset}" +
                        "</script>")
        List<java.util.Map<String, Object>> selectPageWithChunkCount(
                        @Param("kbId") Long kbId,
                        @Param("content") String content,
                        @Param("offset") long offset,
                        @Param("limit") long limit);

        /**
         * 查询满足条件的问题总数
         * 
         * @param kbId    知识库ID（可选）
         * @param content 问题内容（模糊匹配，可选）
         * @return 问题总数
         */
        @Select("<script>" +
                        "SELECT COUNT(DISTINCT q.id) " +
                        "FROM km_question q " +
                        "<where>" +
                        "  <if test='kbId != null'>" +
                        "    q.kb_id = #{kbId}" +
                        "  </if>" +
                        "  <if test='content != null and content != \"\"'>" +
                        "    AND q.content LIKE CONCAT('%', #{content}, '%')" +
                        "  </if>" +
                        "</where>" +
                        "</script>")
        long countByCondition(
                        @Param("kbId") Long kbId,
                        @Param("content") String content);

        /**
         * 根据文档ID查询关联的问题列表 - 单次JOIN查询优化
         * 
         * @param documentId 文档ID
         * @return 问题列表
         */
        List<KmQuestion> selectByDocumentId(@Param("documentId") Long documentId);

        /**
         * 分页查询问题列表(含分段数量) - 使用 XML 映射
         * 
         * @param page    分页对象
         * @param kbId    知识库ID(可选)
         * @param content 问题内容(模糊匹配,可选)
         * @return 分页结果
         */
        Page<KmQuestionVo> selectPageList(
                        Page<KmQuestionVo> page,
                        @Param("kbId") Long kbId,
                        @Param("content") String content);
}
