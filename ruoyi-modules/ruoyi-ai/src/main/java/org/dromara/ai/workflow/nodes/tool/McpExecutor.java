package org.dromara.ai.workflow.nodes.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP 客户端执行器
 * 负责将 Tool Call 代理给远端 MCP Server
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
public class McpExecutor implements ToolExecutor {
    private final Long serverId;
    private final String serverUrl;
    private final String toolName;
    private final RestTemplate restTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String execute(String arguments) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("id", System.currentTimeMillis());
        requestBody.put("method", "tools/call");
        
        Map<String, Object> params = new HashMap<>();
        // MCP 协议要求 params 中同时包含 name 和 arguments 两个字段
        params.put("name", toolName);
        params.put("arguments", MAPPER.readValue(arguments == null ? "{}" : arguments, new TypeReference<Map<String, Object>>() {}));
        requestBody.put("params", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("向 MCP Server [{}] 发起工具调用: {}", serverId, requestBody);
        
        try {
            String responseStr = restTemplate.postForObject(serverUrl, entity, String.class);
            log.debug("MCP Server [{}] 响应: {}", serverId, responseStr);

            Map<String, Object> response = MAPPER.readValue(responseStr, new TypeReference<Map<String, Object>>() {});
            if (response.containsKey("error")) {
                return "Error from MCP server: " + response.get("error");
            }
            
            Object result = response.get("result");
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            log.error("MCP 调用异常, URL: {}", serverUrl, e);
            throw new RuntimeException("MCP Execution failed: " + e.getMessage(), e);
        }
    }
}
