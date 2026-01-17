package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmChatSession;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 聊天会话业务对象 km_chat_session
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmChatSession.class, reverseConvertGenerate = false)
public class KmChatSessionBo extends BaseEntity {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    /**
     * 会话标题
     */
    @NotBlank(message = "会话标题不能为空")
    @Size(min = 0, max = 200, message = "会话标题长度不能超过200个字符")
    private String title;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 删除标志(0代表存在 1代表删除)
     */
    private String delFlag;
}
