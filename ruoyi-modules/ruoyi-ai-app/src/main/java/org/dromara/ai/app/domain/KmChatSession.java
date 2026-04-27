package org.dromara.ai.app.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天会话对象 km_chat_session
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_chat_session", autoResultMap = true)
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
    @TableField("abort_reason")
    private String abortReason;

    /**
     * 异常类型（仅当中断原因为exception时有值）
     */
    @TableField("abort_exception_type")
    private String abortExceptionType;

    /**
     * 异常消息（仅当中断原因为exception时有值）
     */
    @TableField("abort_exception_message")
    private String abortExceptionMessage;

    /**
     * 异常堆栈信息（仅当中断原因为exception时有值）
     */
    @TableField("abort_exception_stacktrace")
    private String abortExceptionStacktrace;

    /**
     * 会话被中断的时间戳
     */
    @TableField("abort_timestamp")
    private java.time.LocalDateTime abortTimestamp;

    /**
     * 中断时最后一条消息的ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 会话是否可恢复（0=不可恢复, 1=可恢复）
     */
    @TableField("is_resumable")
    private String isResumable;

    /**
     * 恢复令牌，用于防止重复恢复
     */
    @TableField("resume_token")
    private String resumeToken;

    /**
     * 会话被恢复的时间戳
     */
    @TableField("resumed_at")
    private java.time.LocalDateTime resumedAt;

    /**
     * 会话变量（JSONB），用于在同一会话的多轮对话间持久化状态
     * key: 变量名, value: 变量值
     */
    @TableField(value = "session_variables", typeHandler = JsonTypeHandler.class)
    private Map<String, Object> sessionVariables = new HashMap<>();
}
