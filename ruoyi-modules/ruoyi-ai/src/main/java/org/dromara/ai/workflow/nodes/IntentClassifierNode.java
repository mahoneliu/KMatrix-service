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

        // 从输入获取动态参数
        String text = (String) context.getInput("instruction");
        // if (text == null) {
        // text = (String) context.getGlobalValue("userInput");
        // }

        // 获取意图配置并提取意图名称
        List<String> intentNames = extractIntentNames(context.getConfig("intents"));

        // 加载模型
        KmModel model = modelMapper.selectById(modelId);
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());

        // 构建提示词
        String systemPrompt = buildIntentPrompt(intentNames);

        // 调用LLM识别意图
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());
        dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(text));

        String responseText = response.content().text();

        // 获取并记录 token 使用情况
        if (response.tokenUsage() != null) {
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

        // 验证意图是否在列表中
        if (intentNames != null && !intentNames.contains(intent)) {
            intent = "else"; // 使用 'else' 匹配前端条件表达式
        }

        // 保存输出
        output.addOutput("intent", intent);
        // output.addOutput("confidence", 0.9); // 简化实现,固定置信度

        // 同时保存到全局状态,方便下游节点直接访问
        // context.setGlobalValue("intent", intent);

        log.info("INTENT_CLASSIFIER节点执行完成, intent={}", intent);
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
