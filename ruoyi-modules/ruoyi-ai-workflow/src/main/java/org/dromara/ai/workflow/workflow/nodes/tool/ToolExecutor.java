package org.dromara.ai.workflow.workflow.nodes.tool;

/**
 * 工具执行器接口
 *
 * @deprecated 已迁移至 {@link org.dromara.ai.execution.core.ToolExecutor}
 * @author Mahone
 * @date 2026-03-20
 */
@Deprecated
public interface ToolExecutor {

    /**
     * 执行工具（返回纯文本，兼容旧版）
     *
     * @param arguments JSON格式的参数字符串
     * @return 执行结果（通常为字符串或序列化好的JSON字符串）
     * @throws Exception 执行异常
     */
    String execute(String arguments) throws Exception;

    /**
     * 执行工具（返回富媒体结果）
     * <p>
     * 默认实现调用 {@link #execute} 并包装为纯文本 {@link ToolResult}。
     * 如需返回图片、文件等媒体内容，覆盖本方法即可，无需修改 {@link #execute}。
     *
     * @param arguments JSON格式的参数字符串
     * @return {@link ToolResult} 结果（含文本或富媒体内容列表）
     * @throws Exception 执行异常
     */
    default ToolResult executeForResult(String arguments) throws Exception {
        return ToolResult.ofText(execute(arguments));
    }
}

