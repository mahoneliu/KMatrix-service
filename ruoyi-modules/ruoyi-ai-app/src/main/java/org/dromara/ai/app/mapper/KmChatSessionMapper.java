package org.dromara.ai.app.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.ai.app.domain.KmChatSession;
import org.dromara.ai.app.domain.vo.KmChatSessionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 聊天会话Mapper接口
 *
 * @author Mahone
 * @date 2025-12-31
 */
public interface KmChatSessionMapper extends BaseMapperPlus<KmChatSession, KmChatSessionVo> {

    /**
     * 查询指定时间范围内应用的每日用户数（去重）趋势
     */
    @Select("<script>" +
            "SELECT TO_CHAR(create_time, 'YYYY-MM-DD') as date, COUNT(DISTINCT user_id) as count " +
            "FROM km_chat_session " +
            "WHERE app_id = #{appId} " +
            "<if test='startTime != null'> AND create_time &gt;= #{startTime} </if> " +
            "GROUP BY TO_CHAR(create_time, 'YYYY-MM-DD') " +
            "ORDER BY date ASC" +
            "</script>")
    List<Map<String, Object>> getUserTrendByAppId(@Param("appId") Long appId, @Param("startTime") Date startTime);
}
