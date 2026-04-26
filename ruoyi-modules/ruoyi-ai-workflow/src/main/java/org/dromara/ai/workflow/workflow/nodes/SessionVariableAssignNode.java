package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.session.ISessionVariableProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话变量赋值节点
 * <p>
 * 对 AppInfo 节点中定义的会话变量（sessionParams）进行赋值操作。
 * 会话变量持久化存储在 km_chat_session.session_variables（JSONB）字段中，
 * 在同一聊天会话的多个对话轮次之间保持状态。
 *
 * <h3>支持的操作模式</h3>
 * <ul>
 *   <li><b>overwrite（覆写）</b>：用另一个变量引用或表达式的值替换当前值</li>
 *   <li><b>clear（清除）</b>：将变量值设为 null（移除当前值）</li>
 *   <li><b>set（设置）</b>：手动分配一个固定值</li>
 * </ul>
 *
 * <h3>节点配置结构（config.assignments）</h3>
 * <pre>
 * [
 *   {
 *     "variableName": "userName",   // 会话变量名（对应 sessionParams 中的 key）
 *     "mode": "overwrite",          // 操作模式：overwrite / clear / set
 *     "sourceValue": "{{node1.output}}" // 源数据（overwrite/set 时有效）
 *   }
 * ]
 * </pre>
 *
 * @author Mahone
 * @date 2026-05-01
 */
@Slf4j
@Component("SESSION_VARIABLE_ASSIGN")
public class SessionVariableAssignNode extends AbstractWorkflowNode {

    /** 操作模式：覆写（用另一个变量或表达式的值替换） */
    public static final String MODE_OVERWRITE = "overwrite";
    /** 操作模式：清除（设为 null） */
    public static final String MODE_CLEAR = "clear";
    /** 操作模式：设置（手动分配固定值） */
    public static final String MODE_SET = "set";

    /** 全局状态中存储会话变量的 key */
    public static final String GLOBAL_KEY_SESSION_VARS = "sessionVariables";

    @Autowired(required = false)
    private ObjectProvider<ISessionVariableProvider> sessionVariableProviderHolder;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 SESSION_VARIABLE_ASSIGN 节点");

        NodeOutput output = new NodeOutput();
        Long sessionId = context.getSessionId();

        // 1. 读取赋值配置列表
        Object assignmentsObj = context.getConfig("assignments");
        List<Map<String, Object>> assignments = parseAssignments(assignmentsObj);

        if (assignments.isEmpty()) {
            log.warn("SESSION_VARIABLE_ASSIGN 节点：未配置任何赋值项，跳过执行");
            output.addOutput("sessionVariables", new HashMap<>());
            return output;
        }

        // 2. 加载当前会话变量（优先从 globalState 取，其次从数据库加载）
        Map<String, Object> sessionVars = loadCurrentSessionVariables(context, sessionId);

        // 3. 执行赋值操作
        for (Map<String, Object> assignment : assignments) {
            String variableName = (String) assignment.get("variableName");
            String mode = (String) assignment.get("mode");
            Object sourceValue = assignment.get("sourceValue");

            if (StrUtil.isBlank(variableName)) {
                log.warn("SESSION_VARIABLE_ASSIGN 节点：赋值项缺少 variableName，跳过");
                continue;
            }
            if (StrUtil.isBlank(mode)) {
                log.warn("SESSION_VARIABLE_ASSIGN 节点：赋值项 [{}] 缺少 mode，跳过", variableName);
                continue;
            }

            switch (mode) {
                case MODE_CLEAR -> {
                    // 清除：将变量设为 null
                    sessionVars.put(variableName, null);
                    log.info("SESSION_VARIABLE_ASSIGN：清除变量 [{}]", variableName);
                }
                case MODE_OVERWRITE, MODE_SET -> {
                    // 覆写/设置：解析源数据值
                    Object resolvedValue = resolveValue(sourceValue, context);
                    sessionVars.put(variableName, resolvedValue);
                    log.info("SESSION_VARIABLE_ASSIGN：{} 变量 [{}] = [{}]",
                            MODE_OVERWRITE.equals(mode) ? "覆写" : "设置", variableName, resolvedValue);
                }
                default -> log.warn("SESSION_VARIABLE_ASSIGN 节点：未知操作模式 [{}]，跳过变量 [{}]", mode, variableName);
            }
        }

        // 4. 将更新后的会话变量写回 globalState（使后续节点可以引用）
        context.setGlobalValue(GLOBAL_KEY_SESSION_VARS, sessionVars);

        // 5. 持久化到数据库（仅在有 sessionId 且非调试模式时）
        if (sessionId != null) {
            ISessionVariableProvider provider = sessionVariableProviderHolder != null
                    ? sessionVariableProviderHolder.getIfAvailable() : null;
            if (provider != null) {
                provider.saveSessionVariables(sessionId, sessionVars);
                log.info("SESSION_VARIABLE_ASSIGN：会话变量已持久化，sessionId={}, 变量数={}",
                        sessionId, sessionVars.size());
            } else {
                log.warn("SESSION_VARIABLE_ASSIGN：ISessionVariableProvider 不可用，会话变量仅更新到内存，未持久化");
            }
        } else {
            log.debug("SESSION_VARIABLE_ASSIGN：sessionId 为空（调试模式），跳过持久化");
        }

        // 6. 输出更新后的会话变量
        output.addOutput("sessionVariables", sessionVars);
        log.info("SESSION_VARIABLE_ASSIGN 节点执行完成，共处理 {} 个变量", assignments.size());
        return output;
    }

    /**
     * 加载当前会话变量
     * <p>
     * 优先从 globalState 取（本次工作流执行中已加载），
     * 若不存在则从数据库加载并写入 globalState。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadCurrentSessionVariables(NodeContext context, Long sessionId) {
        // 优先从 globalState 取
        Object existing = context.getGlobalValue(GLOBAL_KEY_SESSION_VARS);
        if (existing instanceof Map) {
            return new HashMap<>((Map<String, Object>) existing);
        }

        // 从数据库加载
        if (sessionId != null) {
            ISessionVariableProvider provider = sessionVariableProviderHolder != null
                    ? sessionVariableProviderHolder.getIfAvailable() : null;
            if (provider != null) {
                Map<String, Object> dbVars = provider.loadSessionVariables(sessionId);
                if (dbVars != null && !dbVars.isEmpty()) {
                    log.debug("SESSION_VARIABLE_ASSIGN：从数据库加载会话变量，sessionId={}, 变量数={}",
                            sessionId, dbVars.size());
                    return new HashMap<>(dbVars);
                }
            }
        }

        return new HashMap<>();
    }

    /**
     * 解析源数据值
     * <p>
     * 如果 sourceValue 是字符串且以 "{{" 开头，则尝试从节点输出中解析变量引用。
     * 否则直接返回原始值。
     */
    private Object resolveValue(Object sourceValue, NodeContext context) {
        if (!(sourceValue instanceof String strVal)) {
            return sourceValue;
        }

        // 简单变量引用解析：支持 {{nodeId.outputKey}} 格式
        String trimmed = strVal.trim();
        if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
            String ref = trimmed.substring(2, trimmed.length() - 2).trim();
            String[] parts = ref.split("\\.", 2);
            if (parts.length == 2) {
                String nodeId = parts[0];
                String outputKey = parts[1];
                Map<String, Map<String, Object>> allOutputs = context.getAllNodeOutputs();
                if (allOutputs != null) {
                    Map<String, Object> nodeOutput = allOutputs.get(nodeId);
                    if (nodeOutput != null && nodeOutput.containsKey(outputKey)) {
                        Object resolved = nodeOutput.get(outputKey);
                        log.debug("SESSION_VARIABLE_ASSIGN：解析变量引用 [{}] = [{}]", ref, resolved);
                        return resolved;
                    }
                }
                // 也尝试从 globalState 解析（如 sessionId、userInput 等全局变量）
                Object globalVal = context.getGlobalValue(ref);
                if (globalVal != null) {
                    return globalVal;
                }
                log.warn("SESSION_VARIABLE_ASSIGN：无法解析变量引用 [{}]，返回原始字符串", ref);
            }
        }

        return strVal;
    }

    /**
     * 解析赋值配置列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAssignments(Object assignmentsObj) {
        if (assignmentsObj instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) assignmentsObj) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public String getNodeType() {
        return "SESSION_VARIABLE_ASSIGN";
    }

    @Override
    public String getNodeName() {
        return "会话变量赋值";
    }
}
