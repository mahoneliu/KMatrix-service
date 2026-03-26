package org.dromara.ai.workflow.workflow.nodes.chat;
import dev.langchain4j.data.message.ChatMessage;
import java.util.List;
public interface IChatMessageProvider {
    List<ChatMessage> loadHistoryMessages(Long sessionId, Integer limit);
}