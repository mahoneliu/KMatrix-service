package org.dromara.ai.workflow.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.ai.workflow.state.WorkflowState;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;

/**
 * 开始节点
 * 接收用户输入并保存到全局状态
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component("START")
public class StartNode implements WorkflowNode {

    public static final String KEY_USER_INPUT = "userInput";

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行START节点");

        NodeOutput output = new NodeOutput();

        // 获取用户输入 - 从 LangGraph 的 state 中获取
        String userInput = (String) context.getGlobalValue(KEY_USER_INPUT);

        // 1. 保存用户输入到全局状态
        List<String> historyContext = new ArrayList<>();
        historyContext.add(userInput);
        context.setGlobalValue(WorkflowState.KEY_HISTORY_CONTEXT, historyContext);

        // 2.保存用户名到全局状态
        String username = LoginHelper.getUsername();
        context.setGlobalValue(WorkflowState.KEY_USER_NAME, username);

        // 3.保存到输出
        output.addOutput(KEY_USER_INPUT, userInput);

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
