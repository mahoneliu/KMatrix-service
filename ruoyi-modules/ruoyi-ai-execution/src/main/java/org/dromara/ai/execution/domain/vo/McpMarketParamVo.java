package org.dromara.ai.execution.domain.vo;

import lombok.Data;

/**
 * MCP 市场条目参数视图对象
 *
 * @author Kiro
 */
@Data
public class McpMarketParamVo {

    /**
     * 占位符 key，如 API_KEY
     */
    private String key;

    /**
     * 显示名称
     */
    private String label;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 参数描述
     */
    private String description;

}
