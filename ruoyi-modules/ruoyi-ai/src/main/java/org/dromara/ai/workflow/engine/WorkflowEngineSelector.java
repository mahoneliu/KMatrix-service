// package org.dromara.ai.workflow.engine;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.dromara.ai.workflow.core.WorkflowConfig;
// import org.springframework.stereotype.Component;

// import java.util.List;

// /**
// * 工作流引擎选择器
// *
// * @author Mahone
// * @date 2026-01-03
// */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class WorkflowEngineSelector {

// private final List<WorkflowEngine> engines;

// /**
// * 选择合适的工作流引擎
// */
// public WorkflowEngine selectEngine(WorkflowConfig config) {
// // 1. 优先使用配置指定的引擎
// if (config.getPreferredEngine() != null) {
// return getEngineByType(config.getPreferredEngine());
// }

// // 2. 根据工作流特征自动选择
// for (WorkflowEngine engine : engines) {
// if (engine.supports(config)) {
// log.info("为工作流选择引擎: {}", engine.getEngineType());
// return engine;
// }
// }

// // 3. 默认使用简单引擎
// log.warn("未找到合适的引擎，使用默认简单引擎");
// return getEngineByType(WorkflowEngineType.SIMPLE);
// }

// private WorkflowEngine getEngineByType(WorkflowEngineType type) {
// return engines.stream()
// .filter(e -> e.getEngineType() == type)
// .findFirst()
// .orElseThrow(() -> new RuntimeException("未找到引擎: " + type));
// }
// }
