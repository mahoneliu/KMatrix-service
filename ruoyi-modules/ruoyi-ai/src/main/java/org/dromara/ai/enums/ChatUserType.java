package org.dromara.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对话用户类型枚举
 *
 * @author Mahone
 * @date 2026-01-27
 */
@Getter
@AllArgsConstructor
public enum ChatUserType {

    /**
     * 匿名用户 - 通过 App Token 访问的未登录用户
     */
    ANONYMOUS_USER("anonymous_user", "匿名用户"),

    /**
     * 系统用户 - 系统内已登录的用户
     */
    SYSTEM_USER("system_user", "系统用户"),

    /**
     * 第三方用户 - 通过 SSO 等方式认证的外部用户
     */
    THIRD_USER("third_user", "第三方用户");

    private final String code;
    private final String label;

    /**
     * 根据 code 获取枚举
     */
    public static ChatUserType fromCode(String code) {
        for (ChatUserType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
