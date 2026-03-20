package org.dromara.ai.workflow.nodes.tool;

/**
 * 工具执行器接口
 *
 * @author Mahone
 * @date 2026-03-20
 */
public interface ToolExecutor {
    
    /**
     * 执行工具
     *
     * @param arguments JSON格式的参数字符串
     * @return 执行结果（通常为字符串或序列化好的JSON字符串）
     * @throws Exception 执行异常
     */
    String execute(String arguments) throws Exception;
}
