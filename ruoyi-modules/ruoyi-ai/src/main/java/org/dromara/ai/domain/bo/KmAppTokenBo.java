package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.domain.KmAppToken;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * App嵌入Token业务对象
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Data
@AutoMapper(target = KmAppToken.class, reverseConvertGenerate = false)
public class KmAppTokenBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Token ID (更新时必填)
     */
    private Long tokenId;

    /**
     * 关联应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    /**
     * Token名称
     */
    @NotBlank(message = "{ai.val.auth.token_name_required}")
    private String tokenName;

    /**
     * 允许的来源域名（逗号分隔，* 表示全部）
     */
    private String allowedOrigins;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
