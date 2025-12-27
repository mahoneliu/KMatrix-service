package org.dromara.ai.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI文档状态
 *
 * @author Mahone
 */
@Getter
@AllArgsConstructor
public enum AiDocumentStatus {
    /**
     * 解析中
     */
    PARSING("0", "解析中"),
    /**
     * 完成
     */
    COMPLETED("1", "完成"),
    /**
     * 失败
     */
    FAIL("2", "失败");

    private final String code;
    private final String info;
}
