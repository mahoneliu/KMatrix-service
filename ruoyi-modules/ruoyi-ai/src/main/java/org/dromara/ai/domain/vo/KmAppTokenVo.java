package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmAppToken;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * App嵌入Token视图对象
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Data
@AutoMapper(target = KmAppToken.class)
public class KmAppTokenVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Token ID
     */
    private Long tokenId;

    /**
     * 关联应用ID
     */
    private Long appId;

    /**
     * Token值
     */
    private String token;

    /**
     * Token名称
     */
    private String tokenName;

    /**
     * 允许的来源域名
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
