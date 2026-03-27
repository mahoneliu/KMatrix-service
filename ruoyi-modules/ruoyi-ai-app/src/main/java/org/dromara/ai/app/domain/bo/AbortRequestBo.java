package org.dromara.ai.app.domain.bo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 中断请求业务对象
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Data
public class AbortRequestBo {

    /**
     * 请求ID
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 中断原因（可选）
     */
    private String reason;
}
