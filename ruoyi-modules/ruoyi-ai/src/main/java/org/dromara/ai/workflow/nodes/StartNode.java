package org.dromara.ai.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * 开始节点
 * 接收用户输入并保存到全局状态
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@Component("START")
public class StartNode implements WorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行START节点");

        NodeOutput output = new NodeOutput();

        // 获取用户输入
        String userInput = (String) context.getInput("userInput");
        if (userInput == null) {
            userInput = (String) context.getGlobalValue("userInput");
        }

        // 保存到输出
        output.addOutput("userInput", userInput);

        // 保存到全局状态
        context.setGlobalValue("userInput", userInput);

        log.info("START节点执行完成, userInput={}", userInput);
        return output;
    }

    @Override
    public String getNodeType() {
        return "START";
    }

    @Override
    public String getNodeName() {
        return "开始";
    }
}
