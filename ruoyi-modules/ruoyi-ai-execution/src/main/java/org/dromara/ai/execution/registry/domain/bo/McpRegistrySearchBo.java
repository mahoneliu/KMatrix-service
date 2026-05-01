package org.dromara.ai.execution.registry.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * MCP 注册源条目搜索请求对象
 *
 * @author Mahone
 */
@Data
public class McpRegistrySearchBo {

    /**
     * 关键词（模糊匹配名称、描述、标签）
     */
    private String keyword;

    /**
     * 来源平台筛选（official / smithery / 空=全部）
     */
    private String sourcePlatform;

    /**
     * 标签筛选列表
     */
    private List<String> tags;

    /**
     * 页码
     */
    private int pageNum = 1;

    /**
     * 每页条数
     */
    private int pageSize = 20;

}
