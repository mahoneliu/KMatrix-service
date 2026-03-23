package org.dromara.ai.workflow.core;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
public class WorkflowStateTest {

    @Test
    public void testTotalTokensAccessor() {
        WorkflowState state1 = new WorkflowState();
        assertEquals(0, state1.getTotalTokens());
        
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put(WorkflowState.KEY_TOTAL_TOKENS, 150);
        WorkflowState state2 = new WorkflowState(data);
        assertEquals(150, state2.getTotalTokens());
    }
}
