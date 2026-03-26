package org.dromara.ai.app.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 聊天消息评价请求参数
 */
@Data
public class KmChatFeedbackBo {

    /**
     * 消息 ID
     */
    @NotNull(message = "消息 ID 不能为空")
    private Long messageId;

    /**
     * 评价状态 (0=取消, 1=赞, -1=踩)
     */
    @NotNull(message = "评价状态不能为空")
    private Integer feedbackStatus;
}
