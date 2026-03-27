package org.dromara.ai.app.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 聊天消息业务对象 km_chat_message
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmChatMessage.class, reverseConvertGenerate = false)
public class KmChatMessageBo extends BaseEntity {

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话ID
     */
    @NotNull(message = "{ai.val.chat.session_id_required}")
    private Long sessionId;

    /**
     * 角色(user/assistant)
     */
    @NotBlank(message = "{ai.val.common.role_required}")
    private String role;

    /**
     * 内容
     */
    @NotBlank(message = "{ai.val.chat.message_content_required}")
    private String content;

    /**
     * 工作流实例ID
     */
    private Long instanceId;

    /**
     * 中断状态: none=未中断, aborted=已中断
     */
    private String abortStatus;

    /**
     * 请求被中断时已生成的部分内容
     */
    private String partialContent;

    /**
     * 请求被中断的时间
     */
    private java.time.LocalDateTime abortTime;

    /**
     * 请求的唯一标识符
     */
    private String requestId;

    /**
     * 中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误
     */
    private String abortReason;
}
