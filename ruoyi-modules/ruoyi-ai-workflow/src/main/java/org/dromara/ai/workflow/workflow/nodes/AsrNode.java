package org.dromara.ai.workflow.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

/**
 * 语音识别节点
 * 将传入的音频附件转换成文本形式
 */
@Slf4j
@Component("AUDIO_ASR")
public class AsrNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行AUDIO_ASR节点");
        NodeOutput output = new NodeOutput();
        
        Object ossId = context.getInput("ossId");
        if (ossId == null) {
            ossId = context.getConfig("ossId");
        }
        
        // TODO: 结合第三方服务，通过ossId获取音频流并转换为文本
        String transcribedText = "[ASR 语音转文本占位返回，未对接最终API]";
        
        output.addOutput("transcription", transcribedText);
        output.addOutput("ossId", ossId);
        return output;
    }

    @Override
    public String getNodeType() {
        return "AUDIO_ASR";
    }

    @Override
    public String getNodeName() {
        return "语音识别";
    }
}
