package org.dromara.ai.workflow.workflow.nodes;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeRouteConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
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
@Component(NodeTypeConstants.INTENT_CLASSIFIER)
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
        return (String) context.getInput(NodeIOConstants.INPUT_INSTRUCTION);
    }

    /**
     * 系统提示词：支持用户自定义增强，结合意图列表动态构建
     * 优先从 inputs 获取用户自定义系统提示词，其次从 config 获取，最后使用默认提示词
     */
    @Override
    protected String buildSystemPrompt(NodeContext context) {
        List<String> intentNames = extractIntentNames(context.getConfig(NodeConfigConstants.CFG_IC_INTENTS));
        
        String customSystemPrompt = (String) context.getInput(NodeIOConstants.INPUT_SYSTEM_PROMPT);
        if (customSystemPrompt == null) {
            customSystemPrompt = context.getConfigAsString(NodeConfigConstants.CFG_DIALOG_SYSTEM_PROMPT);
        }
        
        String basePrompt = buildIntentPrompt(intentNames);
        
        if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) {
            return customSystemPrompt + "\n\n" + basePrompt;
        }
        
        return basePrompt;
    }

    /**
     * 处理模型响应：提取意图名称并映射到路由 key
     */
    @Override
    protected void processAiResponse(ChatResponse response, NodeContext context, NodeOutput output) {
        String responseText = response.aiMessage().text();
        String intent = responseText.trim().toLowerCase();

        List<String> intentNames = extractIntentNames(context.getConfig(NodeConfigConstants.CFG_IC_INTENTS));
        String routeKey = NodeRouteConstants.INTENT_ROUTE_ELSE;
        int intentIndex = -1;
        for (int i = 0; i < intentNames.size(); i++) {
            if (intentNames.get(i).toLowerCase().equals(intent)) {
                intentIndex = i;
                routeKey = NodeRouteConstants.INTENT_BRANCH_PREFIX + i;
                break;
            }
        }

        if (intentIndex == -1) {
            intent = NodeRouteConstants.INTENT_ROUTE_ELSE;
        }

        output.addOutput(NodeIOConstants.OUTPUT_INTENT, intent);
        output.addOutput(NodeRouteConstants.OUTPUT_ROUTE_KEY, routeKey);
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
    public String getNodeType() { return NodeTypeConstants.INTENT_CLASSIFIER; }

    @Override
    public String getNodeName() { return "意图识别"; }
}
