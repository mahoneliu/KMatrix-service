package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * App嵌入Token对象 km_app_token
 * 用于第三方应用嵌入对话窗口的授权
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_app_token")
public class KmAppToken extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Token ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "token_id")
    private Long tokenId;

    /**
     * 关联应用ID
     */
    private Long appId;

    /**
     * Token值（唯一）
     */
    private String token;

    /**
     * Token名称
     */
    private String tokenName;

    /**
     * 允许的来源域名（逗号分隔，* 表示全部）
     */
    private String allowedOrigins;

    /**
     * 过期时间（null表示永不过期）
     */
    private LocalDateTime expiresAt;

    /**
     * 状态（0停用 1启用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    private String delFlag;
}
