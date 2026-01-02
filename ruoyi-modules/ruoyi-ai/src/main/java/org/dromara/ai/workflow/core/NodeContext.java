package org.dromara.ai.workflow.core;

import lombok.Data;
import org.dromara.ai.domain.vo.KmAppVo;
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

    /**
     * 应用信息
     */
    private KmAppVo app;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 工作流实例ID
     */
    private Long instanceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否为新会话（首次对话）
     */
    private boolean isNewSession;

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
}
