package org.dromara.ai.domain.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * AI应用统计 VO
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Data
public class KmAppStatisticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户总数
     */
    private Long userCount;

    /**
     * 用户总数增量 (相比上个周期)
     */
    private Long userCountDelta;

    /**
     * 提问次数 (对话消息数)
     */
    private Long questionCount;

    /**
     * Tokens 总数
     */
    private Long tokensTotal;

    /**
     * 用户满意度 (点赞/点踩)
     */
    private Satisfaction satisfaction;

    /**
     * 趋势图表数据 (日期 -> 数量)
     */
    private Map<String, Long> userTrend;

    /**
     * 提问趋势图表数据 (日期 -> 数量)
     */
    private Map<String, Long> questionTrend;

    @Data
    public static class Satisfaction implements Serializable {
        private Long like;
        private Long dislike;
    }
}
