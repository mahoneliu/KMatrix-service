package org.dromara.ai.workflow.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点输出
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
public class NodeOutput {

    /**
     * 输出参数
     */
    private Map<String, Object> outputs = new HashMap<>();

    /**
     * 下一个节点ID (用于条件分支)
     */
    private String nextNode;

    /**
     * 是否结束工作流
     */
    private boolean finished = false;

    /**
     * 添加输出参数
     */
    public void addOutput(String key, Object value) {
        outputs.put(key, value);
    }

    /**
     * 获取输出参数
     */
    public Object getOutput(String key) {
        return outputs.get(key);
    }
}
