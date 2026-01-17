package org.dromara.ai.workflow.nodes;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        String response = (String) context.getConfig("content");
        if (response == null) {
            response = (String) context.getInput("content");
        }

        if (response == null) {
            response = "抱歉，未配置回复内容";
        } else {
            response = fillTextWithParamPattern(response, context);
        }

        // 发送消息事件
        SseEmitter emitter = context.getSseEmitter();
        if (emitter != null) {
            try {
                emitter.send(
                        SseEmitter.event().data(response));
            } catch (java.io.IOException e) {
                log.error("发送SSE消息失败", e);
            }
        }

        // 保存输出
        output.addOutput("response", response);

        log.info("FIXED_RESPONSE节点执行完成, response={}", response);
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
