package org.dromara.ai.domain.vo;

import lombok.Data;
import org.dromara.ai.enums.ChatUserType;

/**
 * Session Token 解析后的信息
 *
 * @author Mahone
 * @date 2026-01-27
 */
@Data
public class ChatSessionTokenInfo {

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 应用 Token (用于二次验证)
     */
    private String appToken;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private ChatUserType userType;

    /**
     * Token 过期时间戳 (毫秒)
     */
    private Long expireTime;
}
