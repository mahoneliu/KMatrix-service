package org.dromara.ai.execution.core.impl;

import org.dromara.ai.execution.core.IToolProvider;
import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.core.ToolExecutor;
import org.dromara.ai.execution.domain.KmBuiltinTool;
import org.dromara.ai.execution.mapper.KmBuiltinToolMapper;
import org.dromara.ai.execution.mcp.client.McpToolAdapter;
import org.dromara.ai.execution.mcp.service.McpClientManager;
import org.dromara.ai.execution.skill.service.SkillOrchestrationService;
import org.dromara.ai.execution.tool.executor.PythonBuiltinExecutor;
import org.dromara.ai.execution.tool.util.ToolJsonSchemaUtils;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具提供服务核心实现
 * <p>
 * 统一协调 BuiltinTool、MCP、Skill 三类工具的解析与绑定。
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Primary
public class ToolProviderServiceImpl implements IToolProvider {

    private final KmBuiltinToolMapper builtinToolMapper;
    private final McpClientManager mcpClientManager;
    private final SkillOrchestrationService skillOrchestrationService;

    /**
     * 根据节点 tools 配置解析工具绑定列表
     *
     * @param toolRefs 工具引用列表，格式：[{"type":"builtin","id":123},
     *                 {"type":"mcp","id":456}]
     * @return 工具绑定列表（可直接用于注入 LLM）
     */
    @Override
    public List<ToolBinding> resolveBindings(List<Map<String, Object>> toolRefs) {
        List<ToolBinding> bindings = new ArrayList<>();
        if (toolRefs == null || toolRefs.isEmpty()) {
            return bindings;
        }

        for (Map<String, Object> toolRef : toolRefs) {
            String type = (String) toolRef.get("type");
            Object idObj = toolRef.get("id");
            log.debug("ToolProviderServiceImpl - 解析引用: type={}, id={}", type, idObj);
            if (StrUtil.isBlank(type) || idObj == null) {
                continue;
            }

            Long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());

            try {
                if ("builtin".equalsIgnoreCase(type)) {
                    List<ToolBinding> builtinBindings = resolveBuiltinTool(id);
                    bindings.addAll(builtinBindings);
                } else if ("mcp".equalsIgnoreCase(type)) {
                    List<ToolBinding> mcpBindings = resolveMcpTools(id);
                    bindings.addAll(mcpBindings);
                } else if ("skill".equalsIgnoreCase(type)) {
                    List<ToolBinding> skillBindings = skillOrchestrationService.resolveSkillAsBinding(id);
                    bindings.addAll(skillBindings);
                } else {
                    log.warn("未知工具/技能类型: type={}", type);
                }
            } catch (Exception e) {
                log.error("解析工具绑定失败: type={}, id={}", type, id, e);
            }
        }

        log.info("工具绑定解析完成，共 {} 个工具", bindings.size());
        return bindings;
    }

    /**
     * 获取指定技能的底层工具绑定集合
     */
    @Override
    public List<ToolBinding> resolveSkillInnerBindings(Long skillId) {
        return skillOrchestrationService.getInnerBindings(skillId);
    }

    /**
     * 解析内置 Python 工具绑定
     */
    private List<ToolBinding> resolveBuiltinTool(Long toolId) {
        List<ToolBinding> result = new ArrayList<>();

        KmBuiltinTool tool = builtinToolMapper.selectById(toolId);
        if (tool == null) {
            log.error("内置工具不存在 (DB查询失败): toolId={}", toolId);
            return result;
        }
        log.debug("解析内置工具详情: toolName={}, status={}, inputSchema={}, pythonCode长度={}",
                tool.getToolName(), tool.getStatus(), tool.getInputSchema(),
                tool.getPythonCode() == null ? 0 : tool.getPythonCode().length());
        if (!"0".equals(tool.getStatus())) {
            log.warn("内置工具已停用: toolId={}, toolName={}", toolId, tool.getToolName());
            return result;
        }

        var spec = ToolJsonSchemaUtils.buildToolSpecification(
                tool.getToolName(),
                tool.getSpec(),
                tool.getInputSchema());

        ToolExecutor executor = new PythonBuiltinExecutor(tool.getToolName(), tool.getPythonCode());

        result.add(ToolBinding.builder()
                .toolName(tool.getToolName())
                .specification(spec)
                .executor(executor)
                .type("builtin")
                .sourceId(toolId)
                .build());

        log.info("内置工具绑定成功: toolName={}", tool.getToolName());
        return result;
    }

    /**
     * 解析 MCP Server 工具绑定
     */
    private List<ToolBinding> resolveMcpTools(Long serverId) {
        return McpToolAdapter.adaptTools(serverId, mcpClientManager);
    }
}