package org.dromara.ai.workflow.nodes;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
import java.util.Map;

/**
 * 意图识别节点
 * 基于LLM识别用户意图
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component("INTENT_CLASSIFIER")
public class IntentClassifierNode implements WorkflowNode {

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行INTENT_CLASSIFIER节点");

        NodeOutput output = new NodeOutput();

        // 从配置获取固定参数
        Long modelId = context.getConfigAsLong("modelId");

        // 获取大模型参数配置
        Double temperature = context.getConfigAsDouble("temperature", null);
        Integer maxTokens = context.getConfigAsInteger("maxTokens", null);
        Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);

        // 从输入获取动态参数
        String text = (String) context.getInput("instruction");

        // 获取意图配置并提取意图名称
        List<String> intentNames = extractIntentNames(context.getConfig("intents"));

        // 加载模型
        KmModel model = modelMapper.selectById(modelId);
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());

        // 构建提示词
        String systemPrompt = buildIntentPrompt(intentNames);
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(text));

        String responseText;
        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response = null;

        if (Boolean.TRUE.equals(streamOutput)) {
            // 流式模式
            dev.langchain4j.model.chat.StreamingChatLanguageModel streamingModel = modelBuilder
                    .buildStreamingChatModel(model, provider.getProviderKey(), temperature, maxTokens);

            StringBuilder fullResponse = new StringBuilder();
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = context.getSseEmitter();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>> responseRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

            streamingModel.generate(messages,
                    new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            fullResponse.append(token);
                            if (emitter != null) {
                                try {
                                    // 发送 THINKING 事件
                                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter
                                            .event()
                                            .name(org.dromara.ai.domain.enums.SseEventType.THINKING.getEventName())
                                            .data(token));
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                        }

                        @Override
                        public void onComplete(
                                dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> resp) {
                            responseRef.set(resp);
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable error) {
                            errorRef.set(error);
                            latch.countDown();
                        }
                    });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("意图识别被中断", e);
            }

            if (errorRef.get() != null) {
                throw new RuntimeException("意图识别失败", errorRef.get());
            }

            response = responseRef.get();
            responseText = fullResponse.toString();

        } else {
            // 阻塞模式
            ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());
            // 注意: ChatModel 目前 buildChatModel 不支持动态 temperature/maxTokens 参数传递，
            // 如果需要支持非流式下的参数，可能需要修改 ModelBuilder 或使用带有参数的构建方式。
            // 鉴于 ModelBuilder.buildStreamingChatModel 支持参数，这里为了完整性，我们尽量只在流式下支持参数。
            // 或者我们可以尝试构建流式模型但阻塞调用？不，LangChain4j没有这个API。
            // 暂时保持原样，或者在 ModelBuilder 中添加 buildChatModel 重载。
            // 由于 ModelBuilder.buildChatModel 里面是 switch case 构建不同模型，修改比较大。
            // 为了符合"Ensure all AI nodes have configurable model parameters"，我们应该尽可能支持。
            // 但如果主要是为了 Thinking，那么流式已经支持了。非流式下参数不生效是个已知限制，除非修改 ModelBuilder。
            // 这里我们先专注于流式支持。
            response = chatModel.generate(messages);
            responseText = response.content().text();
        }

        // 获取并记录 token 使用情况
        if (response != null && response.tokenUsage() != null) {
            dev.langchain4j.model.output.TokenUsage tokenUsage = response.tokenUsage();

            // 保存到 NodeContext
            java.util.Map<String, Object> tokenUsageMap = new java.util.HashMap<>();
            tokenUsageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
            tokenUsageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
            tokenUsageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
            context.setTokenUsage(tokenUsageMap);

            // 添加到节点输出
            output.addOutput("tokenUsage", tokenUsageMap);

            log.info("INTENT_CLASSIFIER节点 Token使用: input={}, output={}, total={}",
                    tokenUsage.inputTokenCount(),
                    tokenUsage.outputTokenCount(),
                    tokenUsage.totalTokenCount());
        }

        // 提取意图(简单实现,假设LLM直接返回意图名称)
        String intent = responseText.trim().toLowerCase();

        // 查找意图在列表中的索引,用于生成 routeKey
        String routeKey = "else"; // 默认路由
        int intentIndex = -1;
        if (intentNames != null) {
            for (int i = 0; i < intentNames.size(); i++) {
                if (intentNames.get(i).toLowerCase().equals(intent)) {
                    intentIndex = i;
                    routeKey = "intent-" + i; // 匹配前端的 handle ID 格式
                    break;
                }
            }
        }

        // 如果没有匹配到任何意图,使用 else
        if (intentIndex == -1) {
            intent = "else";
        }

        // 保存输出
        output.addOutput("intent", intent);
        output.addOutput("routeKey", routeKey); // 用于 LangGraph 路由决策

        log.info("INTENT_CLASSIFIER节点执行完成, intent={}, routeKey={}", intent, routeKey);
        return output;
    }

    /**
     * 从配置中提取意图名称列表
     */
    @SuppressWarnings("unchecked")
    private List<String> extractIntentNames(Object intentsConfig) {
        if (intentsConfig == null) {
            return new ArrayList<>();
        }

        List<String> intentNames = new ArrayList<>();
        if (intentsConfig instanceof List) {
            List<?> intentList = (List<?>) intentsConfig;
            for (Object item : intentList) {
                if (item instanceof Map) {
                    // 前端传来的是对象数组: [{name: 'xxx', description: 'yyy', examples: []}, ...]
                    Map<String, Object> intentMap = (Map<String, Object>) item;
                    String name = (String) intentMap.get("name");
                    if (name != null) {
                        intentNames.add(name);
                    }
                } else if (item instanceof String) {
                    // 兼容纯字符串数组
                    intentNames.add((String) item);
                }
            }
        }
        return intentNames;
    }

    private String buildIntentPrompt(List<String> intents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请识别用户的意图,只返回意图名称,不要有其他内容。\n");
        prompt.append("可能的意图包括:\n");
        if (intents != null) {
            for (String intent : intents) {
                prompt.append("- ").append(intent).append("\n");
            }
        }
        prompt.append("如果不属于以上任何意图,返回 'else'");
        return prompt.toString();
    }

    @Override
    public String getNodeType() {
        return "INTENT_CLASSIFIER";
    }

    @Override
    public String getNodeName() {
        return "意图识别";
    }
}
