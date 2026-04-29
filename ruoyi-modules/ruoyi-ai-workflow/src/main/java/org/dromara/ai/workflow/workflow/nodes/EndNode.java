package org.dromara.ai.workflow.workflow.nodes;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

import org.dromara.ai.api.enums.SseEventType;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowParamConverter;
import org.dromara.common.core.utils.ObjectUtils;
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
@Component(NodeTypeConstants.END)
public class EndNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行END节点");

        NodeOutput output = new NodeOutput();

        // 获取最终响应
        // String finalResponse =
        // WorkflowParamConverter.asString(context.getInput(NodeIOConstants.INPUT_FINAL_RESPONSE));
        // if (finalResponse == null) {
        // finalResponse =
        // WorkflowParamConverter.asString(context.getGlobalValue(WorkflowState.KEY_FINAL_RESPONSE));
        // }

        // 从配置获取指定回复内容
        String finalResponse = (String) context.getConfig(NodeConfigConstants.CFG_END_CUSTOM_RESPONSE);
        if (ObjectUtils.isEmpty(finalResponse)) {
            finalResponse = (String) context.getInput(NodeIOConstants.INPUT_FINAL_RESPONSE);
        }

        Boolean coverStreamMsg = context.getConfigAsBoolean("coverStreamMsg", false);

        // 仅保存到输出（包装发送SSE事件）
        if (ObjectUtils.isNotEmpty(finalResponse)) {
            output.addOutput(NodeIOConstants.OUTPUT_FINAL_RESPONSE, finalResponse);

            cn.hutool.json.JSONObject payload = new cn.hutool.json.JSONObject();
            payload.set("text", finalResponse);
            payload.set("coverStreamMsg", coverStreamMsg);

            sendComplete(context.getSseEmitter(), payload.toString());
            log.info("END节点执行完成, finalResponse={}, coverStreamMsg={}", finalResponse, coverStreamMsg);
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
        return NodeTypeConstants.END;
    }

    @Override
    public String getNodeName() {
        return "结束";
    }
}
