package org.dromara.ai.app.domain.vo;

import lombok.Data;

/**
 * 聊天频率限制配置 VO
 * 对应 JSON 结构: {"minute":{"requests":N,"tokens":N},"hour":{...},"day":{...}}
 *
 * @author Mahone
 * @date 2026-03-23
 */
@Data
public class ChatRateLimitConfigVo {

    /**
     * 分钟级限制
     */
    private LimitQuota minute;

    /**
     * 小时级限制
     */
    private LimitQuota hour;

    /**
     * 天级限制
     */
    private LimitQuota day;

    /**
     * 单个维度的配额
     */
    @Data
    public static class LimitQuota {

        /**
         * 请求次数上限，null 表示不限制
         */
        private Integer requests;

        /**
         * Token 消耗上限，null 表示不限制
         */
        private Integer tokens;
    }
}
