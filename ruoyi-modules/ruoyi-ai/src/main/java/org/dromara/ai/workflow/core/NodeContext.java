package org.dromara.ai.workflow.core;

import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行上下文
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
public class NodeContext {

    /**
     * 全局状态(所有节点共享)
     */
    private Map<String, Object> globalState = new HashMap<>();

    /**
     * 当前节点输入参数
     */
    private Map<String, Object> nodeInputs = new HashMap<>();

    /**
     * 当前节点配置
     */
    private Map<String, Object> nodeConfig = new HashMap<>();

    /**
     * SSE推送器
     */
    private SseEmitter sseEmitter;

    // /**
    // * 会话ID
    // */
    // private Long sessionId;

    // /**
    // * 工作流实例ID
    // */
    // private Long instanceId;

    // /**
    // * 用户ID
    // */
    // private Long userId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 开始时间(毫秒)
     */
    private Long startTime;

    /**
     * Token使用统计
     */
    private Map<String, Object> tokenUsage = new HashMap<>();

    /**
     * 获取全局状态值
     */
    public Object getGlobalValue(String key) {
        return globalState.get(key);
    }

    /**
     * 设置全局状态值
     */
    public void setGlobalValue(String key, Object value) {
        globalState.put(key, value);
    }

    /**
     * 获取节点输入参数
     */
    public Object getInput(String key) {
        return nodeInputs.get(key);
    }

    /**
     * 设置节点输入参数
     */
    public void setInput(String key, Object value) {
        nodeInputs.put(key, value);
    }

    /**
     * 获取节点配置参数
     */
    public Object getConfig(String key) {
        return nodeConfig.get(key);
    }

    /**
     * 设置节点配置参数
     */
    public void setConfig(String key, Object value) {
        nodeConfig.put(key, value);
    }

    /**
     * 获取配置参数(Long类型)
     */
    public Long getConfigAsLong(String key) {
        Object value = getConfig(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }

    /**
     * 获取配置参数(String类型)
     */
    public String getConfigAsString(String key) {
        Object value = getConfig(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取配置参数(Integer类型)
     */
    public Integer getConfigAsInteger(String key, Integer defaultValue) {
        Object value = getConfig(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取配置参数(Double类型)
     */
    public Double getConfigAsDouble(String key, Double defaultValue) {
        Object value = getConfig(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取配置参数(Boolean类型)
     */
    public Boolean getConfigAsBoolean(String key, Boolean defaultValue) {
        Object value = getConfig(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    // ========== 条件评估辅助方法 ==========

    /**
     * 所有节点的输出（用于条件评估）
     */
    private Map<String, Map<String, Object>> allNodeOutputs = new HashMap<>();

    /**
     * 获取所有节点输出
     */
    public Map<String, Map<String, Object>> getAllNodeOutputs() {
        return allNodeOutputs;
    }

    /**
     * 设置所有节点输出
     */
    public void setAllNodeOutputs(Map<String, Map<String, Object>> outputs) {
        this.allNodeOutputs = outputs != null ? outputs : new HashMap<>();
    }

    /**
     * 设置全局变量（别名方法，与 setGlobalValue 功能相同）
     */
    public void setGlobalVariable(String key, Object value) {
        setGlobalValue(key, value);
    }

    // ========== 基础信息访问方法 ==========

    /**
     * 获取会话ID
     * 从 globalState 中获取，用于加载历史对话
     */
    public Long getSessionId() {
        Object value = globalState.get("sessionId");
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
