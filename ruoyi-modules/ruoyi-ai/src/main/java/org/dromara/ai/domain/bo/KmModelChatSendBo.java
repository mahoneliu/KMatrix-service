package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模型测试对话请求对象
 *
 * @author Mahone
 * @date 2025-01-08
 */
@Data
public class KmModelChatSendBo {

    /**
     * 模型ID
     */
    @NotNull(message = "{ai.val.model.id_required}")
    private Long modelId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "{ai.val.chat.message_content_required}")
    private String message;

    /**
     * 是否流式返回 (默认true)
     */
    private Boolean stream = true;

    /**
     * 温度 (0.0 - 2.0)
     */
    private Double temperature;

    /**
     * 最大token数
     */
    private Integer maxTokens;
}
