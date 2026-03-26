package org.dromara.ai.workflow.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.ai.workflow.domain.bo.WorkflowExecutionReq;
import org.dromara.ai.workflow.service.IWorkflowInstanceService;
import org.dromara.ai.workflow.workflow.core.WorkflowConfig;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.dromara.ai.workflow.workflow.engine.LangGraphWorkflowEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
public class WorkflowExecutorTest {

    @Mock
    private LangGraphWorkflowEngine langGraphEngine;

    @Mock
    private IWorkflowInstanceService instanceService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkflowExecutor workflowExecutor;

    private WorkflowExecutionReq req;

    @BeforeEach
    void setUp() {
        req = WorkflowExecutionReq.builder()
                .appId(1L)
                .sessionId(123L)
                .userId(456L)
                .message("Hello AI")
                .dslData("{\"nodes\":[{\"id\":\"node1\",\"type\":\"START\"}]}")
                .build();
    }

    @Test
    void testExecuteWorkflowDebug() throws Exception {
        // Arrange
        WorkflowConfig config = new WorkflowConfig();
        config.setNodes(Collections.singletonList(new WorkflowConfig.NodeConfig()));
        
        when(objectMapper.readValue(anyString(), eq(WorkflowConfig.class))).thenReturn(config);
        
        WorkflowState finalState = mock(WorkflowState.class);
        Map<String, Object> data = new HashMap<>();
        data.put(WorkflowState.KEY_TOTAL_TOKENS, 100);
        when(finalState.data()).thenReturn(data);
        when(finalState.getFinalResponse()).thenReturn("AI Response");
        
        when(langGraphEngine.execute(any(), any(), any())).thenReturn(finalState);

        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        Map<String, Object> result = workflowExecutor.executeWorkflowDebug(req, emitter);

        // Assert
        assertNotNull(result);
        assertEquals("AI Response", result.get("finalResponse"));
        assertEquals(100, result.get(WorkflowState.KEY_TOTAL_TOKENS));
        assertEquals(-1L, result.get("instanceId"));

        verify(instanceService, never()).createInstance(any(), any(), any());
        verify(langGraphEngine).execute(eq(config), any(WorkflowState.class), eq(emitter));
    }
}
