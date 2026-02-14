package org.dromara.ai.domain.vo;

import lombok.Data;

/**
 * 匿名用户认证响应 Vo
 *
 * @author Mahone
 * @date 2026-01-27
 */
@Data
public class AnonymousAuthVo {

    /**
     * Session Token (JWT格式)
     */
    private String sessionToken;

    /**
     * 用户ID (雪花算法生成, 用于关联会话)
     */
    private Long userId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * Token 过期时间戳 (毫秒)
     */
    private Long expireTime;
}
