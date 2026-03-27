package org.dromara.ai.app.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 聊天消息对象 km_chat_message
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_chat_message")
public class KmChatMessage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private Long messageId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 角色(user/assistant)
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 工作流实例ID
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 用户反馈状态: 0=无, 1=赞同, -1=踩
     */
    @TableField("feedback_status")
    private Integer feedbackStatus;

    /**
     * 消耗的 Token 总数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 中断状态: none=未中断, aborted=已中断
     */
    @TableField("abort_status")
    private String abortStatus;

    /**
     * 请求被中断时已生成的部分内容
     */
    @TableField("partial_content")
    private String partialContent;

    /**
     * 请求被中断的时间
     */
    @TableField("abort_time")
    private java.time.LocalDateTime abortTime;

    /**
     * 请求的唯一标识符
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误
     */
    @TableField("abort_reason")
    private String abortReason;
}
