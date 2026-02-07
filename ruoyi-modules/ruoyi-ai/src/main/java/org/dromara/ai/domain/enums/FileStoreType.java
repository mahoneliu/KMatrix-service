package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型枚举
 *
 * @author AI Assistant
 * @date 2026-02-06
 */
@Getter
@AllArgsConstructor
public enum FileStoreType {

    /**
     * OSS 对象存储
     */
    OSS(1, "对象存储"),

    /**
     * 本地文件存储
     */
    LOCAL(2, "本地存储");

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据值获取枚举
     *
     * @param value 类型值
     * @return 枚举对象
     */
    public static FileStoreType fromValue(Integer value) {
        if (value == null) {
            return OSS; // 默认返回 OSS
        }
        for (FileStoreType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return OSS; // 默认返回 OSS
    }

    /**
     * 判断是否为 OSS 存储
     *
     * @return true-OSS存储, false-其他存储
     */
    public boolean isOss() {
        return this == OSS;
    }

    /**
     * 判断是否为本地存储
     *
     * @return true-本地存储, false-其他存储
     */
    public boolean isLocal() {
        return this == LOCAL;
    }
}
