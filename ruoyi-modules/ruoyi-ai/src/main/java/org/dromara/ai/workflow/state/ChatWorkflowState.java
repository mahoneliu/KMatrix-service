package org.dromara.ai.workflow.state;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流状态类
 * 用于在 LangGraph4j 工作流节点间传递数据
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
public class ChatWorkflowState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户输入
     */
    private String userInput;

    /**
     * AI响应
     */
    private String aiResponse;

    /**
     * 意图识别结果 (greeting: 打招呼, question: 咨询问题, other: 其他)
     */
    private String intent;

    /**
     * 对话历史消息列表
     */
    private List<String> messages = new ArrayList<>();

    /**
     * 是否完成
     */
    private boolean finished = false;

    /**
     * 添加消息到历史
     */
    public void addMessage(String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
}
