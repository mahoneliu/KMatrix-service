package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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
        @Update("UPDATE km_question SET hit_num = hit_num + 1 WHERE id = #{id}")
        int incrementHitNum(@Param("id") Long id);

        /**
         * 批量更新问题命中次数
         * 
         * @param questionIds 问题ID列表
         */
        @Update("<script>" +
                        "UPDATE km_question SET hit_num = hit_num + 1 " +
                        "WHERE id IN " +
                        "<foreach collection='questionIds' item='id' open='(' separator=',' close=')'>" +
                        "  #{id}" +
                        "</foreach>" +
                        "</script>")
        int batchIncrementHitNum(@Param("questionIds") List<Long> questionIds);


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
