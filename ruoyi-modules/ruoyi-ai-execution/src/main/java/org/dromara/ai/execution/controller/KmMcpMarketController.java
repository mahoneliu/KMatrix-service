package org.dromara.ai.execution.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.domain.vo.McpMarketItemVo;
import org.dromara.ai.execution.service.McpMarketService;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MCP 市场控制器
 * <p>
 * 提供 MCP 市场条目的公开浏览接口，无需权限即可访问。
 *
 * @author Kiro
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/mcp-market")
public class KmMcpMarketController {

    private final McpMarketService mcpMarketService;

    /**
     * 查询 MCP 市场条目列表
     *
     * @param keyword  关键词（可选，匹配名称或描述，忽略大小写）
     * @param category 分类（可选，精确匹配）
     * @return 过滤后的条目列表
     */
    @GetMapping("/list")
    public R<List<McpMarketItemVo>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return R.ok(mcpMarketService.listItems(keyword, category));
    }

}
