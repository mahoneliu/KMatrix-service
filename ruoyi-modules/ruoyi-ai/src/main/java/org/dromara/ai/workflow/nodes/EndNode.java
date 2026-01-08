package org.dromara.ai.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 结束节点
 * 标记工作流结束并输出最终结果
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("END")
public class EndNode implements WorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行END节点");

        NodeOutput output = new NodeOutput();

        // 获取最终响应
        Object finalResponse = context.getInput("finalResponse");
        if (finalResponse == null) {
            finalResponse = context.getGlobalValue("aiResponse");
        }

        // 保存到输出
        if (finalResponse != null) {
            output.addOutput("finalResponse", finalResponse);
            log.info("END节点执行完成, finalResponse={}", finalResponse);
        }
        // 标记为结束
        output.setFinished(true);

        return output;
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
