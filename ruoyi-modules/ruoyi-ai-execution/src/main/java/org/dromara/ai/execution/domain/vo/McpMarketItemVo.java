package org.dromara.ai.execution.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * MCP 市场条目视图对象
 *
 * @author Kiro
 */
@Data
public class McpMarketItemVo {

    /**
     * 条目唯一标识
     */
    private String id;

    /**
     * 条目名称
     */
    private String name;

    /**
     * 图标（如 mdi:magnify）
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 传输类型（sse / streamable_http）
     */
    private String transportType;

    /**
     * 配置模板 JSON 字符串，含占位符如 ${API_KEY}
     */
    private String configTemplate;

    /**
     * 必填参数定义列表
     */
    private List<McpMarketParamVo> params;

    /**
     * 配置示例说明
     */
    private String configExample;

}
