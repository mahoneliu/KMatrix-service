package org.dromara.ai.workflow.nodes.tool;

import cn.hutool.json.JSONUtil;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.domain.KmBuiltinTool;
import org.dromara.ai.domain.KmSkill;
import org.dromara.ai.mapper.KmBuiltinToolMapper;
import org.dromara.ai.mapper.KmMcpServerMapper;
import org.dromara.ai.mapper.KmSkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ToolProviderServiceTest extends BaseUnitTest {

    @Mock
    private KmBuiltinToolMapper builtinToolMapper;
    @Mock
    private KmMcpServerMapper mcpServerMapper;
    @Mock
    private KmSkillMapper skillMapper;

    private ToolProviderService toolProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        toolProviderService = new ToolProviderService(builtinToolMapper, mcpServerMapper, skillMapper);
    }

    @Test
    void testResolveBuiltinBindings() {
        KmBuiltinTool builtin = new KmBuiltinTool();
        builtin.setToolId(1L);
        builtin.setToolName("T1");
        builtin.setStatus("0");
        builtin.setInputSchema("{}");
        when(builtinToolMapper.selectById(any())).thenReturn(builtin);

        List<Map<String, Object>> refs = new ArrayList<>();
        Map<String, Object> ref = new HashMap<>();
        ref.put("type", "builtin");
        ref.put("id", 1L);
        refs.add(ref);

        List<ToolBinding> result = toolProviderService.resolveBindings(refs);
        assertEquals(1, result.size());
        assertEquals("T1", result.get(0).getToolName());
    }

    @Test
    void testResolveSkillBindings() {
        KmSkill skill = new KmSkill();
        skill.setSkillId(10L);
        skill.setSkillName("S1");
        skill.setStatus("0");
        skill.setToolBindings("[{\"type\":\"builtin\",\"id\":1}]");
        skill.setInputSchema("{}");
        when(skillMapper.selectById(any())).thenReturn(skill);

        KmBuiltinTool builtin = new KmBuiltinTool();
        builtin.setToolId(1L);
        builtin.setToolName("T1");
        builtin.setStatus("0");
        builtin.setInputSchema("{}");
        when(builtinToolMapper.selectById(any())).thenReturn(builtin);

        List<Map<String, Object>> refs = new ArrayList<>();
        Map<String, Object> ref = new HashMap<>();
        ref.put("type", "skill");
        ref.put("id", 10L);
        refs.add(ref);

        List<ToolBinding> result = toolProviderService.resolveBindings(refs);
        // Skill 解析后包装为一个同名的 ToolBinding
        assertEquals(1, result.size());
        assertEquals("S1", result.get(0).getToolName());
    }

    @Test
    void testEmptyBindings() {
        List<ToolBinding> result = toolProviderService.resolveBindings(new ArrayList<>());
        assertTrue(result.isEmpty());
    }
}
