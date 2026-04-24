package org.dromara.ai.workflow.workflow.nodes.tool;
import java.util.List;
import java.util.Map;
/**
 * 工具提供服务接口
 *
 * @deprecated 已迁移至 {@link org.dromara.ai.execution.core.IToolProvider}
 * @author Mahone
 * @date 2026-03-20
 */
@Deprecated
public interface IToolProvider {
    List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs);
    List<ToolBinding> resolveSkillInnerBindings(Long skillId);
}