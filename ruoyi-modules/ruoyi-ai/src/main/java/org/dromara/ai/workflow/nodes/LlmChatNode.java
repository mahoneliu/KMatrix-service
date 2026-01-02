package org.dromara.ai.workflow.nodes;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行LLM_CHAT节点");

        NodeOutput output = new NodeOutput();

        // 从配置获取固定参数
        Long modelId = context.getConfigAsLong("modelId");

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

        // 构建消息列表
        List<ChatMessage> messages = buildMessages(context, systemPrompt);

        // 使用流式模型
        dev.langchain4j.model.chat.StreamingChatLanguageModel streamingModel = ModelBuilder
                .buildStreamingChatModel(model, provider.getProviderKey());

        StringBuilder fullResponse = new StringBuilder();
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = context.getSseEmitter();

        // 使用 CountDownLatch 等待流式完成
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

        streamingModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                if (emitter != null) {
                    try {
                        emitter.send(
                                org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(token));
                    } catch (java.io.IOException e) {
                        log.error("发送SSE消息失败", e);
                    }
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
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

        // 保存输出
        output.addOutput("response", aiResponse);
        context.setGlobalValue("aiResponse", aiResponse);

        log.info("LLM_CHAT节点执行完成, response={}", aiResponse);
        return output;
    }

    private List<ChatMessage> buildMessages(NodeContext context, String systemPrompt) {
        List<ChatMessage> messages = new ArrayList<>();

        // 添加系统提示
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 添加用户消息
        String userInput = (String) context.getGlobalValue("userInput");
        if (userInput != null) {
            messages.add(new UserMessage(userInput));
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
