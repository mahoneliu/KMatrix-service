package org.dromara.ai.workflow.nodes;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.nodes.tool.ToolBinding;
import org.dromara.ai.workflow.nodes.tool.ToolExecutor;
import org.dromara.ai.workflow.nodes.tool.ToolProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 技能节点执行测试
 */
class SkillExecutionNodeTest extends BaseUnitTest {

    @Mock
    private ToolProviderService toolProviderService;

    @InjectMocks
    private SkillExecutionNode skillNode;

    @Mock
    private NodeContext nodeContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        skillNode = new SkillExecutionNode(toolProviderService);
    }

    @Test
    @DisplayName("执行包含单个工具的技能")
    void testExecuteSingleToolSkill() throws Exception {
        Long skillId = 1L;
        when(nodeContext.getConfigAsLong("skillId")).thenReturn(skillId);
        when(nodeContext.getNodeInputs()).thenReturn(Collections.singletonMap("query", "test"));

        ToolExecutor mockExecutor = mock(ToolExecutor.class);
        when(mockExecutor.execute(any())).thenReturn("{\"answer\":\"success\"}");

        ToolBinding binding = ToolBinding.builder()
                .toolName("TestTool")
                .executor(mockExecutor)
                .build();

        when(toolProviderService.resolveSkillInnerBindings(any())).thenReturn(Collections.singletonList(binding));

        NodeOutput output = skillNode.execute(nodeContext);

        assertNotNull(output);
        assertEquals("success", output.getOutputs().get("answer"));
        verify(mockExecutor).execute(anyString());
    }

    @Test
    @DisplayName("执行包含多个工具的技能并合并结果")
    void testExecuteMultiToolSkill() throws Exception {
        Long skillId = 1L;
        when(nodeContext.getConfigAsLong("skillId")).thenReturn(skillId);

        ToolExecutor exec1 = mock(ToolExecutor.class);
        when(exec1.execute(anyString())).thenReturn("{\"part1\":\"ok\"}");

        ToolExecutor exec2 = mock(ToolExecutor.class);
        when(exec2.execute(anyString())).thenReturn("{\"part2\":\"done\"}");

        ToolBinding b1 = ToolBinding.builder().toolName("T1").executor(exec1).build();
        ToolBinding b2 = ToolBinding.builder().toolName("T2").executor(exec2).build();

        when(toolProviderService.resolveSkillInnerBindings(any())).thenReturn(Arrays.asList(b1, b2));

        NodeOutput output = skillNode.execute(nodeContext);

        assertNotNull(output);
        Object resObj = output.getOutputs().get("result");
        assertTrue(resObj instanceof Map, "Result should be a Map");
        Map<String, Object> results = (Map<String, Object>) resObj;
        assertEquals("ok", results.get("part1"));
        assertEquals("done", results.get("part2"));
        
        String text = (String) output.getOutputs().get("text");
        assertTrue(text.contains("ok"));
        assertTrue(text.contains("done"));
    }

    @Test
    @DisplayName("执行未配置技能ID的节点应抛出异常")
    void testExecuteWithoutSkillId() {
        when(nodeContext.getConfigAsLong("skillId")).thenReturn(null);
        
        Exception exception = assertThrows(RuntimeException.class, () -> skillNode.execute(nodeContext));
        assertTrue(exception.getMessage().contains("skillId"));
    }

    @Test
    @DisplayName("执行未绑定工具的技能应抛出异常")
    void testExecuteWithoutBindings() {
        when(nodeContext.getConfigAsLong(anyString())).thenReturn(1L);
        when(toolProviderService.resolveSkillInnerBindings(any())).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> skillNode.execute(nodeContext));
    }
}
