package org.dromara.ai.mapper;

import org.dromara.ai.domain.KmAppAccessStat;
import org.dromara.ai.domain.vo.KmAppAccessStatVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 应用访问统计Mapper接口
 *
 * @author Mahone
 * @date 2025-12-27
 */
public interface KmAppAccessStatMapper extends BaseMapperPlus<KmAppAccessStat, KmAppAccessStatVo> {

    /**
     * 原子增量更新应用访问统计
     * @param appId 应用ID
     * @param userId 用户ID
     * @param tokens 增加的 Token 数
     * @param likes 点赞增量 (1, -1, 0)
     * @param dislikes 点踩增量 (1, -1, 0)
     * @param questions 提问次数增量 (1)
     */
    @Update("INSERT INTO km_app_access_stat (id, app_id, user_id, token_count, like_count, dislike_count, question_count, access_count, last_access_time) " +
            "VALUES (#{id}, #{appId}, #{userId}, #{tokens}, #{likes}, #{dislikes}, #{questions}, 1, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (app_id, user_id) DO UPDATE SET " +
            "token_count = km_app_access_stat.token_count + EXCLUDED.token_count, " +
            "like_count = km_app_access_stat.like_count + EXCLUDED.like_count, " +
            "dislike_count = km_app_access_stat.dislike_count + EXCLUDED.dislike_count, " +
            "question_count = km_app_access_stat.question_count + EXCLUDED.question_count, " +
            "access_count = km_app_access_stat.access_count + 1, " +
            "last_access_time = EXCLUDED.last_access_time")
    int incrementStats(@Param("id") Long id, @Param("appId") Long appId, @Param("userId") Long userId,
                       @Param("tokens") Long tokens, @Param("likes") Integer likes,
                       @Param("dislikes") Integer dislikes, @Param("questions") Integer questions);

}
