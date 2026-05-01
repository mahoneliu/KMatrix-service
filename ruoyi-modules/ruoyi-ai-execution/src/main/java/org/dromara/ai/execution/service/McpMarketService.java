package org.dromara.ai.execution.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.domain.vo.McpMarketItemVo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCP 市场 Service
 * <p>
 * 应用启动时从 classpath 加载 mcp-market.json 并缓存到内存，
 * 提供关键词和分类过滤查询能力。
 *
 * @author Kiro
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class McpMarketService {

    private static final String MCP_MARKET_JSON = "mcp-market.json";

    private final ObjectMapper objectMapper;

    /**
     * 内存缓存，应用启动时加载一次
     */
    private List<McpMarketItemVo> cachedItems = Collections.emptyList();

    /**
     * 应用启动时加载并缓存 mcp-market.json
     * 配置文件不存在或格式错误时抛出异常，阻止应用启动（快速失败原则）
     */
    @PostConstruct
    public void init() {
        log.info("正在加载 MCP 市场配置文件: {}", MCP_MARKET_JSON);
        ClassPathResource resource = new ClassPathResource(MCP_MARKET_JSON);
        if (!resource.exists()) {
            throw new IllegalStateException("MCP 市场配置文件不存在: " + MCP_MARKET_JSON);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode itemsNode = root.get("items");
            if (itemsNode == null || !itemsNode.isArray()) {
                throw new IllegalStateException("MCP 市场配置文件格式错误：缺少 'items' 数组节点");
            }
            List<McpMarketItemVo> items = new ArrayList<>();
            for (JsonNode node : itemsNode) {
                McpMarketItemVo item = objectMapper.treeToValue(node, McpMarketItemVo.class);
                items.add(item);
            }
            cachedItems = Collections.unmodifiableList(items);
            log.info("MCP 市场配置文件加载完成，共 {} 个条目", cachedItems.size());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("MCP 市场配置文件解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询 MCP 市场条目列表
     *
     * @param keyword  关键词（非空时过滤名称或描述，忽略大小写）
     * @param category 分类（非空时过滤分类等于该值）
     * @return 过滤后的条目列表
     */
    public List<McpMarketItemVo> listItems(String keyword, String category) {
        try {
            return cachedItems.stream()
                .filter(item -> {
                    if (StrUtil.isBlank(keyword)) {
                        return true;
                    }
                    String lowerKeyword = keyword.toLowerCase();
                    String name = StrUtil.nullToEmpty(item.getName()).toLowerCase();
                    String description = StrUtil.nullToEmpty(item.getDescription()).toLowerCase();
                    return name.contains(lowerKeyword) || description.contains(lowerKeyword);
                })
                .filter(item -> {
                    if (StrUtil.isBlank(category)) {
                        return true;
                    }
                    return category.equals(item.getCategory());
                })
                .toList();
        } catch (Exception e) {
            log.error("MCP 市场条目过滤异常", e);
            return Collections.emptyList();
        }
    }

}
