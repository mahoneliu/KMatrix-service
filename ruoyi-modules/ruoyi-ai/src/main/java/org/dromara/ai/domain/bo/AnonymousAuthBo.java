package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 匿名用户认证请求 Bo
 *
 * @author Mahone
 * @date 2026-01-27
 */
@Data
public class AnonymousAuthBo {

    /**
     * 应用 Token
     */
    @NotBlank(message = "appToken 不能为空")
    private String appToken;
}
