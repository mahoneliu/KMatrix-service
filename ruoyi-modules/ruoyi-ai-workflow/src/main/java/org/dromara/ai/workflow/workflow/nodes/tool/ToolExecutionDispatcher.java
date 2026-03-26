package org.dromara.ai.workflow.workflow.nodes.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * 工具执行分发器
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
public class ToolExecutionDispatcher {

    /**
     * 分发并执行工具调用请求
     *
     * @param request  大模型返回的工具调用请求
     * @param bindings 当前节点绑定的工具列表
     * @return 工具执行结果消息
     */
    public static ToolExecutionResultMessage dispatch(ToolExecutionRequest request, List<ToolBinding> bindings) {
        String toolName = request.name();
        String arguments = request.arguments();

        Optional<ToolBinding> bindingOpt = bindings.stream()
                .filter(b -> b.getToolName().equals(toolName))
                .findFirst();

        if (bindingOpt.isEmpty()) {
            log.warn("未找到对应的工具绑定: {}", toolName);
            return ToolExecutionResultMessage.from(request, "Error: Tool '" + toolName + "' not found or not bound.");
        }

        ToolBinding binding = bindingOpt.get();
        try {
            log.info("开始执行工具 [{}], 参数: {}", toolName, arguments);
            String result = binding.getExecutor().execute(arguments);
            log.info("工具 [{}] 执行成功", toolName);
            return ToolExecutionResultMessage.from(request, result == null ? "Success with empty result" : result);
        } catch (Exception e) {
            log.error("执行工具 [{}] 时发生异常", toolName, e);
            return ToolExecutionResultMessage.from(request, "Error executing tool: " + e.getMessage());
        }
    }
}
