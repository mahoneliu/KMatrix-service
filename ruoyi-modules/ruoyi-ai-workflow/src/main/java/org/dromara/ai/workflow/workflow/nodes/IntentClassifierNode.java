package org.dromara.ai.workflow.workflow.nodes;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.workflow.core.AbstractAiWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 意图识别节点
 * <p>
 * 基于 LLM 识别用户意图，支持对话配置（userPrompt / 历史消息 / 多模态）。
 * 使用基类模板方法 {@code executeWithDialogConfig} 统一处理公共配置逻辑。
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("INTENT_CLASSIFIER")
public class IntentClassifierNode extends AbstractAiWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行INTENT_CLASSIFIER节点");
        // 公共配置处理（读配置 → 加载模型 → 构建消息 → 调用模型 → token统计）全部由基类完成
        return executeWithDialogConfig(context);
    }

    /**
     * 用户输入：意图识别的指令文本
     */
    @Override
    protected String getUserInput(NodeContext context) {
        return (String) context.getInput("instruction");
    }

    /**
     * 系统提示词：根据配置的意图列表动态构建
     */
    @Override
    protected String buildSystemPrompt(NodeContext context) {
        List<String> intentNames = extractIntentNames(context.getConfig("intents"));
        return buildIntentPrompt(intentNames);
    }

    /**
     * 处理模型响应：提取意图名称并映射到路由 key
     */
    @Override
    protected void processAiResponse(ChatResponse response, NodeContext context, NodeOutput output) {
        String responseText = response.aiMessage().text();
        String intent = responseText.trim().toLowerCase();

        List<String> intentNames = extractIntentNames(context.getConfig("intents"));
        String routeKey = "else";
        int intentIndex = -1;
        for (int i = 0; i < intentNames.size(); i++) {
            if (intentNames.get(i).toLowerCase().equals(intent)) {
                intentIndex = i;
                routeKey = "intent-" + i;
                break;
            }
        }

        if (intentIndex == -1) {
            intent = "else";
        }

        output.addOutput("intent", intent);
        output.addOutput("routeKey", routeKey);
        log.info("INTENT_CLASSIFIER节点执行完成, intent={}, routeKey={}", intent, routeKey);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractIntentNames(Object intentsConfig) {
        if (intentsConfig == null) return new ArrayList<>();
        List<String> intentNames = new ArrayList<>();
        if (intentsConfig instanceof List) {
            for (Object item : (List<?>) intentsConfig) {
                if (item instanceof Map) {
                    String name = (String) ((Map<String, Object>) item).get("name");
                    if (name != null) intentNames.add(name);
                } else if (item instanceof String) {
                    intentNames.add((String) item);
                }
            }
        }
        return intentNames;
    }

    private String buildIntentPrompt(List<String> intents) {
        StringBuilder prompt = new StringBuilder("请识别用户的意图,只返回意图名称,不要有其他内容。\n可能的意图包括:\n");
        if (intents != null) {
            for (String intent : intents) {
                prompt.append("- ").append(intent).append("\n");
            }
        }
        prompt.append("如果不属于以上任何意图,返回 'else'");
        return prompt.toString();
    }

    @Override
    public String getNodeType() { return "INTENT_CLASSIFIER"; }

    @Override
    public String getNodeName() { return "意图识别"; }
}
