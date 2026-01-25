package org.dromara.ai.workflow.nodes;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LLM对话节点
 * 调用大语言模型进行对话
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component("LLM_CHAT")
public class LlmChatNode implements WorkflowNode {

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

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

        // systemPrompt支持从inputs动态获取，也支持从config静态配置
        String systemPrompt = (String) context.getInput("systemPrompt");
        if (systemPrompt == null) {
            systemPrompt = context.getConfigAsString("systemPrompt");
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

        String inputMessage = (String) context.getInput("inputMessage");
        if (inputMessage == null) {
            throw new RuntimeException("inputMessage不能为空");
        }
        // 构建消息列表
        List<ChatMessage> messages = buildMessages(inputMessage, systemPrompt);

        // 使用流式模型（带参数）
        StreamingChatLanguageModel streamingModel = modelBuilder
                .buildStreamingChatModel(model, provider.getProviderKey(), temperature, maxTokens);

        StringBuilder fullResponse = new StringBuilder();
        SseEmitter emitter = context.getSseEmitter();

        // 使用 AtomicReference 保存 Response 对象以便在流式完成后访问
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response<AiMessage>> responseRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                if (emitter != null) {
                    try {
                        // 如果开启流式输出，发送THINKING事件
                        if (Boolean.TRUE.equals(streamOutput)) {
                            emitter.send(SseEmitter.event()
                                    .name(SseEventType.THINKING.getEventName())
                                    .data(token));
                        } else {
                            // 默认行为：发送普通消息
                            emitter.send(SseEmitter.event().data(token));
                        }
                    } catch (java.io.IOException e) {
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

        String aiResponse = fullResponse.toString();

        // 获取并记录 token 使用情况
        Response<AiMessage> response = responseRef.get();
        if (response != null && response.tokenUsage() != null) {
            dev.langchain4j.model.output.TokenUsage tokenUsage = response.tokenUsage();

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
        output.addOutput("response", aiResponse);
        context.setGlobalValue("aiResponse", aiResponse);

        log.info("LLM_CHAT节点执行完成, response={}", aiResponse);
        return output;
    }

    private List<ChatMessage> buildMessages(String inputMessage, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();

        // 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 添加用户消息
        if (inputMessage != null) {
            messages.add(new UserMessage(inputMessage));
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
}
