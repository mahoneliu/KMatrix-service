package org.dromara.ai.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流引擎类型
 *
 * @author Mahone
 * @date 2026-01-03
 */
@Getter
@AllArgsConstructor
public enum WorkflowEngineType {
    /**
     * 简单工作流引擎
     * 适用于：线性流程、简单分支、基础 RAG
     */
    SIMPLE("simple", "简单工作流引擎"),

    /**
     * LangGraph 工作流引擎
     * 适用于：Multi-Agent、复杂状态管理、循环、并行
     */
    LANGGRAPH("langgraph", "LangGraph工作流引擎");

    private final String code;
    private final String name;
}
