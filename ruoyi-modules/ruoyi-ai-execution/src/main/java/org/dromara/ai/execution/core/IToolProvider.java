package org.dromara.ai.execution.core;

import java.util.List;
import java.util.Map;

/**
 * 工具提供服务接口
 * <p>
 * 根据工具引用配置解析并返回 {@link ToolBinding} 列表。
 * 每个绑定包含 LangChain4j {@link dev.langchain4j.agent.tool.ToolSpecification}
 * 和对应的 {@link ToolExecutor}。
 *
 * @author Mahone
 * @date 2026-03-20
 */
public interface IToolProvider {

    /**
     * 根据节点 tools 配置解析工具绑定列表
     *
     * @param toolRefs 工具引用列表，格式：[{"type":"builtin","id":123},
     *                 {"type":"mcp","id":456}]
     * @return 工具绑定列表（可直接用于注入 LLM）
     */
    List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs);

    /**
     * 获取指定技能的底层工具绑定集合
     *
     * @param skillId 技能ID
     * @return 技能内部的工具绑定列表
     */
    List<ToolBinding> resolveSkillInnerBindings(Long skillId);
}
