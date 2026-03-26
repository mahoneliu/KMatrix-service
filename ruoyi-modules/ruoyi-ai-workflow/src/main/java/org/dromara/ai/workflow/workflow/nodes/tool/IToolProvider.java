package org.dromara.ai.workflow.workflow.nodes.tool;
import java.util.List;
import java.util.Map;
public interface IToolProvider {
    List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs);
    List<ToolBinding> resolveSkillInnerBindings(Long skillId);
}