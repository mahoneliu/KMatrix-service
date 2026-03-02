package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmChatMessage;
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
}
