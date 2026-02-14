package org.dromara.ai.workflow.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 节点工厂
 * 根据节点类型创建节点实例
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NodeFactory {

    private final ApplicationContext applicationContext;

    /**
     * 根据节点类型创建节点实例
     *
     * @param nodeType 节点类型
     * @return 节点实例
     */
    public WorkflowNode createNode(String nodeType) {
        try {
            // 从Spring容器中获取节点Bean (节点的@Component注解value即为nodeType)
            WorkflowNode node = (WorkflowNode) applicationContext.getBean(nodeType);
            log.debug("创建节点实例: {}", nodeType);
            return node;
        } catch (Exception e) {
            log.error("创建节点实例失败: {}", nodeType, e);
            throw new RuntimeException("不支持的节点类型: " + nodeType, e);
        }
    }
}
