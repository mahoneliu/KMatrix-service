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

import java.util.List;

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

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行INTENT_CLASSIFIER节点");

        NodeOutput output = new NodeOutput();

        // 从配置获取固定参数
        Long modelId = context.getConfigAsLong("modelId");

        // 从输入获取动态参数
        String text = (String) context.getInput("text");
        if (text == null) {
            text = (String) context.getGlobalValue("userInput");
        }

        @SuppressWarnings("unchecked")
        List<String> intents = (List<String>) context.getConfig("intents");

        // 加载模型
        KmModel model = modelMapper.selectById(modelId);
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());

        // 构建提示词
        String systemPrompt = buildIntentPrompt(intents);

        // 调用LLM识别意图
        ChatLanguageModel chatModel = ModelBuilder.buildChatModel(model, provider.getProviderKey());
        String response = chatModel.generate(
                new SystemMessage(systemPrompt),
                new UserMessage(text)).content().text();

        // 提取意图(简单实现,假设LLM直接返回意图名称)
        String intent = response.trim().toLowerCase();

        // 验证意图是否在列表中
        if (intents != null && !intents.contains(intent)) {
            intent = "other";
        }

        // 保存输出
        output.addOutput("intent", intent);
        output.addOutput("confidence", 0.9); // 简化实现,固定置信度

        context.setGlobalValue("intent", intent);

        log.info("INTENT_CLASSIFIER节点执行完成, intent={}", intent);
        return output;
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
        prompt.append("如果不属于以上任何意图,返回 'other'");
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
