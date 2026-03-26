package org.dromara.ai.app.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.domain.vo.KmChatMessageVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息Mapper接口
 *
 * @author Mahone
 * @date 2025-12-31
 */
public interface KmChatMessageMapper extends BaseMapperPlus<KmChatMessage, KmChatMessageVo> {

    /**
     * 查询指定时间范围内应用消耗的 Token 总数
     */
    @Select("<script>" +
            "SELECT COALESCE(SUM(m.total_tokens), 0) FROM km_chat_message m " +
            "JOIN km_chat_session s ON m.session_id = s.session_id " +
            "WHERE s.app_id = #{appId} AND m.role = 'assistant' " +
            "<if test='startTime != null'> AND s.create_time &gt;= #{startTime} </if>" +
            "</script>")
    Long sumTotalTokensByAppId(@Param("appId") Long appId, @Param("startTime") Date startTime);

    /**
     * 查询指定时间范围内应用的评价统计
     */
    @Select("<script>" +
            "SELECT m.feedback_status as status, COUNT(*) as count FROM km_chat_message m " +
            "JOIN km_chat_session s ON m.session_id = s.session_id " +
            "WHERE s.app_id = #{appId} AND m.feedback_status IN (1, -1) " +
            "<if test='startTime != null'> AND s.create_time &gt;= #{startTime} </if> " +
            "GROUP BY m.feedback_status" +
            "</script>")
    List<Map<String, Object>> countFeedbackByAppId(@Param("appId") Long appId, @Param("startTime") Date startTime);

    /**
     * 查询指定时间范围内应用的每日提问次数趋势
     */
    @Select("<script>" +
            "SELECT TO_CHAR(m.create_time, 'YYYY-MM-DD') as date, COUNT(*) as count " +
            "FROM km_chat_message m " +
            "JOIN km_chat_session s ON m.session_id = s.session_id " +
            "WHERE s.app_id = #{appId} AND m.role = 'user' " +
            "<if test='startTime != null'> AND s.create_time &gt;= #{startTime} </if> " +
            "GROUP BY TO_CHAR(m.create_time, 'YYYY-MM-DD') " +
            "ORDER BY date ASC" +
            "</script>")
    List<Map<String, Object>> getQuestionTrendByAppId(@Param("appId") Long appId, @Param("startTime") Date startTime);
}
