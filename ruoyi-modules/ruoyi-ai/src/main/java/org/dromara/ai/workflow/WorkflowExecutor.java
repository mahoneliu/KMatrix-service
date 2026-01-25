package org.dromara.ai.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.config.WorkflowConfig;
import org.dromara.ai.workflow.engine.LangGraphWorkflowEngine;
import org.dromara.ai.workflow.state.WorkflowState;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.HashMap;
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
    public Map<String, Object> executeWorkflow(KmAppVo app, Long sessionId, KmChatSendBo bo,
            SseEmitter emitter, Long userId) throws Exception {
        return executeWorkflow(app, sessionId, bo, emitter, userId, false);
    }

    /**
     * 调试模式执行工作流（不创建instance，不写数据库）
     */
    public Map<String, Object> executeWorkflowDebug(KmAppVo app, Long debugSessionId, KmChatSendBo bo,
            SseEmitter emitter, Long userId) throws Exception {
        return executeWorkflow(app, debugSessionId, bo, emitter, userId, true);
    }

    /**
     * 执行工作流（内部统一实现）
     * 
     * @param debug true=调试模式（不入库），false=正式模式（入库）
     */
    private Map<String, Object> executeWorkflow(KmAppVo app, Long sessionId, KmChatSendBo bo,
            SseEmitter emitter, Long userId, boolean debug) throws Exception {

        // 1. 解析工作流配置
        WorkflowConfig config = objectMapper.readValue(app.getDslData(), WorkflowConfig.class);
        if (config == null || config.getNodes() == null) {
            throw new RuntimeException("工作流配置无效");
        }

        log.info("执行工作流: appId={}, debug={}", app.getAppId(), debug);

        // 2. 创建或使用虚拟实例ID
        Long instanceId;
        if (debug) {
            instanceId = -1L; // 调试模式：虚拟ID
        } else {
            instanceId = instanceService.createInstance(app.getAppId(), sessionId,
                    app.getDslData());
        }

        Boolean showExecutionInfo = debug ? true
                : ("1".equals(app.getEnableExecutionDetail()) && Boolean.TRUE.equals(bo.getShowExecutionInfo()));
        // // 3. 初始化状态
        Map<String, Object> globalState = new HashMap<>();
        globalState.put(WorkflowState.KEY_INSTANCE_ID, instanceId);
        globalState.put(WorkflowState.KEY_USER_INPUT, bo.getMessage());
        globalState.put(WorkflowState.KEY_SESSION_ID, sessionId);
        globalState.put(WorkflowState.KEY_USER_ID, userId);
        globalState.put(WorkflowState.KEY_SHOW_EXECUTION_INFO, showExecutionInfo);

        // 初始化app参数
        // globalState.put(ChatWorkflowState.KEY_APP, app);

        if (debug) {
            globalState.put(WorkflowState.KEY_DEBUG, true); // 标记调试模式
        }

        Map<String, Object> initData = new HashMap<>();
        initData.put("globalState", globalState);

        WorkflowState chatWorkflowState = new WorkflowState(initData);

        String finalResponse = null;
        long startTime = System.currentTimeMillis();

        try {
            // 4. 执行工作流
            finalResponse = langGraphEngine.execute(config, chatWorkflowState, emitter);

            // 5. 标记实例完成（调试模式：跳过）
            if (!debug) {
                instanceService.completeInstance(instanceId);
            }

            // 6. 发送 done 事件（调试模式：包含统计信息）
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("sessionId", sessionId.toString());

            if (showExecutionInfo) {
                long durationMs = System.currentTimeMillis() - startTime;
                Integer totalTokens = (Integer) chatWorkflowState.data().get("totalTokens");
                doneData.put("totalTokens", totalTokens != null ? totalTokens : 0);
                doneData.put("durationMs", durationMs);
            }

            sendSseEvent(emitter, SseEventType.DONE, doneData);

            // 7. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("instanceId", instanceId);
            result.put("finalResponse", finalResponse != null ? finalResponse : "");

            // 调试模式：返回额外的统计信息
            if (showExecutionInfo) {
                long durationMs = System.currentTimeMillis() - startTime;
                Integer totalTokens = (Integer) chatWorkflowState.data().get("totalTokens");
                result.put("totalTokens", totalTokens != null ? totalTokens : 0);
                result.put("durationMs", durationMs);
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
        try {
            emitter.send(SseEmitter.event().name(eventType.getEventName()).data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }
}
