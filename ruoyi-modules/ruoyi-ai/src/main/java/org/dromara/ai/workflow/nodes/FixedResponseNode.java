package org.dromara.ai.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 固定回复节点
 * 返回预设的固定文本内容
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("FIXED_RESPONSE")
public class FixedResponseNode implements WorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行FIXED_RESPONSE节点");

        NodeOutput output = new NodeOutput();

        // 从配置获取固定回复内容
        String responseText = (String) context.getConfig("responseText");
        if (responseText == null) {
            responseText = (String) context.getInput("responseText");
        }

        if (responseText == null) {
            responseText = "抱歉，未配置回复内容";
        }

        // 发送消息事件
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = context.getSseEmitter();
        if (emitter != null) {
            try {
                emitter.send(
                        org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().data(responseText));
            } catch (java.io.IOException e) {
                log.error("发送SSE消息失败", e);
            }
        }

        // 保存输出
        output.addOutput("response", responseText);
        output.addOutput("finalResponse", responseText);

        // 保存到全局状态，与LLM_CHAT节点保持一致
        context.setGlobalValue("aiResponse", responseText);

        log.info("FIXED_RESPONSE节点执行完成, response={}", responseText);
        return output;
    }

    @Override
    public String getNodeType() {
        return "FIXED_RESPONSE";
    }

    @Override
    public String getNodeName() {
        return "固定回复";
    }
}
