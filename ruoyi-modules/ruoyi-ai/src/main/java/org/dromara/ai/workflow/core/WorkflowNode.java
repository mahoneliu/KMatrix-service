package org.dromara.ai.workflow.core;

import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;

/**
 * 工作流节点接口
 * 所有工作流节点必须实现此接口
 *
 * @author Mahone
 * @date 2026-01-02
 */
public interface WorkflowNode {

    /**
     * 执行节点
     *
     * @param context 节点上下文
     * @return 节点输出
     * @throws Exception 执行异常
     */
    NodeOutput execute(NodeContext context) throws Exception;

    /**
     * 获取节点类型
     *
     * @return 节点类型标识
     */
    String getNodeType();

    /**
     * 获取节点名称
     *
     * @return 节点名称
     */
    default String getNodeName() {
        return getNodeType();
    }
}
