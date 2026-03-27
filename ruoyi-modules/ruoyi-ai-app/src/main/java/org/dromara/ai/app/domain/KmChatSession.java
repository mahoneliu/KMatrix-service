package org.dromara.ai.app.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 聊天会话对象 km_chat_session
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_chat_session")
public class KmChatSession extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "session_id")
    private Long sessionId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型 (anonymous_user/system_user/third_user)
     */
    private String userType;

    /**
     * 删除标志(0代表存在 1代表删除)
     */
    private String delFlag;

    /**
     * 中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误
     */
    private String abortReason;

    /**
     * 异常类型（仅当中断原因为exception时有值）
     */
    private String abortExceptionType;

    /**
     * 异常消息（仅当中断原因为exception时有值）
     */
    private String abortExceptionMessage;

    /**
     * 异常堆栈信息（仅当中断原因为exception时有值）
     */
    private String abortExceptionStacktrace;

    /**
     * 会话被中断的时间戳
     */
    private java.time.LocalDateTime abortTimestamp;

    /**
     * 中断时最后一条消息的ID
     */
    private Long lastMessageId;

    /**
     * 会话是否可恢复（0=不可恢复, 1=可恢复）
     */
    private String isResumable;

    /**
     * 恢复令牌，用于防止重复恢复
     */
    private String resumeToken;

    /**
     * 会话被恢复的时间戳
     */
    private java.time.LocalDateTime resumedAt;
}
