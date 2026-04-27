package org.dromara.ai.workflow.workflow.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.api.enums.SseEventType;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.workflow.workflow.nodes.chat.IChatMessageProvider;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowNodeUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
/**
 * AI 节点抽象基类
 * <p>
 * 所有需要调用大模型的节点（require_ai_config = 1）继承此类。
 * 封装了两个配置模块对应的全部读取与处理逻辑：
 * <ul>
 *   <li><b>require_ai_config</b>：模型加载、流式/阻塞调用、token 统计</li>
 *   <li><b>require_dialog_config</b>：用户提示词、多模态文件收集、历史消息加载、消息列表构建</li>
 * </ul>
 *
 * <h3>子类使用示例</h3>
 * <pre>{@code
 * @Component("MY_AI_NODE")
 * public class MyAiNode extends AbstractAiWorkflowNode {
 *
 *     @Override
 *     public NodeOutput execute(NodeContext context) throws Exception {
 *         // 1. 读取配置
 *         AiConfig aiConfig = readAiConfig(context);
 *         DialogConfig dialogConfig = readDialogConfig(context);
 *
 *         // 2. 加载模型
 *         Object[] mp = loadModelAndProvider(context);
 *
 *         // 3. 构建消息（自动处理 userPrompt / 历史 / 多模态）
 *         String userInput = (String) context.getInput("userInput");
 *         List<ChatMessage> messages = buildChatMessages(context, userInput, systemPrompt, dialogConfig);
 *
 *         // 4. 调用模型（自动处理流式/阻塞 + token 统计）
 *         ChatResponse response = callModel(messages, context, (KmModel) mp[0], (KmModelProvider) mp[1], aiConfig);
 *
 *         // 5. 处理结果
 *         NodeOutput output = new NodeOutput();
 *         output.addOutput("response", response.aiMessage().text());
 *         return output;
 *     }
 * }
 * }</pre>
 *
 * @author Mahone
 * @date 2026-04-26
 */
@Slf4j
public abstract class AbstractAiWorkflowNode extends AbstractWorkflowNode {

    @Autowired
    protected KmModelMapper modelMapper;

    @Autowired
    protected KmModelProviderMapper providerMapper;

    @Autowired
    protected ModelBuilder modelBuilder;

    @Autowired(required = false)
    private ObjectProvider<IChatMessageProvider> chatMessageProviderHolder;

    @Autowired(required = false)
    private WorkflowNodeUtils workflowNodeUtils;

    // =========================================================================
    // 模板方法：标准对话类 AI 节点执行骨架
    // =========================================================================

    /**
     * 标准对话类 AI 节点的执行骨架（模板方法）
     * <p>
     * 自动完成：读取 AI 配置 → 读取对话配置 → 加载模型 → 构建消息列表 → 调用模型 → 写入 token 统计
     * <p>
     * 子节点只需实现三个差异点：
     * <ol>
     *   <li>{@link #getUserInput(NodeContext)} — 返回当前节点的用户输入文本</li>
     *   <li>{@link #buildSystemPrompt(NodeContext)} — 返回系统提示词（null 则不添加）</li>
     *   <li>{@link #processAiResponse(ChatResponse, NodeContext, NodeOutput)} — 处理模型响应，写入输出</li>
     * </ol>
     * <p>
     * <b>适用场景：</b>标准对话类节点（require_dialog_config = 1），如 INTENT_CLASSIFIER 等。
     * 对于有特殊消息构建逻辑的节点（如 LLM_CHAT 的工具调用循环），仍应覆盖 {@link #execute} 自行实现。
     */
    protected final NodeOutput executeWithDialogConfig(NodeContext context) throws Exception {
        // 1. 读取配置
        AiConfig aiConfig = readAiConfig(context);
        DialogConfig dialogConfig = readDialogConfig(context);

        // 2. 加载模型
        Object[] mp = loadModelAndProvider(context);
        KmModel model = (KmModel) mp[0];
        KmModelProvider provider = (KmModelProvider) mp[1];

        // 3. 获取用户输入 & 系统提示词（由子节点提供）
        String userInput = getUserInput(context);
        String systemPrompt = buildSystemPrompt(context);

        // 4. 构建消息列表（自动处理 userPrompt / 历史 / 多模态）
        List<ChatMessage> messages = buildChatMessages(context, userInput, systemPrompt, dialogConfig);

        // 5. 调用模型（自动处理流式/阻塞 + token 统计）
        ChatResponse response = callModel(messages, context, model, provider, aiConfig);

        // 6. 子节点处理响应
        NodeOutput output = new NodeOutput();
        Map<String, Object> tokenUsage = context.getTokenUsage();
        if (tokenUsage != null) {
            output.addOutput("tokenUsage", tokenUsage);
        }
        processAiResponse(response, context, output);

        return output;
    }

    /**
     * 返回当前节点的用户输入文本
     * <p>
     * 使用 {@link #executeWithDialogConfig} 的子节点必须实现此方法。
     * 例如：{@code return (String) context.getInput("userInput");}
     */
    protected String getUserInput(NodeContext context) {
        throw new UnsupportedOperationException(
                getNodeType() + " 使用了 executeWithDialogConfig 但未实现 getUserInput()");
    }

    /**
     * 构建系统提示词
     * <p>
     * 使用 {@link #executeWithDialogConfig} 的子节点可覆盖此方法提供自定义系统提示词。
     * 默认从 config 中读取 systemPrompt 字段，null 表示不添加系统提示。
     */
    protected String buildSystemPrompt(NodeContext context) {
        String systemPrompt = (String) context.getInput("systemPrompt");
        if (systemPrompt == null) {
            systemPrompt = context.getConfigAsString("systemPrompt");
        }
        return systemPrompt;
    }

    /**
     * 处理模型响应，将结果写入 NodeOutput
     * <p>
     * 使用 {@link #executeWithDialogConfig} 的子节点必须实现此方法。
     * token 统计已由框架自动写入 output，此方法只需处理业务输出。
     */
    protected void processAiResponse(ChatResponse response, NodeContext context, NodeOutput output) {
        throw new UnsupportedOperationException(
                getNodeType() + " 使用了 executeWithDialogConfig 但未实现 processAiResponse()");
    }

    // =========================================================================
    // 值对象
    // =========================================================================

    /**
     * AI 配置值对象，对应前端 AiConfigPanel（require_ai_config 面板）的配置项
     */
    @Data
    public static class AiConfig {
        /** 模型 ID */
        private Long modelId;
        /** 温度（null = 使用模型默认值） */
        private Double temperature;
        /** 最大 token 数（null = 使用模型默认值） */
        private Integer maxTokens;
        /** 是否流式输出 */
        private boolean streamOutput;
    }

    /**
     * 对话配置值对象，对应前端对话配置面板（require_dialog_config 面板）的配置项
     */
    @Data
    public static class DialogConfig {
        /** 用户提示词（支持变量引用） */
        private String userPrompt;
        /** 是否启用多模态 */
        private boolean enableMultimodal;
        /** 是否启用历史对话 */
        private boolean historyEnabled;
        /** 历史消息条数上限 */
        private int historyLimit = 10;
        /** 历史消息 Token 上限（0 = 不限制，仅按条数） */
        private int historyMaxTokens = 0;
    }

    // =========================================================================
    // 配置读取
    // =========================================================================

    /**
     * 从 context 读取 AI 配置（require_ai_config 面板对应的配置项）
     */
    protected AiConfig readAiConfig(NodeContext context) {
        AiConfig cfg = new AiConfig();
        cfg.setModelId(context.getConfigAsLong("modelId"));
        cfg.setTemperature(context.getConfigAsDouble("temperature", null));
        cfg.setMaxTokens(context.getConfigAsInteger("maxTokens", null));
        cfg.setStreamOutput(Boolean.TRUE.equals(context.getConfigAsBoolean("streamOutput", false)));
        return cfg;
    }

    /**
     * 从 context 读取对话配置（require_dialog_config 面板对应的配置项）
     * <p>
     * userPrompt 优先从 inputs 取（支持上游节点动态传入），再从 config 取。
     */
    protected DialogConfig readDialogConfig(NodeContext context) {
        DialogConfig cfg = new DialogConfig();
        String userPrompt = (String) context.getInput("userPrompt");
        if (userPrompt == null) {
            userPrompt = context.getConfigAsString("userPrompt");
        }
        cfg.setUserPrompt(userPrompt);
        cfg.setEnableMultimodal(Boolean.TRUE.equals(context.getConfigAsBoolean("enableMultimodal", false)));
        cfg.setHistoryEnabled(Boolean.TRUE.equals(context.getConfigAsBoolean("historyEnabled", false)));
        cfg.setHistoryLimit(context.getConfigAsInteger("historyLimit", 10));
        cfg.setHistoryMaxTokens(context.getConfigAsInteger("historyMaxTokens", 0));
        return cfg;
    }

    // =========================================================================
    // 模型加载
    // =========================================================================

    /**
     * 根据 modelId 加载模型实体，找不到则抛异常
     */
    protected KmModel loadModel(Long modelId) {
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        return model;
    }

    /**
     * 根据 model 加载 Provider，找不到则抛异常
     */
    protected KmModelProvider loadProvider(KmModel model) {
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }
        return provider;
    }

    /**
     * 从 context 读取 modelId，加载模型和 Provider，并将 apiBase 回填到 model。
     *
     * @return [model, provider] 数组
     */
    protected Object[] loadModelAndProvider(NodeContext context) {
        Long modelId = context.getConfigAsLong("modelId");
        return loadModelAndProviderById(modelId);
    }

    /**
     * 根据 modelId 加载模型和 Provider，并将 apiBase 回填到 model
     */
    protected Object[] loadModelAndProviderById(Long modelId) {
        KmModel model = loadModel(modelId);
        KmModelProvider provider = loadProvider(model);
        String apiBase = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : provider.getDefaultEndpoint();
        model.setApiBase(apiBase);
        return new Object[]{model, provider};
    }

    // =========================================================================
    // 模型调用
    // =========================================================================

    /**
     * 统一的模型调用入口（AiConfig 版本），自动处理流式/阻塞分支，并将 token 统计写入 context。
     */
    protected ChatResponse callModel(List<ChatMessage> messages, NodeContext context,
            KmModel model, KmModelProvider provider, AiConfig aiConfig) throws Exception {
        return callModel(messages, context, model, provider, aiConfig.getTemperature(), aiConfig.getMaxTokens());
    }

    /**
     * 统一的模型调用入口（直接传参版本），自动处理流式/阻塞分支，并将 token 统计写入 context。
     */
    protected ChatResponse callModel(List<ChatMessage> messages, NodeContext context,
            KmModel model, KmModelProvider provider,
            Double temperature, Integer maxTokens) throws Exception {

        Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);
        SseEmitter emitter = context.getSseEmitter();

        ChatResponse response;
        if (Boolean.TRUE.equals(streamOutput)) {
            StreamingChatModel streamingModel = modelBuilder.buildStreamingChatModel(
                    model, provider.getProviderKey(), temperature, maxTokens);
            response = callStreaming(streamingModel, messages, emitter);
        } else {
            ChatModel chatModel = modelBuilder.buildChatModel(
                    model, provider.getProviderKey(), temperature, maxTokens);
            response = chatModel.chat(messages);
        }

        recordTokenUsage(response, context);
        return response;
    }

    /**
     * 流式调用，阻塞等待完成，并将 token 逐片推送到 SSE
     */
    protected ChatResponse callStreaming(StreamingChatModel streamingModel,
            List<ChatMessage> messages, SseEmitter emitter) throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        streamingModel.chat(messages, new StreamingChatResponseHandler() {
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
                    } catch (IOException e) {
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
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("流式调用被中断", e);
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        return responseRef.get();
    }

    // =========================================================================
    // Token 统计
    // =========================================================================

    /**
     * 将 token 使用情况写入 context 并返回统计 Map（可直接放入 NodeOutput）
     */
    protected Map<String, Object> recordTokenUsage(ChatResponse response, NodeContext context) {
        if (response == null || response.tokenUsage() == null) {
            return null;
        }
        TokenUsage tokenUsage = response.tokenUsage();
        Map<String, Object> usageMap = new HashMap<>();
        usageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
        usageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
        usageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
        context.setTokenUsage(usageMap);
        log.info("{} Token使用: input={}, output={}, total={}",
                getNodeType(),
                tokenUsage.inputTokenCount(),
                tokenUsage.outputTokenCount(),
                tokenUsage.totalTokenCount());
        return usageMap;
    }

    // =========================================================================
    // 对话配置处理（require_dialog_config）
    // =========================================================================

    /**
     * 收集多模态文件（enableMultimodal=true 时调用）
     * <p>
     * 优先级：inputs.files > inputs.ossIds > globalState.files
     */
    protected List<KmWorkflowFile> collectMultimodalFiles(NodeContext context) {
        List<KmWorkflowFile> workflowFiles = new ArrayList<>();

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
                    wf.setOssId(map.get("ossId") instanceof Number ? ((Number) map.get("ossId")).longValue() : null);
                    wf.setTempFileId(map.get("tempFileId") instanceof Number ? ((Number) map.get("tempFileId")).longValue() : null);
                    wf.setName((String) map.get("name"));
                    workflowFiles.add(wf);
                }
            }
        } else if (inputFiles instanceof KmWorkflowFile) {
            workflowFiles.add((KmWorkflowFile) inputFiles);
        }

        if (workflowFiles.isEmpty()) {
            Object inputOssIds = context.getInput("ossIds");
            if (inputOssIds == null) inputOssIds = context.getInput("ossId");
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
                        wf.setType("image");
                        workflowFiles.add(wf);
                    } catch (Exception e) {
                        log.warn("collectMultimodalFiles 解析 ossId 失败: {}", idObj);
                    }
                }
            }
        }

        if (workflowFiles.isEmpty()) {
            Object globalFiles = context.getGlobalValue("files");
            if (globalFiles instanceof List) {
                @SuppressWarnings("unchecked")
                List<KmWorkflowFile> list = (List<KmWorkflowFile>) globalFiles;
                workflowFiles.addAll(list);
            }
        }

        return workflowFiles;
    }

    /**
     * 构建完整的聊天消息列表
     * <p>
     * 处理顺序：systemPrompt → 历史消息（含 token 裁剪）→ 当前用户消息（含多模态内容拼装）
     * <p>
     * 所有 require_dialog_config = 1 的节点都应调用此方法，而不是自己手写消息构建逻辑。
     *
     * @param context      节点上下文
     * @param userInput    当前用户输入（纯文本或 JSON 多模态数组）
     * @param systemPrompt 系统提示词（null 则不添加）
     * @param dialogConfig 对话配置（从 {@link #readDialogConfig} 获取）
     */
    protected List<ChatMessage> buildChatMessages(NodeContext context, String userInput,
            String systemPrompt, DialogConfig dialogConfig) throws Exception {

        String userPrompt = dialogConfig.getUserPrompt();
        boolean historyEnabled = dialogConfig.isHistoryEnabled();
        int historyLimit = dialogConfig.getHistoryLimit();
        int historyMaxTokens = dialogConfig.getHistoryMaxTokens();
        boolean enableMultimodal = dialogConfig.isEnableMultimodal();

        String chatContext = (String) context.getInput("chatContext");
        Long sessionId = context.getSessionId();
        List<KmWorkflowFile> files = enableMultimodal ? collectMultimodalFiles(context) : new ArrayList<>();

        List<ChatMessage> messages = new ArrayList<>();

        // 1. 系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 历史消息（含 token 裁剪）
        if (historyEnabled && sessionId != null) {
            IChatMessageProvider historyProvider = chatMessageProviderHolder != null
                    ? chatMessageProviderHolder.getIfAvailable() : null;
            if (historyProvider != null) {
                int fetchLimit = historyMaxTokens > 0 ? Math.max(historyLimit * 2, 50) : historyLimit;
                List<ChatMessage> rawHistory = historyProvider.loadHistoryMessages(sessionId, fetchLimit);

                if (historyMaxTokens > 0 && !rawHistory.isEmpty()) {
                    int maxChars = historyMaxTokens * 4;
                    int totalChars = 0;
                    int startIdx = rawHistory.size();
                    for (int i = rawHistory.size() - 1; i >= 0; i--) {
                        totalChars += extractText(rawHistory.get(i)).length();
                        if (totalChars > maxChars) { startIdx = i + 1; break; }
                        startIdx = i;
                    }
                    messages.addAll(rawHistory.subList(startIdx, rawHistory.size()));
                    log.debug("历史记录 Token 裁剪: 原始 {} 条 → 裁剪后 {} 条", rawHistory.size(), messages.size() - (systemPrompt != null ? 1 : 0));
                } else {
                    messages.addAll(rawHistory);
                }
                log.debug("加载历史对话: sessionId={}, 条数={}", sessionId, rawHistory.size());
            }
        }

        // 3. 构建用户消息前缀（chatContext + userPrompt）
        StringBuilder contextPrefix = new StringBuilder();
        if (chatContext != null && !chatContext.isEmpty()) {
            contextPrefix.append("已知信息：").append(chatContext).append("\n");
        }
        if (userPrompt != null && !userPrompt.isEmpty()) {
            contextPrefix.append(userPrompt).append("\n");
        }

        boolean processed = false;

        // 4. JSON 多模态数组解析
        if (enableMultimodal && JSONUtil.isTypeJSONArray(userInput)) {
            try {
                cn.hutool.json.JSONArray array = JSONUtil.parseArray(userInput);
                boolean hasMultimodal = false;
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item != null && JSONUtil.parseObj(item).containsKey("type")) {
                        hasMultimodal = true;
                        break;
                    }
                }

                if (hasMultimodal) {
                    StringBuilder textPart = new StringBuilder(contextPrefix);
                    List<Content> contents = new ArrayList<>();

                    for (int i = 0; i < array.size(); i++) {
                        Object item = array.get(i);
                        if (item == null) continue;
                        JSONObject obj = JSONUtil.parseObj(item);
                        String type = obj.getStr("type");

                        if ("text".equals(type)) {
                            textPart.append(obj.getStr("text"));
                        } else {
                            if (textPart.length() > 0) {
                                contents.add(TextContent.from(textPart.toString()));
                                textPart.setLength(0);
                            }
                            String fileIdRef = resolveFileIdRef(obj);
                            addMultimodalContent(contents, type, fileIdRef, obj.getStr("url"), obj.getStr("name"));
                        }
                    }

                    if (textPart.length() > 0) contents.add(TextContent.from(textPart.toString()));
                    if (!contents.isEmpty()) {
                        messages.add(UserMessage.from(contents));
                        processed = true;
                        log.info("{} buildChatMessages JSON多模态转化成功, 内容元素数={}", getNodeType(), contents.size());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 JSON 多模态输入失败，将作为普通文本处理: {}", e.getMessage());
            }
        }

        // 5. 普通文本 / 工作流文件列表
        if (!processed) {
            StringBuilder fullText = new StringBuilder(contextPrefix).append(userInput);
            List<Content> contents = new ArrayList<>();

            if (files != null && !files.isEmpty()) {
                for (KmWorkflowFile file : files) {
                    String fileIdRef = file.getTempFileId() != null
                            ? file.getTempFileId().toString()
                            : (file.getOssId() != null ? file.getOssId().toString() : null);
                    addMultimodalContent(contents, file.getType(), fileIdRef, file.getUrl(), file.getName());
                }
            }

            if (!contents.isEmpty()) {
                contents.add(0, TextContent.from(fullText.toString()));
                messages.add(UserMessage.from(contents));
                log.info("{} buildChatMessages 工作流文件多模态转化成功", getNodeType());
            } else {
                messages.add(UserMessage.from(fullText.toString()));
                log.info("{} buildChatMessages 普通文本转化完成", getNodeType());
            }
        }

        return messages;
    }

    /**
     * 根据文件类型添加多模态内容到 contents 列表
     */
    private void addMultimodalContent(List<Content> contents, String type,
            String fileIdRef, String fallbackUrl, String fileName) {
        if ("image".equals(type)) {
            String url = resolveUrl(fileIdRef, fallbackUrl, "image/jpeg");
            if (StrUtil.isNotBlank(url)) contents.add(ImageContent.from(url));
        } else if ("audio".equals(type)) {
            String url = resolveUrl(fileIdRef, fallbackUrl, "audio/mpeg");
            if (StrUtil.isNotBlank(url)) {
                try { contents.add(AudioContent.from(url)); }
                catch (Throwable e) { log.warn("LC4J不支持AudioContent: {}", e.getMessage()); }
            }
        } else if ("pdf".equals(type) || ("file".equals(type) && fileName != null && fileName.toLowerCase().endsWith(".pdf"))) {
            String url = resolveUrl(fileIdRef, fallbackUrl, "application/pdf");
            if (StrUtil.isNotBlank(url)) {
                try {
                    contents.add(url.startsWith("http")
                            ? PdfFileContent.from(java.net.URI.create(url))
                            : PdfFileContent.from(url));
                } catch (Throwable e) { log.warn("LC4J不支持PdfFileContent: {}", e.getMessage()); }
            }
        } else if ("video".equals(type) || ("file".equals(type) && fileName != null && fileName.toLowerCase().matches(".*\\.(mp4|avi|mov|wmv|flv|mkv)$"))) {
            String url = resolveUrl(fileIdRef, fallbackUrl, "video/mp4");
            if (StrUtil.isNotBlank(url)) {
                try {
                    contents.add(url.startsWith("http")
                            ? VideoContent.from(java.net.URI.create(url))
                            : VideoContent.from(url));
                } catch (Throwable e) { log.warn("LC4J不支持VideoContent: {}", e.getMessage()); }
            }
        } else if ("file".equals(type) && StrUtil.isNotBlank(fileName)) {
            // 通用文件：仅记录文件名，实际内容由 FILE_PARSE 节点处理
            log.debug("通用文件类型，仅记录文件名: {}", fileName);
        }
    }

    /**
     * 从 JSON 对象中解析文件 ID 引用（tempFileId 优先，其次 ossId）
     */
    private String resolveFileIdRef(JSONObject obj) {
        String fileIdRef = obj.getStr("tempFileId");
        if (StrUtil.isBlank(fileIdRef) || "undefined".equals(fileIdRef)) {
            fileIdRef = obj.getStr("ossId");
        }
        return "undefined".equals(fileIdRef) ? null : fileIdRef;
    }

    /**
     * 解析文件 URL，优先通过 workflowNodeUtils 转换，不可用时降级使用 fallbackUrl
     */
    private String resolveUrl(String fileIdRef, String fallbackUrl, String defaultMimeType) {
        if (workflowNodeUtils != null) {
            return workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, fallbackUrl, defaultMimeType);
        }
        log.warn("workflowNodeUtils 不可用，降级使用原始 URL: {}", fallbackUrl);
        return fallbackUrl;
    }

    /**
     * 从 ChatMessage 中提取纯文本内容，用于历史消息 token 数量的字符估算
     * （估算策略：1 token ≈ 4 字符）
     */
    protected String extractText(ChatMessage message) {
        if (message == null) return "";
        return switch (message.type()) {
            case USER -> {
                dev.langchain4j.data.message.UserMessage um = (dev.langchain4j.data.message.UserMessage) message;
                StringBuilder sb = new StringBuilder();
                for (dev.langchain4j.data.message.Content c : um.contents()) {
                    if (c instanceof dev.langchain4j.data.message.TextContent tc) sb.append(tc.text());
                }
                yield sb.toString();
            }
            case AI -> {
                dev.langchain4j.data.message.AiMessage am = (dev.langchain4j.data.message.AiMessage) message;
                yield am.text() != null ? am.text() : "";
            }
            case SYSTEM -> {
                dev.langchain4j.data.message.SystemMessage sm = (dev.langchain4j.data.message.SystemMessage) message;
                yield sm.text();
            }
            case TOOL_EXECUTION_RESULT -> {
                dev.langchain4j.data.message.ToolExecutionResultMessage tr =
                        (dev.langchain4j.data.message.ToolExecutionResultMessage) message;
                yield tr.text() != null ? tr.text() : "";
            }
            default -> message.toString();
        };
    }
}
