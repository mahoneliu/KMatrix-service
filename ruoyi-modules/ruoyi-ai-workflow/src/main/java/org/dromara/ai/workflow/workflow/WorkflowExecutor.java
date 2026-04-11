package org.dromara.ai.workflow.workflow;

import org.dromara.common.core.utils.MessageUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.ai.workflow.domain.bo.WorkflowExecutionReq;
import org.dromara.ai.api.enums.SseEventType;

import org.dromara.ai.workflow.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.workflow.core.WorkflowConfig;
import org.dromara.ai.workflow.workflow.engine.LangGraphWorkflowEngine;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.dromara.ai.api.domain.vo.config.AppParametersConfig;
import org.dromara.ai.api.domain.vo.config.ParamDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行器
 * 统一入口，直接使用 LangGraph 引擎执行工作流
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WorkflowExecutor {

    private final LangGraphWorkflowEngine langGraphEngine;
    private final IWorkflowInstanceService instanceService;
    private final ObjectMapper objectMapper;

    /**
     * 执行工作流（统一入口，支持调试和正式模式）
     */
    public Map<String, Object> executeWorkflow(WorkflowExecutionReq req, SseEmitter emitter) throws Exception {
        return executeWorkflow(req, emitter, false);
    }

    /**
     * 调试模式执行工作流（不创建instance，不写数据库）
     */
    public Map<String, Object> executeWorkflowDebug(WorkflowExecutionReq req, SseEmitter emitter) throws Exception {
        return executeWorkflow(req, emitter, true);
    }

    /**
     * 执行工作流（内部统一实现）
     *
     * @param debug true=调试模式（不入库），false=正式模式（入库）
     */
    private Map<String, Object> executeWorkflow(WorkflowExecutionReq req, SseEmitter emitter, boolean debug)
            throws Exception {

        // 1. 解析工作流配置
        WorkflowConfig config = objectMapper.readValue(req.getDslData(), WorkflowConfig.class);
        if (config == null || config.getNodes() == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.workflow.invalid_config"));
        }

        log.info("执行工作流: appId={}, debug={}", req.getAppId(), debug);

        // 2. 创建或使用虚拟实例ID
        Long instanceId;
        if (debug) {
            instanceId = -1L; // 调试模式：虚拟ID
        } else {
            instanceId = instanceService.createInstance(req.getAppId(), req.getSessionId(),
                    req.getDslData());
        }

        Boolean showExecutionInfo = debug ? true
                : ("1".equals(req.getEnableExecutionDetail()) && Boolean.TRUE.equals(req.getShowExecutionInfo()));
        // // 3. 初始化状态
        Map<String, Object> globalState = new HashMap<>();
        globalState.put(WorkflowState.KEY_INSTANCE_ID, instanceId);
        globalState.put(WorkflowState.KEY_USER_INPUT, req.getMessage());
        globalState.put(WorkflowState.KEY_SESSION_ID, req.getSessionId());
        globalState.put(WorkflowState.KEY_USER_ID, req.getUserId());
        globalState.put(WorkflowState.KEY_SHOW_EXECUTION_INFO, showExecutionInfo);
        globalState.put(WorkflowState.KEY_TEMP_FILE_IDS, req.getTempFileIds());
        globalState.put(WorkflowState.KEY_DOCUMENT_ID, req.getDocumentId());
        log.info("req.getDocumentId():{}", req.getDocumentId());

        // 初始化app参数
        // globalState.put(ChatWorkflowState.KEY_APP, app);

        if (debug) {
            globalState.put(WorkflowState.KEY_DEBUG, true); // 标记调试模式
        }

        // 注入自定义参数到 globalState
        log.info("处理 customParameters: {}", req.getApiParameters());
        if (req.getApiParameters() != null && !req.getApiParameters().isEmpty()) {
            for (Map.Entry<String, Object> entry : req.getApiParameters().entrySet()) {
                globalState.put(entry.getKey(), entry.getValue());
                log.info("注入 custom parameter: {} = {}", entry.getKey(), entry.getValue());
            }
        } else {
            log.warn("customParameters 为空，无法注入到 globalState");
        }

        // 分类参数硬注入 (注入到 nodeOutputs 中的虚拟 ID: interface, app, session)
        Map<String, Map<String, Object>> nodeOutputs = new HashMap<>();
        AppParametersConfig paramsConfig = req.getParametersConfig();
        Map<String, Object> apiParams = req.getApiParameters();

        if (paramsConfig != null && apiParams != null && !apiParams.isEmpty()) {
            injectParamCategory(nodeOutputs, WorkflowState.KEY_INTERFACE, paramsConfig.getInterfaceParams(), apiParams);
            // app和session参数示例
            // injectParamCategory(nodeOutputs, "app", paramsConfig.getAppParams(),
            // apiParams);
            // injectParamCategory(nodeOutputs, "session", paramsConfig.getSessionParams(),
            // apiParams);
        }
        Map<String, Object> initData = new HashMap<>();
        initData.put(WorkflowState.KEY_GLOBAL_STATE, globalState);
        initData.put(WorkflowState.KEY_NODE_OUTPUTS, nodeOutputs);
        // 同时将 documentId 放到顶层，确保 LangGraph4j 能正确传递
        if (req.getDocumentId() != null) {
            initData.put(WorkflowState.KEY_DOCUMENT_ID, req.getDocumentId());
        }

        WorkflowState chatWorkflowState = new WorkflowState(initData);

        String finalResponse = null;
        long startTime = System.currentTimeMillis();

        try {
            // 4. 执行工作流
            WorkflowState finalState = langGraphEngine.execute(config, chatWorkflowState, emitter);
            finalResponse = finalState.getFinalResponse();

            // 5. 标记实例完成（调试模式：跳过）
            if (!debug) {
                instanceService.completeInstance(instanceId);
            }

            // 6. 发送 done 事件（调试模式：包含统计信息）
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("req.getSessionId()", req.getSessionId().toString());

            if (showExecutionInfo) {
                long durationMs = System.currentTimeMillis() - startTime;
                doneData.put(WorkflowState.KEY_DURATION_MS, durationMs);
            }
            Integer tokensTotal = (Integer) finalState.data().get(WorkflowState.KEY_TOTAL_TOKENS);
            doneData.put(WorkflowState.KEY_TOTAL_TOKENS, tokensTotal != null ? tokensTotal : 0);

            sendSseEvent(emitter, SseEventType.DONE, doneData);

            // 7. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put(WorkflowState.KEY_INSTANCE_ID, instanceId);
            result.put(WorkflowState.KEY_FINAL_RESPONSE, finalResponse != null ? finalResponse : "");

            Integer totalTokens = (Integer) finalState.data().get(WorkflowState.KEY_TOTAL_TOKENS);
            result.put(WorkflowState.KEY_TOTAL_TOKENS, totalTokens != null ? totalTokens : 0);

            // 调试模式：返回额外的统计信息
            if (showExecutionInfo) {
                long durationMs = System.currentTimeMillis() - startTime;
                result.put(WorkflowState.KEY_DURATION_MS, durationMs);
            }

            return result;

        } catch (Exception e) {
            // 标记实例失败（调试模式：跳过）
            if (!debug) {
                instanceService.failInstance(instanceId, e.getMessage());
            }
            sendSseEvent(emitter, SseEventType.NODE_ERROR, Map.of("error", e.getMessage()));
            throw e;
        }
    }

    private void sendSseEvent(SseEmitter emitter, SseEventType eventType, Map<String, Object> data) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventType.getEventName()).data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }

    private void injectParamCategory(Map<String, Map<String, Object>> nodeOutputs, String categoryKey,
            List<ParamDefinition> definitions, Map<String, Object> apiParams) {
        if (definitions == null || definitions.isEmpty())
            return;
        Map<String, Object> categoryOutput = new HashMap<>();
        for (ParamDefinition def : definitions) {
            String key = def.getKey();
            if (apiParams.containsKey(key)) {
                categoryOutput.put(key, apiParams.get(key));
            }
        }
        if (!categoryOutput.isEmpty()) {
            nodeOutputs.put(categoryKey, categoryOutput);
        }
    }
}
