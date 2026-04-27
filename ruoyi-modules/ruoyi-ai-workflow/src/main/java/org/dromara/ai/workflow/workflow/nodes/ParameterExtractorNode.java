package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.workflow.workflow.core.AbstractAiWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数提取器节点
 * <p>
 * 节点类型标识: PARAMETER_EXTRACTOR
 * <p>
 * 使用大型语言模型将非结构化文本智能转换为结构化 JSON 数据。
 * <p>
 * 配置项 (来自节点 config):
 * - modelId (Long): 用于提取的 LLM 模型 ID
 * - parameters (List): 参数定义列表，每项包含 name/type/description/required
 * - extractionInstructions (String): 提取指令，描述如何提取参数
 * <p>
 * 输入:
 * - inputText (String): 待提取参数的文本内容
 * <p>
 * 输出:
 * - 每个定义的参数名作为独立的输出 key
 * - extractedJson (String): 完整的提取结果 JSON 字符串
 *
 * @author Mahone
 * @date 2026-05-01
 */
@Slf4j
@Component("PARAMETER_EXTRACTOR")
public class ParameterExtractorNode extends AbstractAiWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 PARAMETER_EXTRACTOR 节点");

        // 1. 获取输入文本
        String inputText = (String) context.getInput("inputText");
        if (StrUtil.isBlank(inputText)) {
            throw new IllegalArgumentException("PARAMETER_EXTRACTOR 节点缺少输入参数: inputText");
        }

        // 2. 读取参数定义列表
        Object parametersObj = context.getConfig("parameters");
        List<Map<String, Object>> parameters = parseParameterList(parametersObj);
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("PARAMETER_EXTRACTOR 节点未配置任何参数定义");
        }

        // 3. 读取提取指令
        String extractionInstructions = context.getConfigAsString("extractionInstructions", "");

        // 4. 加载模型
        Object[] mp = loadModelAndProvider(context);
        KmModel model = (KmModel) mp[0];
        KmModelProvider provider = (KmModelProvider) mp[1];

        // 5. 构建系统提示词
        String systemPrompt = buildExtractionSystemPrompt(parameters, extractionInstructions);

        // 6. 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(UserMessage.from(inputText));

        log.info("PARAMETER_EXTRACTOR 开始调用模型提取参数, 参数数量={}", parameters.size());

        // 7. 调用模型（使用阻塞模式，确保获取完整 JSON）
        AiConfig aiConfig = readAiConfig(context);
        // 参数提取强制使用非流式，确保 JSON 完整性
        aiConfig.setStreamOutput(false);
        ChatResponse response = callModel(messages, context, model, provider, aiConfig);

        String responseText = response.aiMessage().text();
        log.info("PARAMETER_EXTRACTOR 模型响应: {}", responseText);

        // 8. 解析 JSON 响应
        JSONObject extracted = parseJsonResponse(responseText);

        // 9. 构建输出
        NodeOutput output = new NodeOutput();
        output.addOutput("extractedJson", extracted.toString());

        // 将每个参数单独输出
        for (Map<String, Object> param : parameters) {
            String paramName = (String) param.get("name");
            if (StrUtil.isBlank(paramName)) continue;

            Object value = extracted.get(paramName);
            output.addOutput(paramName, value);
            log.info("PARAMETER_EXTRACTOR 提取参数: {}={}", paramName, value);
        }

        // 记录 token 使用
        Map<String, Object> tokenUsage = context.getTokenUsage();
        if (tokenUsage != null) {
            output.addOutput("tokenUsage", tokenUsage);
        }

        log.info("PARAMETER_EXTRACTOR 节点执行完成");
        return output;
    }

    /**
     * 构建参数提取的系统提示词
     */
    private String buildExtractionSystemPrompt(List<Map<String, Object>> parameters, String instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个参数提取助手。请从用户提供的文本中提取以下参数，并以 JSON 格式返回结果。\n\n");

        if (StrUtil.isNotBlank(instructions)) {
            sb.append("提取指令：\n").append(instructions).append("\n\n");
        }

        sb.append("需要提取的参数：\n");
        for (Map<String, Object> param : parameters) {
            String name = (String) param.get("name");
            String type = (String) param.getOrDefault("type", "string");
            String description = (String) param.getOrDefault("description", "");
            Object required = param.getOrDefault("required", false);

            sb.append("- ").append(name)
                    .append(" (类型: ").append(type).append(")")
                    .append(Boolean.TRUE.equals(required) ? " [必填]" : " [可选]");
            if (StrUtil.isNotBlank(description)) {
                sb.append(": ").append(description);
            }
            sb.append("\n");
        }

        sb.append("\n输出要求：\n");
        sb.append("1. 只返回 JSON 对象，不要有任何其他文字说明\n");
        sb.append("2. JSON 的 key 必须与上面定义的参数名完全一致\n");
        sb.append("3. 如果某个可选参数在文本中未找到，将其值设为 null\n");
        sb.append("4. 数据类型要与参数定义一致（string/number/boolean/array/object）\n");
        sb.append("5. 示例格式：{\"param1\": \"value1\", \"param2\": 123}");

        return sb.toString();
    }

    /**
     * 解析模型返回的 JSON 响应，支持 markdown 代码块格式
     */
    private JSONObject parseJsonResponse(String responseText) {
        if (StrUtil.isBlank(responseText)) {
            return new JSONObject();
        }

        String jsonStr = responseText.trim();

        // 尝试从 markdown 代码块中提取 JSON
        if (jsonStr.contains("```json")) {
            Matcher m = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```").matcher(jsonStr);
            if (m.find()) {
                jsonStr = m.group(1).trim();
            }
        } else if (jsonStr.contains("```")) {
            Matcher m = Pattern.compile("```\\s*([\\s\\S]*?)\\s*```").matcher(jsonStr);
            if (m.find()) {
                jsonStr = m.group(1).trim();
            }
        }

        // 尝试找到第一个 { 到最后一个 } 之间的内容
        int start = jsonStr.indexOf('{');
        int end = jsonStr.lastIndexOf('}');
        if (start >= 0 && end > start) {
            jsonStr = jsonStr.substring(start, end + 1);
        }

        try {
            if (JSONUtil.isTypeJSONObject(jsonStr)) {
                return JSONUtil.parseObj(jsonStr);
            }
        } catch (Exception e) {
            log.warn("PARAMETER_EXTRACTOR 解析 JSON 响应失败: {}", e.getMessage());
        }

        log.warn("PARAMETER_EXTRACTOR 无法解析为 JSON，返回空对象。原始响应: {}", responseText);
        return new JSONObject();
    }

    /**
     * 解析参数定义列表（兼容 List<Map> 格式）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseParameterList(Object parametersObj) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (parametersObj instanceof List) {
            for (Object item : (List<?>) parametersObj) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
        }
        return result;
    }

    @Override
    public String getNodeType() {
        return "PARAMETER_EXTRACTOR";
    }

    @Override
    public String getNodeName() {
        return "参数提取器";
    }
}
