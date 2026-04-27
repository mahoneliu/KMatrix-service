package org.dromara.ai.execution.core;

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
     * 分发并执行工具调用请求（返回富媒体 ToolResult）
     *
     * @param request  大模型返回的工具调用请求
     * @param bindings 当前节点绑定的工具列表
     * @return ToolResult（含文本 + 可选的富媒体内容列表）
     */
    public static ToolResult dispatchForResult(ToolExecutionRequest request, List<ToolBinding> bindings) {
        String toolName = request.name();
        String arguments = request.arguments();
        long startMs = System.currentTimeMillis();

        Optional<ToolBinding> bindingOpt = bindings.stream()
                .filter(b -> b.getToolName().equals(toolName))
                .findFirst();

        if (bindingOpt.isEmpty()) {
            log.warn("未找到对应的工具绑定: {}", toolName);
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Tool '" + toolName + "' not found or not bound.")
                    .durationMs(System.currentTimeMillis() - startMs)
                    .build();
        }

        ToolBinding binding = bindingOpt.get();
        try {
            log.info("开始执行工具 [{}], 参数: {}", toolName, arguments);
            ToolResult result = binding.getExecutor().executeForResult(arguments);
            long duration = System.currentTimeMillis() - startMs;
            log.info("工具 [{}] 执行成功, hasContents={}, 耗时={}ms", toolName, result.hasContents(), duration);

            // 补充耗时信息
            if (result.getDurationMs() == null) {
                return ToolResult.builder()
                        .text(result.getText())
                        .contents(result.getContents())
                        .success(result.isSuccess())
                        .errorMessage(result.getErrorMessage())
                        .durationMs(duration)
                        .build();
            }
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            log.error("执行工具 [{}] 时发生异常, 耗时={}ms", toolName, duration, e);
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Error executing tool: " + e.getMessage())
                    .durationMs(duration)
                    .build();
        }
    }

    /**
     * 按名称分发并执行工具（无需 ToolExecutionRequest）
     *
     * @param toolName  工具名称
     * @param arguments JSON 格式参数
     * @param bindings  当前绑定的工具列表
     * @return ToolResult
     */
    public static ToolResult dispatchByName(String toolName, String arguments, List<ToolBinding> bindings) {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id(java.util.UUID.randomUUID().toString())
                .name(toolName)
                .arguments(arguments)
                .build();
        return dispatchForResult(request, bindings);
    }

    /**
     * 分发并执行工具调用请求（兼容旧版，直接返回 ToolExecutionResultMessage）
     *
     * @param request  大模型返回的工具调用请求
     * @param bindings 当前节点绑定的工具列表
     * @return 工具执行结果消息
     */
    public static ToolExecutionResultMessage dispatch(ToolExecutionRequest request, List<ToolBinding> bindings) {
        ToolResult result = dispatchForResult(request, bindings);
        String text = result.getText() != null ? result.getText() : "Success with empty result";
        return ToolExecutionResultMessage.from(request, text);
    }
}
