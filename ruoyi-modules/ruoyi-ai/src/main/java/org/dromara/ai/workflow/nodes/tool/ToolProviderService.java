package org.dromara.ai.workflow.nodes.tool;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmBuiltinTool;
import org.dromara.ai.domain.KmMcpServer;
import org.dromara.ai.mapper.KmBuiltinToolMapper;
import org.dromara.ai.mapper.KmMcpServerMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具提供服务
 * <p>
 * 负责根据工作流节点配置的 tools 字段，解析并返回 {@link ToolBinding} 列表。
 * 每个 ToolBinding 包含 LangChain4j {@link ToolSpecification} 和对应的
 * {@link ToolExecutor}。
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ToolProviderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** MCP HTTP 请求超时（毫秒） */
    private static final int MCP_TIMEOUT_MS = 30_000;

    private final KmBuiltinToolMapper builtinToolMapper;
    private final KmMcpServerMapper mcpServerMapper;

    /**
     * 根据节点 tools 配置解析工具绑定列表
     *
     * @param toolRefs 工具引用列表，格式：[{"type":"builtin","id":123},
     *                 {"type":"mcp","id":456}]
     * @return 工具绑定列表（可直接用于注入 LLM）
     */
    @SuppressWarnings("unchecked")
    public List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs) {
        List<ToolBinding> bindings = new ArrayList<>();
        if (toolRefs == null || toolRefs.isEmpty()) {
            return bindings;
        }

        RestTemplate restTemplate = createRestTemplate();

        for (Map<String, Object> toolRef : toolRefs) {
            String type = (String) toolRef.get("type");
            Object idObj = toolRef.get("id");
            log.debug("ToolProviderService - 解析引用: type={}, id={}", type, idObj);
            if (StrUtil.isBlank(type) || idObj == null) {
                continue;
            }

            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());

            try {
                if ("builtin".equalsIgnoreCase(type)) {
                    // 内置 Python 工具
                    List<ToolBinding> builtinBindings = resolveBuiltinTool(id);
                    bindings.addAll(builtinBindings);
                } else if ("mcp".equalsIgnoreCase(type)) {
                    // MCP Server 工具
                    List<ToolBinding> mcpBindings = resolveMcpTools(id, restTemplate);
                    bindings.addAll(mcpBindings);
                } else {
                    log.warn("未知工具类型: type={}", type);
                }
            } catch (Exception e) {
                log.error("解析工具绑定失败: type={}, id={}", type, id, e);
            }
        }

        log.info("工具绑定解析完成，共 {} 个工具", bindings.size());
        return bindings;
    }

    /**
     * 解析内置 Python 工具绑定（任务 7.3）
     */
    private List<ToolBinding> resolveBuiltinTool(Long toolId) {
        List<ToolBinding> result = new ArrayList<>();

        KmBuiltinTool tool = builtinToolMapper.selectById(toolId);
        if (tool == null) {
            log.error("内置工具不存在 (DB查询失败): toolId={}", toolId);
            return result;
        }
        log.debug("解析内置工具详情: toolName={}, status={}, inputSchema={}, pythonCode长度={}",
                tool.getToolName(), tool.getStatus(), tool.getInputSchema(),
                tool.getPythonCode() == null ? 0 : tool.getPythonCode().length());
        if (!"0".equals(tool.getStatus())) {
            log.warn("内置工具已停用: toolId={}, toolName={}", toolId, tool.getToolName());
            return result;
        }

        // 从 inputSchema 构建 ToolSpecification
        ToolSpecification spec = ToolJsonSchemaUtils.buildToolSpecification(
                tool.getToolName(),
                tool.getSpec(),
                tool.getInputSchema());

        ToolExecutor executor = new PythonBuiltinExecutor(tool.getToolName(), tool.getPythonCode());

        result.add(ToolBinding.builder()
                .toolName(tool.getToolName())
                .specification(spec)
                .executor(executor)
                .build());

        log.info("内置工具绑定成功: toolName={}", tool.getToolName());
        return result;
    }

    /**
     * 动态拉取 MCP Server 工具列表并创建工具绑定（任务 7.2）
     */
    @SuppressWarnings("unchecked")
    private List<ToolBinding> resolveMcpTools(Long serverId, RestTemplate restTemplate) {
        List<ToolBinding> result = new ArrayList<>();

        KmMcpServer server = mcpServerMapper.selectById(serverId);
        if (server == null) {
            log.warn("MCP Server 不存在: serverId={}", serverId);
            return result;
        }
        if (!"0".equals(server.getStatus())) {
            log.warn("MCP Server 已停用: serverId={}, serverName={}", serverId, server.getServerName());
            return result;
        }

        // 解析 serverConfig 中的 url
        String serverUrl = extractServerUrl(server.getServerConfig());
        if (StrUtil.isBlank(serverUrl)) {
            log.error("MCP Server 配置缺少 url 字段: serverId={}", serverId);
            return result;
        }

        // 发送 tools/list 请求获取工具列表
        List<Map<String, Object>> mcpTools = fetchMcpToolList(serverUrl, serverId, restTemplate);

        for (Map<String, Object> mcpTool : mcpTools) {
            String toolName = (String) mcpTool.get("name");
            String description = (String) mcpTool.getOrDefault("description", "");

            if (StrUtil.isBlank(toolName)) {
                continue;
            }

            // 解析 MCP 工具的 inputSchema（MCP 标准：inputSchema 字段）
            Object inputSchemaObj = mcpTool.get("inputSchema");
            String inputSchemaJson = null;
            if (inputSchemaObj != null) {
                try {
                    inputSchemaJson = MAPPER.writeValueAsString(inputSchemaObj);
                } catch (Exception e) {
                    log.warn("序列化 MCP 工具 inputSchema 失败: toolName={}", toolName);
                }
            }

            ToolSpecification spec = ToolJsonSchemaUtils.buildToolSpecification(toolName, description, inputSchemaJson);
            ToolExecutor executor = new McpExecutor(serverId, serverUrl, toolName, restTemplate);

            result.add(ToolBinding.builder()
                    .toolName(toolName)
                    .specification(spec)
                    .executor(executor)
                    .build());

            log.info("MCP 工具绑定成功: serverId={}, toolName={}", serverId, toolName);
        }

        return result;
    }

    /**
     * 从 serverConfig JSON 中提取 url 字段
     */
    @SuppressWarnings("unchecked")
    private String extractServerUrl(String serverConfig) {
        if (StrUtil.isBlank(serverConfig)) {
            return null;
        }
        try {
            Map<String, Object> config = MAPPER.readValue(serverConfig, new TypeReference<Map<String, Object>>() {
            });
            return (String) config.get("url");
        } catch (Exception e) {
            log.error("解析 serverConfig 失败: {}", serverConfig, e);
            return null;
        }
    }

    /**
     * 发送 MCP tools/list 请求获取工具定义列表（任务 7.2）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchMcpToolList(String serverUrl, Long serverId, RestTemplate restTemplate) {
        List<Map<String, Object>> tools = new ArrayList<>();

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jsonrpc", "2.0");
            requestBody.put("id", System.currentTimeMillis());
            requestBody.put("method", "tools/list");
            requestBody.put("params", new HashMap<>());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String responseStr = restTemplate.postForObject(serverUrl, entity, String.class);
            log.debug("MCP tools/list 响应: serverId={}, response={}", serverId, responseStr);

            Map<String, Object> response = MAPPER.readValue(responseStr, new TypeReference<Map<String, Object>>() {
            });

            if (response.containsKey("error")) {
                log.error("MCP tools/list 返回错误: serverId={}, error={}", serverId, response.get("error"));
                return tools;
            }

            Object result = response.get("result");
            if (result instanceof Map) {
                Object toolsObj = ((Map<String, Object>) result).get("tools");
                if (toolsObj instanceof List) {
                    for (Object item : (List<?>) toolsObj) {
                        if (item instanceof Map) {
                            tools.add((Map<String, Object>) item);
                        }
                    }
                }
            }

            log.info("MCP tools/list 成功: serverId={}, 工具数={}", serverId, tools.size());
        } catch (Exception e) {
            log.error("MCP tools/list 请求失败: serverId={}, url={}", serverId, serverUrl, e);
        }

        return tools;
    }

    /**
     * 创建带超时配置的 RestTemplate
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(MCP_TIMEOUT_MS);
        factory.setReadTimeout(MCP_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
