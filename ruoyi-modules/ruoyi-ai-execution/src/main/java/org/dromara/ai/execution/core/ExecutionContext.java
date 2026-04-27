package org.dromara.ai.execution.core;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 工具执行上下文
 * <p>
 * 封装执行过程中的环境信息，供 ToolExecutor / SkillExecutor 使用。
 *
 * @author KMatrix
 */
@Data
@Builder
public class ExecutionContext {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 工作流实例 ID（工作流场景下有效）
     */
    private String workflowInstanceId;

    /**
     * 工作流节点 ID（工作流场景下有效）
     */
    private String nodeId;

    /**
     * 上下文变量
     */
    private Map<String, Object> variables;

    /**
     * 超时时间（毫秒），0 表示不限
     */
    @Builder.Default
    private long timeout = 0;

    /**
     * 是否调试模式
     */
    @Builder.Default
    private boolean debugMode = false;
}
