package org.dromara.ai.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.enums.SseEventType;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.config.WorkflowConfig;
import org.dromara.ai.workflow.engine.LangGraphWorkflowEngine;
import org.dromara.ai.workflow.state.ChatWorkflowState;
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
     * 执行工作流（统一入口）
     */
    public Map<String, Object> executeWorkflow(KmAppVo app, Long sessionId, String userInput,
            SseEmitter emitter, Long userId) throws Exception {

        // 1. 解析工作流配置
        WorkflowConfig config = objectMapper.readValue(app.getDslData(), WorkflowConfig.class);
        if (config == null || config.getNodes() == null) {
            throw new RuntimeException("工作流配置无效");
        }

        log.info("使用 LangGraph 工作流引擎");

        // 2. 创建工作流实例
        Long instanceId = instanceService.createInstance(app.getAppId(), sessionId, app.getDslData());

        // 3. 初始化状态
        // 构建 globalState map，存储基础信息（适配 LangGraph4j 的 AsyncNodeAction）
        Map<String, Object> globalState = new HashMap<>();
        globalState.put("userInput", userInput);
        globalState.put("sessionId", sessionId);
        globalState.put("instanceId", instanceId);
        globalState.put("userId", userId);

        // 创建初始状态，并设置 globalState
        Map<String, Object> initData = new HashMap<>();
        initData.put("globalState", globalState);

        ChatWorkflowState chatWorkflowState = new ChatWorkflowState(initData);

        String finalResponse = null;

        try {
            // 4. 执行工作流（传递 emitter 给引擎）
            finalResponse = langGraphEngine.execute(config, chatWorkflowState, emitter);

            // 5. 标记实例完成
            instanceService.completeInstance(instanceId);

            // 6. 发送 done 事件
            sendSseEvent(emitter, SseEventType.DONE, Map.of("sessionId", sessionId.toString()));

        } catch (Exception e) {
            instanceService.failInstance(instanceId, e.getMessage());
            sendSseEvent(emitter, SseEventType.NODE_ERROR, Map.of("error", e.getMessage()));
            throw e;
        }

        return Map.of(
                "instanceId", instanceId,
                "finalResponse", finalResponse != null ? finalResponse : "");
    }

    private void sendSseEvent(SseEmitter emitter, SseEventType eventType, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(eventType.getEventName()).data(data));
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }
}
