package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.domain.KmQuestion;

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
}
