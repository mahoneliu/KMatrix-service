package org.dromara.ai.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.mapper.KmChatMessageMapper;
import org.dromara.ai.workflow.workflow.nodes.chat.IChatMessageProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 历史聊天消息提供者实现
 * 供工作流中的 LLM_CHAT 节点调用，注入以避免循环依赖
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmChatMessageProviderImpl implements IChatMessageProvider {

    private final KmChatMessageMapper messageMapper;

    @Override
    public List<ChatMessage> loadHistoryMessages(Long sessionId, Integer limit) {
        if (sessionId == null || limit == null || limit <= 0) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = new ArrayList<>();

        // 1. 加载历史消息(按照时间倒序获取最近的 N 条，以获取最新的聊天记录)
        List<KmChatMessage> historyMessages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByDesc(KmChatMessage::getCreateTime)
                        .last("LIMIT " + limit)
        );

        if (historyMessages == null || historyMessages.isEmpty()) {
            return messages;
        }

        // 2. 反转为时间正序（供给大模型时需要顺序排列）
        Collections.reverse(historyMessages);

        // 3. 转换为 LangChain4j 的 ChatMessage 格式
        for (KmChatMessage msg : historyMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AiMessage(msg.getContent()));
            }
        }

        log.debug("成功加载会话 {} 的 {} 条历史消息", sessionId, historyMessages.size());
        return messages;
    }
}
