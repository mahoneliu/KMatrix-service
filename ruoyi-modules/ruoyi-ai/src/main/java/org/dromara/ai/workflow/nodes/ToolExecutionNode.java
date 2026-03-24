package org.dromara.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmBuiltinTool;
import org.dromara.ai.mapper.KmBuiltinToolMapper;
import org.dromara.ai.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.nodes.tool.ToolBinding;
import org.dromara.ai.workflow.nodes.tool.ToolProviderService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具节点
 * 独立执行指定的内置工具或MCP工具
 *
 * @author KMatrix
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
@Component("TOOL")
public class ToolExecutionNode extends AbstractWorkflowNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolProviderService toolProviderService;
    private final KmBuiltinToolMapper builtinToolMapper;

    @Override
    @SuppressWarnings("unchecked")
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行TOOL节点");
        NodeOutput output = new NodeOutput();

        // 1. 获取选中的工具配置
        // 前端配置格式可能是 { "type": "mcp", "id": 1 } 或 { "type": "builtin", "id": 2 }
        Object toolObj = context.getConfig("tool");
        if (!(toolObj instanceof Map)) {
            throw new RuntimeException("工具节点未正确配置具体工具");
        }
        Map<String, Object> toolRef = (Map<String, Object>) toolObj;

        // 2. 解析工具
        List<ToolBinding> bindings = toolProviderService.resolveBindings(Collections.singletonList(toolRef));
        if (bindings.isEmpty()) {
            throw new RuntimeException("无法解析配置的工具: " + toolRef);
        }
        ToolBinding binding = bindings.get(0);

        // 3. 构建参数
        // 策略：先把 initParams 的 defaultValue 作为基础参数，再用 nodeInputs（前端绑定的实际值）覆盖。
        // 这样同名参数时 inputSchema 参数值优先，initParams 的 defaultValue 只在未绑定时生效。
        Map<String, Object> mergedArgs = new LinkedHashMap<>();

        // 3a. 内置工具时，尝试注入 initParams defaultValue
        String toolType = (String) toolRef.get("type");
        if ("builtin".equalsIgnoreCase(toolType)) {
            Object idObj = toolRef.get("id");
            if (idObj != null) {
                Long toolId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                try {
                    KmBuiltinTool tool = builtinToolMapper.selectById(toolId);
                    if (tool != null && StrUtil.isNotBlank(tool.getInitParams())) {
                        List<Map<String, Object>> initParamList = MAPPER.readValue(
                                tool.getInitParams(), new TypeReference<List<Map<String, Object>>>() {});
                        for (Map<String, Object> param : initParamList) {
                            Object name = param.get("name");
                            Object defaultValue = param.get("defaultValue");
                            if (name != null && defaultValue != null) {
                                mergedArgs.put(name.toString(), defaultValue);
                            }
                        }
                        log.debug("工具 [{}] initParams 默认值已注入: {}", binding.getToolName(), mergedArgs.keySet());
                    }
                } catch (Exception e) {
                    log.warn("解析工具 initParams 失败，忽略默认值注入: toolId={}", toolId, e);
                }
            }
        }

        // 3b. 用 nodeInputs（前端绑定的实际值）覆盖，inputSchema 参数优先
        Map<String, Object> inputs = context.getNodeInputs();
        if (inputs != null && !inputs.isEmpty()) {
            mergedArgs.putAll(inputs);
        }

        String argumentsStr = mergedArgs.isEmpty() ? "{}" : JSONUtil.toJsonStr(mergedArgs);

        // 4. 执行工具
        log.info("开始独立执行工具 [{}], 参数: {}", binding.getToolName(), argumentsStr);
        String resultStr;
        try {
            resultStr = binding.getExecutor().execute(argumentsStr);
            log.info("独立工具 [{}] 执行成功", binding.getToolName());
        } catch (Exception e) {
            log.error("独立执行工具 [{}] 时发生异常", binding.getToolName(), e);
            throw new RuntimeException("工具执行失败: " + e.getMessage(), e);
        }

        // 5. 将结果放入输出
        Object parsedResult;
        if (StrUtil.isNotBlank(resultStr)) {
            if (JSONUtil.isTypeJSONObject(resultStr)) {
                parsedResult = JSONUtil.parseObj(resultStr);
                // 自动将 JSON Object 的所有顶层 key 拍平放入 NodeOutput，便于工作流引擎与自定义出参绑定映射
                ((JSONObject) parsedResult).forEach((k, v) -> output.addOutput(k, v));
            } else if (JSONUtil.isTypeJSONArray(resultStr)) {
                parsedResult = JSONUtil.parseArray(resultStr);
            } else {
                parsedResult = resultStr;
            }
        } else {
            parsedResult = "";
        }
        
        // 仅在未冲突时提供默认输出 (防止覆盖脚本显式返回的同名 key)
        if (!output.getOutputs().containsKey("result")) {
            output.addOutput("result", parsedResult);
        }
        if (!output.getOutputs().containsKey("text")) {
            output.addOutput("text", resultStr);
        }

        return output;
    }

    @Override
    public String getNodeType() {
        return "TOOL";
    }

    @Override
    public String getNodeName() {
        return "工具节点";
    }
}
