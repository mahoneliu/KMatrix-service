package org.dromara.ai.workflow.nodes;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.nodes.nodeUtils.WorkflowParamConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 结束节点
 * 标记工作流结束并输出最终结果
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("END")
public class EndNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行END节点");

        NodeOutput output = new NodeOutput();

        // 获取最终响应
        // String finalResponse =
        // WorkflowParamConverter.asString(context.getInput("finalResponse"));
        // if (finalResponse == null) {
        // finalResponse =
        // WorkflowParamConverter.asString(context.getGlobalValue("finalResponse"));
        // }

        // 从配置获取指定回复内容
        String finalResponse = (String) context.getConfig("customResponse");
        if (finalResponse == null) {
            finalResponse = (String) context.getInput("finalResponse");
        }

        // 仅保存到输出（不发送SSE事件）
        if (finalResponse != null) {
            output.addOutput("finalResponse", finalResponse);
            sendComplete(context.getSseEmitter(), finalResponse);
            log.info("END节点执行完成, finalResponse={}", finalResponse);
        }

        // 标记为结束
        output.setFinished(true);

        return output;
    }

    /**
     * 发送complete事件
     */
    private void sendComplete(SseEmitter emitter, String message) {
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.WORKFLOW_COMPLETE.getEventName())
                        .data(message));
            } catch (IOException e) {
                log.error("发送complete事件失败", e);
            }
        }
    }

    @Override
    public String getNodeType() {
        return "END";
    }

    @Override
    public String getNodeName() {
        return "结束";
    }
}
