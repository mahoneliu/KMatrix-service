package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.domain.KmChatMessage;

/**
 * 发送消息业务对象 km_chat_message
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@AutoMapper(target = KmChatMessage.class, reverseConvertGenerate = false)
public class KmChatSendBo {

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    /**
     * 会话ID(可选,首次对话时为空)
     */
    private Long sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 是否流式返回
     */
    private Boolean stream = true;

    /**
     * 是否为调试模式
     * 调试模式下，后端会实时从数据库获取最新的草稿DSL进行测试
     * 调试数据不会保存到数据库
     */
    private Boolean debug = false;
}
