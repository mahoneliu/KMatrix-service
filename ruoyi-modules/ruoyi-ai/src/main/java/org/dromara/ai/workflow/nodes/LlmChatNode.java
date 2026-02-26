package org.dromara.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmChatMessage;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.mapper.KmChatMessageMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
    private final KmChatMessageMapper chatMessageMapper;

    /** 默认历史消息条数限制 */
    private static final int DEFAULT_HISTORY_LIMIT = 10;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
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
            throw new RuntimeException("模型不存在: " + modelId);
        }

        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }

        String userInput = (String) context.getInput("userInput");
        if (userInput == null) {
            throw new RuntimeException("userInput不能为空");
        }
        String chatContext = (String) context.getInput("chatContext");
        log.info("LLM_CHAT节点 - : chatContext={}", chatContext);

        // 获取会话ID用于加载历史对话
        Long sessionId = context.getSessionId();

        // 构建消息列表（包含历史对话）
        List<ChatMessage> messages = buildMessages(userInput, systemPrompt, userPrompt, sessionId, historyEnabled,
                historyLimit, chatContext);
        log.info(
                "LLM_CHAT节点 - : chatContext={}, userInput={}, userPrompt={}, systemPrompt={},historyEnabled={}, historyLimit={}, sessionId={}, 历史消息总数={}",
                chatContext, userInput, userPrompt, systemPrompt, historyEnabled, historyLimit, sessionId,
                messages.size());

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

        // 使用流式模型（带参数）
        StreamingChatLanguageModel streamingModel = modelBuilder
                .buildStreamingChatModel(model, provider.getProviderKey(), temperature, maxTokens);

        // StringBuilder fullResponse = new StringBuilder();
        // SseEmitter emitter = context.getSseEmitter(); // Moved up

        // 使用 AtomicReference 保存 Response 对象以便在流式完成后访问
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response<AiMessage>> responseRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                // fullResponse.append(token);
                if (emitter != null) {
                    try {
                        // 如果开启流式输出，发送THINKING事件
                        // if (Boolean.TRUE.equals(streamOutput)) {
                        // emitter.send(SseEmitter.event()
                        // .name(SseEventType.THINKING.getEventName())
                        // .data(token));
                        // } else {
                        // 默认行为：发送普通消息
                        emitter.send(SseEmitter.event().data(token));
                        // }
                    } catch (IOException e) {
                        log.error("发送SSE消息失败", e);
                    }
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                responseRef.set(response);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(new RuntimeException(error));
                latch.countDown();
            }
        });

        // 等待流式完成
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("流式生成被中断", e);
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }

        // String aiResponse = fullResponse.toString();

        // 获取并记录 token 使用情况
        Response<AiMessage> response = responseRef.get();
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
        AiMessage aiMessage = response.content();
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
            Long sessionId, Boolean historyEnabled, Integer historyLimit, String chatContext) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. 加载并添加历史对话
        if (Boolean.TRUE.equals(historyEnabled) && sessionId != null) {
            List<KmChatMessage> historyMessages = loadHistoryMessages(sessionId, historyLimit);
            for (KmChatMessage msg : historyMessages) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(new AiMessage(msg.getContent()));
                }
            }
            log.debug("加载历史对话: sessionId={}, 条数={}", sessionId, historyMessages.size());
        }

        // 3. 添加当前用户消息
        // 优先使用配置的用户提示词,如果没有配置则使用 userInput
        String defaultUserPrompt = "请回答问题：" + userInput;
        if (chatContext != null && !chatContext.isEmpty()) {
            defaultUserPrompt = "已知信息：" + chatContext + "\n" + defaultUserPrompt;
        }
        String finalUserMessage = (userPrompt != null && !userPrompt.isEmpty()) ? userPrompt : defaultUserPrompt;
        if (finalUserMessage != null) {
            messages.add(new UserMessage(finalUserMessage));
        }

        log.info("LLM_CHAT节点 - : buildMessages finalUserMessage={}, AiMessageList={}", finalUserMessage,
                messages);

        return messages;
    }

    /**
     * 从数据库加载历史对话消息
     *
     * @param sessionId 会话ID
     * @param limit     最大条数
     * @return 历史消息列表（按时间升序）
     */
    private List<KmChatMessage> loadHistoryMessages(Long sessionId, Integer limit) {
        if (sessionId == null || limit == null || limit <= 0) {
            return Collections.emptyList();
        }

        // 查询该会话的最近N条消息，按创建时间降序
        LambdaQueryWrapper<KmChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KmChatMessage::getSessionId, sessionId)
                .orderByDesc(KmChatMessage::getCreateTime)
                .last("LIMIT " + limit);

        List<KmChatMessage> messages = chatMessageMapper.selectList(queryWrapper);

        // 反转列表，使其按时间升序排列（先旧后新）
        Collections.reverse(messages);

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

}
