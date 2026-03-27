package org.dromara.ai.workflow.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;

/**
 * 图像文字识别节点
 * 将图片提取为文本文本
 */
@Slf4j
@Component("IMAGE_OCR")
public class OcrNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行IMAGE_OCR节点");
        NodeOutput output = new NodeOutput();
        
        Object ossId = context.getInput("ossId");
        if (ossId == null) {
            ossId = context.getConfig("ossId");
        }
        
        // TODO: 使用第三方OCR服务将ossId指向的图片转化为文本
        String extractedText = "[OCR 图片文本提取占位返回，未对接最终API]";
        
        output.addOutput("text", extractedText);
        output.addOutput("ossId", ossId);
        return output;
    }

    @Override
    public String getNodeType() {
        return "IMAGE_OCR";
    }

    @Override
    public String getNodeName() {
        return "图像OCR";
    }
}
