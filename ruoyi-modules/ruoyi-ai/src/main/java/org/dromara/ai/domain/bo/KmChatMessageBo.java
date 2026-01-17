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
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /**
     * 角色(user/assistant)
     */
    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 工作流实例ID
     */
    private Long instanceId;
}
