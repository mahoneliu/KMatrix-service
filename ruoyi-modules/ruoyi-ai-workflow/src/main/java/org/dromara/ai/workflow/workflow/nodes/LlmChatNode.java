package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.io.IOException;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.execution.core.IToolProvider;

import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.core.ToolExecutionDispatcher;
import org.dromara.ai.execution.core.ToolResult;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.api.enums.SseEventType;
import org.dromara.ai.knowledge.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.workflow.constant.MediaTypeConstants;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractAiWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import org.dromara.common.core.utils.MessageUtils;

/**
 * LLM对话节点
 * 调用大语言模型进行对话，支持历史对话上下文
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component(NodeTypeConstants.LLM_CHAT)
public class LlmChatNode extends AbstractAiWorkflowNode {

    @Autowired
    private IToolProvider toolProviderService;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        try {
            return doExecute(context);
        } catch (Throwable t) {
            log.error("Exception in LlmChatNode: {}", t.getMessage(), t);
            throw t;
        }
    }

    private NodeOutput doExecute(NodeContext context) throws Exception {
        log.info("Executing LLM_CHAT node");

        NodeOutput output = new NodeOutput();

        // 读取 AI 配置（require_ai_config 面板）
        AiConfig aiConfig = readAiConfig(context);
        Double temperature = aiConfig.getTemperature();
        Integer maxTokens = aiConfig.getMaxTokens();
        Boolean streamOutput = aiConfig.isStreamOutput();

        // 读取对话配置（require_dialog_config 面板）
        DialogConfig dialogConfig = readDialogConfig(context);

        // systemPrompt 支持从 inputs 动态获取，也支持从 config 静态配置
        String systemPrompt = (String) context.getInput(NodeIOConstants.INPUT_SYSTEM_PROMPT);
        if (systemPrompt == null) {
            systemPrompt = context.getConfigAsString(NodeConfigConstants.CFG_DIALOG_SYSTEM_PROMPT);
        }

        // 加载模型（基类统一处理）
        Object[] modelAndProvider = loadModelAndProvider(context);
        KmModel model = (KmModel) modelAndProvider[0];
        KmModelProvider provider = (KmModelProvider) modelAndProvider[1];

        String userInput = (String) context.getInput(NodeIOConstants.INPUT_USER_INPUT);
        if (userInput == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.llm.missing_user_input"));
        }

        // 构建消息列表（基类统一处理：systemPrompt → 历史消息 → 当前用户消息含多模态）
        List<ChatMessage> messages = buildChatMessages(context, userInput, systemPrompt, dialogConfig);
        log.info("LLM_CHAT节点 - 消息构建完成, 总消息数={}", messages.size());

        SseEmitter emitter = context.getSseEmitter();

        // 尝试从输入参数 retrievedDocs 获取引用信息
        Object retrievedDocsObj = context.getInput(NodeIOConstants.INPUT_RETRIEVED_DOCS);
        if (retrievedDocsObj instanceof List && emitter != null) {
            try {
                List<?> list = (List<?>) retrievedDocsObj;
                List<Map<String, Object>> citations = new ArrayList<>();

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    Map<String, Object> citation = null;

                    // 直接从 KmRetrievalResultVo 对象构建 citation
                    if (item instanceof KmRetrievalResultVo) {
                        KmRetrievalResultVo vo = (KmRetrievalResultVo) item;
                        citation = new HashMap<>();
                        citation.put("index", i + 1);
                        citation.put("chunkId", vo.getChunkId());
                        citation.put("documentId", vo.getDocumentId());
                        citation.put("documentName", vo.getDocumentName());
                        citation.put("content", vo.getContent());
                        citation.put("score", vo.getScore());
                    } else if (item instanceof Map) {
                        // 兼容 Map 类型(序列化/反序列化场景)
                        Map<?, ?> map = (Map<?, ?>) item;
                        citation = new HashMap<>();
                        citation.put("index", i + 1);
                        citation.put("chunkId", map.get("chunkId"));
                        citation.put("documentId", map.get("documentId"));
                        citation.put("documentName", map.get("documentName"));
                        citation.put("content", map.get("content"));
                        citation.put("score", map.get("score"));
                    }

                    if (citation != null) {
                        citations.add(citation);
                    }
                }

                if (!citations.isEmpty()) {
                    Map<String, Object> citationData = new HashMap<>();
                    citationData.put("nodeId", context.getNodeId());
                    citationData.put("nodeName", getNodeName());
                    citationData.put("citations", citations);

                    emitter.send(SseEmitter.event()
                            .name(SseEventType.CITATION.getEventName())
                            .data(citationData));
                    log.info("LLM_CHAT节点发送引用事件成功, 引用数量: {}", citations.size());
                }
            } catch (Exception e) {
                log.error("LLM_CHAT节点发送引用事件失败", e);
            }
        }

        // 解析并绑定工具
        List<Map<String, Object>> toolRefs = new ArrayList<>();

        // 1. 兼容旧的 tools 配置
        Object toolsObj = context.getConfig(NodeConfigConstants.CFG_LLM_TOOLS);
        if (toolsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> legacyTools = (List<Map<String, Object>>) toolsObj;
            toolRefs.addAll(legacyTools);
        }

        // 2. 处理 builtinToolIds
        Object builtinIdsObj = context.getConfig(NodeConfigConstants.CFG_LLM_BUILTIN_TOOL_IDS);
        if (builtinIdsObj instanceof List) {
            List<?> builtinIds = (List<?>) builtinIdsObj;
            for (Object id : builtinIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", NodeConfigConstants.CFG_TOOL_TYPE_BUILTIN);
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        // 3. 处理 mcpServerIds
        Object mcpIdsObj = context.getConfig(NodeConfigConstants.CFG_LLM_MCP_SERVER_IDS);
        if (mcpIdsObj instanceof List) {
            List<?> mcpIds = (List<?>) mcpIdsObj;
            for (Object id : mcpIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", NodeConfigConstants.CFG_TOOL_TYPE_MCP);
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        // 4. 处理 skillIds
        Object skillIdsObj = context.getConfig(NodeConfigConstants.CFG_LLM_SKILL_IDS);
        if (skillIdsObj instanceof List) {
            List<?> skillIds = (List<?>) skillIdsObj;
            for (Object id : skillIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", NodeConfigConstants.CFG_TOOL_TYPE_SKILL);
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        List<ToolBinding> toolBindings = toolProviderService.resolveBindings(toolRefs);
        List<ToolSpecification> toolSpecs = toolBindings.stream().map(ToolBinding::getSpecification).toList();
        Boolean enableToolTrace = context.getConfigAsBoolean(NodeConfigConstants.CFG_LLM_ENABLE_TOOL_TRACE, false);

        ChatModel chatModel = null;
        StreamingChatModel streamingModel = null;
        if (Boolean.TRUE.equals(streamOutput)) {
            streamingModel = modelBuilder.buildStreamingChatModel(model, provider.getProviderKey(), temperature,
                    maxTokens);
        } else {
            chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), temperature, maxTokens);
        }

        ChatResponse response = null;

        while (true) {
            if (Boolean.TRUE.equals(streamOutput)) {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
                AtomicReference<Exception> errorRef = new AtomicReference<>();

                StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        if (emitter != null) {
                            try {
                                emitter.send(SseEmitter.event().name(SseEventType.THINKING.getEventName()).data(token));
                            } catch (IOException e) {
                                log.error("发送SSE消息失败", e);
                            }
                        }
                    }

                    @Override
                    public void onPartialThinking(PartialThinking pt) {
                        if (emitter != null && pt.text() != null) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name(SseEventType.THINKING.getEventName())
                                        .data(pt.text()));
                            } catch (Exception e) {
                                log.error("发送THINKING消息失败", e);
                            }
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse r) {
                        responseRef.set(r);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorRef.set(new RuntimeException(error));
                        latch.countDown();
                    }
                };

                if (toolSpecs.isEmpty()) {
                    streamingModel.chat(messages, handler);
                } else {
                    ChatRequest request = ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(toolSpecs)
                            .build();
                    streamingModel.chat(request, handler);
                }

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(MessageUtils.message("ai.workflow.node.llm.interrupted"), e);
                }

                if (errorRef.get() != null) {
                    throw errorRef.get();
                }

                response = responseRef.get();
            } else {
                if (toolSpecs.isEmpty()) {
                    response = chatModel.chat(messages);
                } else {
                    ChatRequest request = ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(toolSpecs)
                            .build();
                    response = chatModel.chat(request);
                }
            }

            AiMessage aiMessage = response.aiMessage();
            messages.add(aiMessage);
            log.info("LLM_CHAT节点 - IMPORTANT - : aiMessage={}", aiMessage);

            List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();

            // ================== 增加 JSON 回退（Fallback）解析机制 ==================
            // 有些大模型即使支持工具，也可能不走底层的 toolRequests 而是以普通 JSON 文本的格式输出
            if ((toolRequests == null || toolRequests.isEmpty()) && !toolSpecs.isEmpty()
                    && StrUtil.isNotBlank(aiMessage.text())) {
                toolRequests = parseFallbackToolRequests(aiMessage.text(), toolSpecs);
                if (toolRequests != null && !toolRequests.isEmpty()) {
                    log.info("LLM_CHAT节点 - 触发Fallback解析, 成功从纯文本中提取了ToolExecutionRequest: {}", toolRequests);
                    // 为了保证 Langchain4j 历史消息结构正确，我们将这个带有解析结果的全新 AiMessage 替换进入 message 队列
                    messages.remove(messages.size() - 1);
                    AiMessage fallbackAiMsg = AiMessage.from(aiMessage.text(), toolRequests);
                    messages.add(fallbackAiMsg);
                }
            }
            // ====================================================================

            if (toolRequests != null && !toolRequests.isEmpty()) {
                log.info("LLM_CHAT节点 - : toolExecutionRequests={}", toolRequests);
                for (ToolExecutionRequest toolExecutionRequest : toolRequests) {
                    if (Boolean.TRUE.equals(enableToolTrace) && emitter != null) {
                        try {
                            Map<String, Object> traceData = new HashMap<>();
                            traceData.put("type", "tool_call_start");
                            traceData.put("toolName", toolExecutionRequest.name());
                            traceData.put("arguments", toolExecutionRequest.arguments());
                            emitter.send(SseEmitter.event().name(SseEventType.TOOL_TRACE.getEventName()).data(traceData));
                        } catch (IOException e) {
                            log.error("发送工具追踪SSE事件失败", e);
                        }
                    }

                    // 使用 dispatchForResult 获取富媒体结果
                    ToolResult toolResult = ToolExecutionDispatcher
                            .dispatchForResult(toolExecutionRequest, toolBindings);

                    // 构建工具回执文本
                    String resultText = toolResult.getText() != null ? toolResult.getText()
                            : "Success with empty result";

                    // 如果工具返回了富媒体内容（图片等），提取图片URL附加到文本中
                    if (toolResult.hasContents()) {
                        StringBuilder richText = new StringBuilder(resultText);
                        for (Content c : toolResult.getContents()) {
                            if (c instanceof ImageContent ic) {
                                // 优先使用 URL，其次处理 base64 数据（此处仅记录提示，后续可扩展为上传OSS）
                                String imageUrl = null;
                                if (ic.image().url() != null) {
                                    imageUrl = ic.image().url().toString();
                                } else if (ic.image().base64Data() != null) {
                                    // base64 图片 → 构造 data URL 传给支持的模型
                                    String mimeType = ic.image().mimeType() != null ? ic.image().mimeType()
                                            : MediaTypeConstants.MIME_IMAGE_PNG;
                                    imageUrl = "data:" + mimeType + ";base64," + ic.image().base64Data();
                                    log.info("LLM_CHAT节点 - 工具返回了base64图片内容, mimeType={}", mimeType);
                                }
                                if (imageUrl != null) {
                                    richText.append("\n[图片]: ").append(imageUrl);
                                }
                            }
                        }
                        resultText = richText.toString();
                    }

                    ToolExecutionResultMessage toolResultMsg = ToolExecutionResultMessage.from(toolExecutionRequest,
                            resultText);
                    messages.add(toolResultMsg);

                    if (Boolean.TRUE.equals(enableToolTrace) && emitter != null) {
                        try {
                            Map<String, Object> traceData = new HashMap<>();
                            traceData.put("type", "tool_call_result");
                            traceData.put("toolName", toolExecutionRequest.name());
                            traceData.put("result", resultText);
                            traceData.put("hasRichContent", toolResult.hasContents());
                            emitter.send(SseEmitter.event().name(SseEventType.TOOL_TRACE.getEventName()).data(traceData));
                        } catch (IOException e) {
                            log.error("发送工具追踪结果SSE事件失败", e);
                        }
                    }
                }
            } else {
                break; // 无需执行工具，跳出循环
            }
        }

        // 获取并记录 token 使用情况（基类统一处理）
        Map<String, Object> tokenUsageMap = recordTokenUsage(response, context);
        if (tokenUsageMap != null) {
            output.addOutput(NodeIOConstants.OUTPUT_TOKEN_USAGE, tokenUsageMap);
        }

        // 保存输出
        AiMessage aiMessage = response.aiMessage();
        String responseText = aiMessage.text();
        log.info("LLM_CHAT节点执行完成, response={}", responseText);
        output.addOutput(NodeIOConstants.OUTPUT_RESPONSE, responseText);
        if (aiMessage.thinking() != null) {
            output.addOutput(NodeIOConstants.OUTPUT_REASONING_CONTENT, aiMessage.thinking());
        }
        context.setGlobalValue(NodeIOConstants.GLOBAL_AI_RESPONSE, responseText);

        return output;
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.LLM_CHAT;
    }

    @Override
    public String getNodeName() {
        return "LLM对话";
    }

    /**
     * Fallback 工具调用解析器
     * 如果大模型未走官方 Tool 函数格式返回，而是直接在文本里返回 JSON，则尝试从文本中解析出来。
     */
    private List<ToolExecutionRequest> parseFallbackToolRequests(String text, List<ToolSpecification> toolSpecs) {
        try {
            String jsonStr = text;
            if (text.contains("```json")) {
                Matcher m = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```")
                        .matcher(text);
                if (m.find()) {
                    jsonStr = m.group(1);
                }
            } else if (text.contains("```")) {
                Matcher m = Pattern.compile("```\\s*([\\s\\S]*?)\\s*```").matcher(text);
                if (m.find()) {
                    jsonStr = m.group(1);
                }
            }

            jsonStr = jsonStr.trim();

            if (JSONUtil.isTypeJSONObject(jsonStr)) {
                JSONObject jsonObj = JSONUtil.parseObj(jsonStr);
                String toolName = jsonObj.getStr("name");
                Object args = jsonObj.get("arguments");

                // 常见的非标准回退格式： {"tool": "weather", "parameters": {"city": "beijing"}}
                if (toolName == null) {
                    toolName = jsonObj.getStr("tool");
                    args = jsonObj.get("parameters");
                }

                if (toolName != null) {
                    String finalToolName = toolName;
                    boolean exists = toolSpecs.stream().anyMatch(spec -> spec.name().equals(finalToolName));
                    if (exists) {
                        String argumentsStr = (args instanceof String) ? (String) args
                                : JSONUtil.toJsonStr(args);
                        if (argumentsStr == null) {
                            argumentsStr = "{}";
                        }
                        return Collections.singletonList(
                                ToolExecutionRequest.builder()
                                        .id("call_" + System.currentTimeMillis())
                                        .name(toolName)
                                        .arguments(argumentsStr)
                                        .build());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Fallback提取工具调用失败: {}", e.getMessage());
        }
        return null;
    }

}
