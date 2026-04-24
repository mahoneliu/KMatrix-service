package org.dromara.ai.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.mapper.KmChatMessageMapper;
import org.dromara.ai.app.util.ChatMessageJsonSerializer;
import org.dromara.ai.workflow.workflow.nodes.chat.IChatMessageProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 历史聊天消息提供者实现
 * <p>
 * 供工作流中的 LLM_CHAT 节点调用，注入以避免循环依赖。
 * 优先从 {@code raw_message_json} 反序列化以还原完整消息（多模态/工具链），
 * 对旧版纯文本记录保持向后兼容。
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

        // 1. 加载历史消息（按时间倒序获取最近的 N 条）
        List<KmChatMessage> historyMessages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByDesc(KmChatMessage::getCreateTime)
                        .last("LIMIT " + limit)
        );

        if (historyMessages == null || historyMessages.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 反转为时间正序（供给大模型时需要顺序排列）
        Collections.reverse(historyMessages);

        // 3. 反序列化为 ChatMessage 列表
        List<ChatMessage> messages = new ArrayList<>(historyMessages.size());
        for (KmChatMessage dbMsg : historyMessages) {
            ChatMessage chatMessage = deserialize(dbMsg);
            if (chatMessage != null) {
                messages.add(chatMessage);
            }
        }

        log.debug("成功加载会话 {} 的 {} 条历史消息（请求 {} 条）",
                sessionId, messages.size(), historyMessages.size());
        return messages;
    }

    /**
     * 反序列化数据库记录为 ChatMessage
     * <p>
     * 策略：
     * 1. 优先从 raw_message_json 反序列化（完整还原多模态和工具链上下文）
     * 2. 若 raw_message_json 为空或解析失败，回退到 role+content 构建简单消息（向后兼容旧数据）
     */
    private ChatMessage deserialize(KmChatMessage dbMsg) {
        // 优先：完整反序列化
        if (dbMsg.getRawMessageJson() != null && !dbMsg.getRawMessageJson().isBlank()) {
            ChatMessage msg = ChatMessageJsonSerializer.fromJson(dbMsg.getRawMessageJson());
            if (msg != null) {
                return msg;
            }
            log.warn("raw_message_json 反序列化失败，降级回退: messageId={}", dbMsg.getMessageId());
        }

        // 回退：基于 role + content 构造（兼容旧版数据）
        return ChatMessageJsonSerializer.buildFromRoleAndContent(dbMsg.getRole(), dbMsg.getContent());
    }
}
