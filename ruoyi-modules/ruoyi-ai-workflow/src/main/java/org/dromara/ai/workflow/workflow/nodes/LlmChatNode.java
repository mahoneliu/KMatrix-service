package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.output.TokenUsage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.api.enums.SseEventType;
import org.dromara.ai.knowledge.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.workflow.workflow.nodes.chat.IChatMessageProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.tool.ToolBinding;
import org.dromara.ai.workflow.workflow.nodes.tool.ToolExecutionDispatcher;
import org.dromara.ai.workflow.workflow.nodes.tool.IToolProvider;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.AudioContent;
import org.springframework.beans.factory.ObjectProvider;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowNodeUtils;

/**
 * LLM对话节点
 * 调用大语言模型进行对话，支持历史对话上下文
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component("LLM_CHAT")
public class LlmChatNode extends AbstractWorkflowNode {

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;
    private final ObjectProvider<IChatMessageProvider> chatMessageProvider;
    private final IToolProvider toolProviderService;
    private final WorkflowNodeUtils workflowNodeUtils;

    /** 默认历史消息条数限制 */
    private static final int DEFAULT_HISTORY_LIMIT = 10;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        try {
            return doExecute(context);
        } catch (Throwable t) {
            log.error("LlmChatNode 执行发生异常: {}", t.getMessage(), t);
            throw t;
        }
    }

    private NodeOutput doExecute(NodeContext context) throws Exception {
        log.info("执行LLM_CHAT节点");

        NodeOutput output = new NodeOutput();

        // 从配置获取固定参数
        Long modelId = context.getConfigAsLong("modelId");

        // 获取大模型参数配置（可选）
        Double temperature = context.getConfigAsDouble("temperature", null);
        Integer maxTokens = context.getConfigAsInteger("maxTokens", null);
        Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);

        // 历史对话配置
        Boolean historyEnabled = context.getConfigAsBoolean("historyEnabled", false);
        Integer historyLimit = context.getConfigAsInteger("historyLimit", DEFAULT_HISTORY_LIMIT);

        // systemPrompt支持从inputs动态获取，也支持从config静态配置
        String systemPrompt = (String) context.getInput("systemPrompt");
        if (systemPrompt == null) {
            systemPrompt = context.getConfigAsString("systemPrompt");
        }

        // userPrompt支持从inputs动态获取,也支持从config静态配置
        String userPrompt = (String) context.getInput("userPrompt");
        if (userPrompt == null) {
            userPrompt = context.getConfigAsString("userPrompt");
        }

        // 加载模型
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.common.model_not_found", modelId));
        }

        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.llm.provider_not_found", model.getProviderId()));
        }

        String userInput = (String) context.getInput("userInput");
        if (userInput == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.llm.missing_user_input"));
        }
        String chatContext = (String) context.getInput("chatContext");
        log.info("LLM_CHAT节点 - : chatContext={}", chatContext);

        // 获取会话ID用于加载历史对话
        Long sessionId = context.getSessionId();

        // 收集待发送的文件 (优先级: files > ossIds > 全局 _files)
        List<KmWorkflowFile> workflowFiles = new ArrayList<>();
        Boolean enableMultimodal = context.getConfigAsBoolean("enableMultimodal", false);

        if (Boolean.TRUE.equals(enableMultimodal)) {
            // 1. 优先尝试提取 inputs 中的 files
            Object inputFiles = context.getInput("files");
            if (inputFiles instanceof List) {
                for (Object item : (List<?>) inputFiles) {
                    if (item instanceof KmWorkflowFile) {
                        workflowFiles.add((KmWorkflowFile) item);
                    } else if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        KmWorkflowFile wf = new KmWorkflowFile();
                        wf.setUrl((String) map.get("url"));
                        wf.setType((String) map.get("type"));
                        wf.setOssId(
                                map.get("ossId") instanceof Number ? ((Number) map.get("ossId")).longValue() : null);
                        wf.setTempFileId(
                                map.get("tempFileId") instanceof Number ? ((Number) map.get("tempFileId")).longValue()
                                        : null);
                        wf.setName((String) map.get("name"));
                        workflowFiles.add(wf);
                    }
                }
            } else if (inputFiles instanceof KmWorkflowFile) {
                workflowFiles.add((KmWorkflowFile) inputFiles);
            }

            // 2. 如果无 files，尝试提取 inputs/config 中的 ossIds
            if (workflowFiles.isEmpty()) {
                Object inputOssIds = context.getInput("ossIds");
                if (inputOssIds == null) {
                    inputOssIds = context.getInput("ossId");
                }
                if (inputOssIds != null) {
                    List<Object> idList = new ArrayList<>();
                    if (inputOssIds instanceof List) {
                        idList.addAll((List<?>) inputOssIds);
                    } else {
                        idList.add(inputOssIds);
                    }
                    for (Object idObj : idList) {
                        try {
                            Long idVal = idObj instanceof Number ? ((Number) idObj).longValue()
                                    : Long.parseLong(idObj.toString());
                            KmWorkflowFile wf = new KmWorkflowFile();
                            wf.setOssId(idVal);
                            wf.setType("image"); // 降级默认为图片
                            workflowFiles.add(wf);
                        } catch (Exception e) {
                            log.warn("LlmChatNode 多模态解析 ossId 失败: {}", idObj);
                        }
                    }
                }
            }

            // 3. 最后兜底兼容全局的初始上传文件
            if (workflowFiles.isEmpty()) {
                Object globalFiles = context.getGlobalValue("files");
                if (globalFiles instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<KmWorkflowFile> list = (List<KmWorkflowFile>) globalFiles;
                    workflowFiles.addAll(list);
                }
            }
        }

        // 构建消息列表（包含历史对话和文件）
        List<ChatMessage> messages = buildMessages(userInput, systemPrompt, userPrompt, sessionId, historyEnabled,
                historyLimit, chatContext, workflowFiles, enableMultimodal);
        log.info(
                "LLM_CHAT节点 - : chatContext={}, userInput={}, userPrompt={}, systemPrompt={},historyEnabled={}, historyLimit={}, sessionId={}, 历史消息总数={}, 多模态={}",
                chatContext, userInput, userPrompt, systemPrompt, historyEnabled, historyLimit, sessionId,
                messages.size(), enableMultimodal);

        SseEmitter emitter = context.getSseEmitter();

        // 尝试从输入参数 retrievedDocs 获取引用信息
        Object retrievedDocsObj = context.getInput("retrievedDocs");
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

        // 处理 apiBase 并设置回 model 对象以便 ModelBuilder 使用
        String apiBase = StrUtil.isNotBlank(model.getApiBase()) ? model.getApiBase() : provider.getDefaultEndpoint();
        model.setApiBase(apiBase);

        // 解析并绑定工具
        List<Map<String, Object>> toolRefs = new ArrayList<>();

        // 1. 兼容旧的 tools 配置
        Object toolsObj = context.getConfig("tools");
        if (toolsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> legacyTools = (List<Map<String, Object>>) toolsObj;
            toolRefs.addAll(legacyTools);
        }

        // 2. 处理 builtinToolIds
        Object builtinIdsObj = context.getConfig("builtinToolIds");
        if (builtinIdsObj instanceof List) {
            List<?> builtinIds = (List<?>) builtinIdsObj;
            for (Object id : builtinIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", "builtin");
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        // 3. 处理 mcpServerIds
        Object mcpIdsObj = context.getConfig("mcpServerIds");
        if (mcpIdsObj instanceof List) {
            List<?> mcpIds = (List<?>) mcpIdsObj;
            for (Object id : mcpIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", "mcp");
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        // 4. 处理 skillIds
        Object skillIdsObj = context.getConfig("skillIds");
        if (skillIdsObj instanceof List) {
            List<?> skillIds = (List<?>) skillIdsObj;
            for (Object id : skillIds) {
                Map<String, Object> ref = new HashMap<>();
                ref.put("type", "skill");
                ref.put("id", id);
                toolRefs.add(ref);
            }
        }

        List<ToolBinding> toolBindings = toolProviderService.resolveBindings(toolRefs);
        List<ToolSpecification> toolSpecs = toolBindings.stream().map(ToolBinding::getSpecification).toList();
        Boolean enableToolTrace = context.getConfigAsBoolean("enableToolTrace", false);

        ChatModel chatModel = null;
        StreamingChatModel streamingModel = null;
        if (Boolean.TRUE.equals(streamOutput)) {
            streamingModel = modelBuilder.buildStreamingChatModel(model, provider.getProviderKey(), temperature,
                    maxTokens);
        } else {
            chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), temperature, maxTokens);
        }

        dev.langchain4j.model.chat.response.ChatResponse response = null;

        while (true) {
            if (Boolean.TRUE.equals(streamOutput)) {
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<dev.langchain4j.model.chat.response.ChatResponse> responseRef = new AtomicReference<>();
                AtomicReference<Exception> errorRef = new AtomicReference<>();

                dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler = new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        if (emitter != null) {
                            try {
                                emitter.send(SseEmitter.event().data(token));
                            } catch (IOException e) {
                                log.error("发送SSE消息失败", e);
                            }
                        }
                    }

                    @Override
                    public void onPartialThinking(dev.langchain4j.model.chat.response.PartialThinking pt) {
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
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse r) {
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
                            emitter.send(SseEmitter.event().name("TOOL_TRACE").data(traceData));
                        } catch (IOException e) {
                            log.error("发送工具追踪SSE事件失败", e);
                        }
                    }

                    ToolExecutionResultMessage toolResult = ToolExecutionDispatcher.dispatch(toolExecutionRequest,
                            toolBindings);
                    messages.add(toolResult);

                    if (Boolean.TRUE.equals(enableToolTrace) && emitter != null) {
                        try {
                            Map<String, Object> traceData = new HashMap<>();
                            traceData.put("type", "tool_call_result");
                            traceData.put("toolName", toolExecutionRequest.name());
                            traceData.put("result", toolResult.text());
                            emitter.send(SseEmitter.event().name("TOOL_TRACE").data(traceData));
                        } catch (IOException e) {
                            log.error("发送工具追踪结果SSE事件失败", e);
                        }
                    }
                }
            } else {
                break; // 无需执行工具，跳出循环
            }
        }

        // 获取并记录 token 使用情况
        if (response != null && response.tokenUsage() != null) {
            TokenUsage tokenUsage = response.tokenUsage();

            // 保存到 NodeContext
            Map<String, Object> tokenUsageMap = new HashMap<>();
            tokenUsageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
            tokenUsageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
            tokenUsageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
            context.setTokenUsage(tokenUsageMap);

            // 添加到节点输出
            output.addOutput("tokenUsage", tokenUsageMap);

            log.info("LLM_CHAT节点 Token使用: input={}, output={}, total={}",
                    tokenUsage.inputTokenCount(),
                    tokenUsage.outputTokenCount(),
                    tokenUsage.totalTokenCount());
        }

        // 保存输出
        AiMessage aiMessage = response.aiMessage();
        String responseText = aiMessage.text();
        log.info("LLM_CHAT节点执行完成, response={}", responseText);
        output.addOutput("response", responseText);
        context.setGlobalValue("aiResponse", responseText);

        return output;
    }

    /**
     * 构建消息列表（包含历史对话）
     *
     * @param userInput      当前用户输入
     * @param systemPrompt   系统提示词
     * @param userPrompt     用户提示词(配置的具体问题)
     * @param sessionId      会话ID
     * @param historyEnabled 是否启用历史对话
     * @param historyLimit   历史消息条数限制
     * @return 完整的消息列表
     */
    private List<ChatMessage> buildMessages(String userInput, String systemPrompt, String userPrompt,
            Long sessionId, Boolean historyEnabled, Integer historyLimit, String chatContext,
            List<KmWorkflowFile> files, Boolean enableMultimodal) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 加载并添加历史对话
        if (Boolean.TRUE.equals(historyEnabled) && sessionId != null) {
            IChatMessageProvider provider = chatMessageProvider.getIfAvailable();
            if (provider != null) {
                List<ChatMessage> historyMessages = provider.loadHistoryMessages(sessionId, historyLimit);
                messages.addAll(historyMessages);
                log.debug("加载历史对话: sessionId={}, 条数={}", sessionId, historyMessages.size());
            }
        }

        // 3. 添加当前用户消息
        String contextPrefix = "";
        if (chatContext != null && !chatContext.isEmpty()) {
            contextPrefix += "已知信息：" + chatContext + "\n";
        }
        if (userPrompt != null && !userPrompt.isEmpty()) {
            contextPrefix += userPrompt + "\n";
        }

        boolean processed = false;
        if (Boolean.TRUE.equals(enableMultimodal) && JSONUtil.isTypeJSONArray(userInput)) {
            try {
                cn.hutool.json.JSONArray array = JSONUtil.parseArray(userInput);
                // 检查是否包含多模态标识（至少有一个元素包含 type 字段）
                boolean hasMultimodal = false;
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item != null) {
                        JSONObject obj = JSONUtil.parseObj(item);
                        if (obj.containsKey("type")) {
                            hasMultimodal = true;
                            break;
                        }
                    }
                }

                if (hasMultimodal) {
                    StringBuilder textPart = new StringBuilder(contextPrefix);
                    List<Content> contents = new ArrayList<>();

                    for (int i = 0; i < array.size(); i++) {
                        Object item = array.get(i);
                        if (item == null)
                            continue;
                        JSONObject obj = JSONUtil.parseObj(item);
                        String type = obj.getStr("type");
                        if ("text".equals(type)) {
                            textPart.append(obj.getStr("text"));
                        } else if ("image".equals(type)) {
                            if (textPart.length() > 0) {
                                contents.add(TextContent.from(textPart.toString()));
                                textPart.setLength(0);
                            }
                            String fileIdRef = obj.getStr("tempFileId");
                            if (StrUtil.isBlank(fileIdRef) || "undefined".equals(fileIdRef)) {
                                fileIdRef = obj.getStr("ossId");
                            }
                            if ("undefined".equals(fileIdRef)) {
                                fileIdRef = null;
                            }
                            String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, obj.getStr("url"),
                                    "image/jpeg");
                            if (StrUtil.isNotBlank(url)) {
                                contents.add(ImageContent.from(url));
                            }
                        } else if ("audio".equals(type)) {
                            if (textPart.length() > 0) {
                                contents.add(TextContent.from(textPart.toString()));
                                textPart.setLength(0);
                            }
                            String fileIdRef = obj.getStr("tempFileId");
                            if (StrUtil.isBlank(fileIdRef) || "undefined".equals(fileIdRef)) {
                                fileIdRef = obj.getStr("ossId");
                            }
                            if ("undefined".equals(fileIdRef)) {
                                fileIdRef = null;
                            }
                            String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, obj.getStr("url"),
                                    "audio/mpeg");
                            try {
                                contents.add(AudioContent.from(url));
                            } catch (Throwable e) {
                                log.warn("LC4J当前版本可能不支持AudioContent: {}", e.getMessage());
                            }
                        } else if ("file".equals(type)) {
                            // 通用文件：将文件名作为文本提示，实际内容由 FILE_PARSE 节点处理
                            String fileName = obj.getStr("name");
                            if (StrUtil.isNotBlank(fileName)) {
                                textPart.append("[文件: ").append(fileName).append("]");
                            }
                        }
                    }

                    if (textPart.length() > 0) {
                        contents.add(TextContent.from(textPart.toString()));
                    }
                    if (!contents.isEmpty()) {
                        messages.add(UserMessage.from(contents));
                        processed = true;
                        log.info("LLM_CHAT节点 - : buildMessages JSON多模态转化成功, 内容元素数={}", contents.size());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 JSON 多模态输入失败，将作为普通文本处理: {}, msg: {}", e.getMessage(), userInput);
            }
        }

        // 4. 如果尚未处理（不是 JSON 多模态），则按普通文本或工作流文件列表处理
        if (!processed) {
            StringBuilder fullText = new StringBuilder(contextPrefix);
            fullText.append(userInput);

            List<Content> contents = new ArrayList<>();
            // 处理工作流流转的文件对象
            if (files != null && !files.isEmpty()) {
                for (KmWorkflowFile file : files) {
                    String fileIdRef = file.getTempFileId() != null
                            ? file.getTempFileId().toString()
                            : (file.getOssId() != null ? file.getOssId().toString() : null);
                    if ("image".equals(file.getType())) {
                        String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, file.getUrl(), "image/jpeg");
                        if (StrUtil.isNotBlank(url)) {
                            contents.add(ImageContent.from(url));
                        }
                    } else if ("audio".equals(file.getType())) {
                        String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, file.getUrl(), "audio/mpeg");
                        if (StrUtil.isNotBlank(url)) {
                            try {
                                contents.add(AudioContent.from(url));
                            } catch (Throwable e) {
                                log.warn("LC4J当前版本不支持音频内容: {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            if (!contents.isEmpty()) {
                contents.add(0, TextContent.from(fullText.toString()));
                messages.add(UserMessage.from(contents));
                log.info("LLM_CHAT节点 - : buildMessages 工作流文件多模态转化成功");
            } else {
                if (StrUtil.isBlank(fullText)) {
                    throw new RuntimeException(MessageUtils.message("ai.workflow.node.llm.missing_user_input"));
                }
                messages.add(UserMessage.from(fullText.toString()));
                log.info("LLM_CHAT节点 - : buildMessages 普通文本转化完成");
            }
        }

        return messages;
    }

    @Override
    public String getNodeType() {
        return "LLM_CHAT";
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
