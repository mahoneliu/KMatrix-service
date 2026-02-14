package org.dromara.ai.workflow.engine;

import org.dromara.ai.workflow.core.WorkflowConfig;
import org.dromara.ai.workflow.core.WorkflowState;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 工作流引擎接口
 * 支持多种实现：简单引擎、LangGraph引擎等
 *
 * @author Mahone
 * @date 2026-01-03
 */
public interface WorkflowEngine {

    /**
     * 执行工作流
     *
     * @param config            工作流配置
     * @param chatWorkflowState 工作流状态
     * @param emitter           SSE推送器（用于实时事件推送）
     * @return 最终响应
     */
    String execute(WorkflowConfig config, WorkflowState chatWorkflowState, SseEmitter emitter) throws Exception;

    /**
     * 获取引擎类型
     */
    // WorkflowEngineType getEngineType();

    /**
     * 判断是否支持该工作流配置
     */
    // boolean supports(WorkflowConfig config);
}
