package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI供应商类型
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum AiProviderType {
    /**
     * 公有
     */
    PUBLIC("1", "公有"),
    /**
     * 本地
     */
    LOCAL("2", "本地");

    private final String code;
    private final String info;
}
