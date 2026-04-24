package org.dromara.ai.app.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.mapper.KmChatMessageMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * KMatrix 数据库聊天记忆存储实现
 * <p>
 * 实现 LangChain4j 的 {@link ChatMemoryStore} SPI，将对话记忆持久化到
 * {@code km_chat_message} 数据库表中。
 * <p>
 * 采用"差量持久化"策略：{@link #updateMessages} 仅保存相较于数据库现有记录新增的消息，
 * 避免全量删除重写带来的性能开销和数据丢失风险。
 * <p>
 * 业务元数据（userId、instanceId 等）通过 {@link ChatMemoryContextHolder} 传入。
 *
 * @author KMatrix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmChatMemoryStore implements ChatMemoryStore {

    private final KmChatMessageMapper messageMapper;

    /**
     * 根据会话ID（memoryId）加载历史消息
     *
     * @param memoryId 会话ID（Long 类型）
     * @return 按时间正序排列的 ChatMessage 列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Long sessionId = toSessionId(memoryId);
        if (sessionId == null) {
            return new ArrayList<>();
        }

        List<KmChatMessage> dbMessages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByAsc(KmChatMessage::getCreateTime)
        );

        List<ChatMessage> result = new ArrayList<>();
        for (KmChatMessage dbMsg : dbMessages) {
            ChatMessage chatMessage = deserialize(dbMsg);
            if (chatMessage != null) {
                result.add(chatMessage);
            }
        }

        log.debug("KmChatMemoryStore.getMessages: sessionId={}, loaded {} messages", sessionId, result.size());
        return result;
    }

    /**
     * 持久化更新后的消息列表
     * <p>
     * 采用差量策略：加载当前数据库中该会话的消息数量，只对新增的消息执行 INSERT。
     * 这样可以保留旧消息上已有的业务元数据（instanceId、feedbackStatus 等）。
     *
     * @param memoryId 会话ID（Long 类型）
     * @param messages 更新后的完整消息列表（由 TokenWindowChatMemory 裁剪后的结果）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long sessionId = toSessionId(memoryId);
        if (sessionId == null) {
            log.warn("KmChatMemoryStore.updateMessages: memoryId 转换失败, memoryId={}", memoryId);
            return;
        }

        // 获取当前数据库中已有的消息数量（用于差量计算）
        Long existingCount = messageMapper.selectCount(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
        );

        int totalIncoming = messages.size();
        int newMessageStart = existingCount.intValue();

        if (newMessageStart >= totalIncoming) {
            // 没有新增消息（可能是 Token 窗口裁剪导致消息数量不变或减少）
            log.debug("KmChatMemoryStore.updateMessages: 无新消息需要持久化, sessionId={}, existing={}, incoming={}",
                    sessionId, existingCount, totalIncoming);
            return;
        }

        // 读取业务上下文元数据
        ChatMemoryContextHolder.ChatMemoryContext ctx = ChatMemoryContextHolder.get();
        Long userId = ctx != null ? ctx.getUserId() : null;
        Long instanceId = ctx != null ? ctx.getInstanceId() : null;
        String requestId = ctx != null ? ctx.getRequestId() : null;

        // 只插入新增的消息
        List<ChatMessage> newMessages = messages.subList(newMessageStart, totalIncoming);
        for (ChatMessage newMsg : newMessages) {
            persist(sessionId, newMsg, userId, instanceId, requestId);
        }

        log.debug("KmChatMemoryStore.updateMessages: sessionId={}, 新增持久化 {} 条消息",
                sessionId, newMessages.size());
    }

    /**
     * 删除指定会话的所有记忆（通常在清除历史时调用）
     * <p>
     * 注意：此方法仅删除消息记录，不删除会话本身。
     *
     * @param memoryId 会话ID
     */
    @Override
    public void deleteMessages(Object memoryId) {
        Long sessionId = toSessionId(memoryId);
        if (sessionId == null) {
            return;
        }
        int deleted = messageMapper.delete(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
        );
        log.info("KmChatMemoryStore.deleteMessages: sessionId={}, 删除 {} 条消息", sessionId, deleted);
    }

    // =========================================================
    //  私有工具方法
    // =========================================================

    private Long toSessionId(Object memoryId) {
        if (memoryId instanceof Long l) {
            return l;
        }
        if (memoryId instanceof Number n) {
            return n.longValue();
        }
        if (memoryId instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                log.warn("KmChatMemoryStore: memoryId 无法转换为 Long: {}", memoryId);
            }
        }
        return null;
    }

    /**
     * 反序列化数据库记录为 ChatMessage
     * 优先使用 raw_message_json；回退到 role + content 兼容旧数据
     */
    private ChatMessage deserialize(KmChatMessage dbMsg) {
        // 优先：从 raw_message_json 反序列化（完整还原）
        if (dbMsg.getRawMessageJson() != null && !dbMsg.getRawMessageJson().isBlank()) {
            ChatMessage msg = ChatMessageJsonSerializer.fromJson(dbMsg.getRawMessageJson());
            if (msg != null) {
                return msg;
            }
            log.warn("raw_message_json 反序列化失败，回退到 role+content: messageId={}", dbMsg.getMessageId());
        }
        // 回退：根据 role + content 构造（兼容旧版数据）
        return ChatMessageJsonSerializer.buildFromRoleAndContent(dbMsg.getRole(), dbMsg.getContent());
    }

    /**
     * 将单条 ChatMessage 持久化到数据库
     */
    private void persist(Long sessionId, ChatMessage message, Long userId, Long instanceId, String requestId) {
        KmChatMessage dbMsg = new KmChatMessage();
        dbMsg.setSessionId(sessionId);
        dbMsg.setRole(mapRole(message));
        dbMsg.setContent(ChatMessageJsonSerializer.extractTextContent(message));
        dbMsg.setRawMessageJson(ChatMessageJsonSerializer.toJson(message));
        dbMsg.setInstanceId(instanceId);
        dbMsg.setRequestId(requestId);
        dbMsg.setCreateTime(new Date());
        dbMsg.setCreateBy(userId);
        dbMsg.setUpdateBy(userId);
        dbMsg.setUpdateTime(new Date());

        messageMapper.insert(dbMsg);
        log.debug("KmChatMemoryStore.persist: sessionId={}, role={}", sessionId, dbMsg.getRole());
    }

    /**
     * 将 ChatMessage 类型映射为 role 字符串
     */
    private String mapRole(ChatMessage message) {
        return switch (message.type()) {
            case USER -> "user";
            case AI -> "assistant";
            case SYSTEM -> "system";
            case TOOL_EXECUTION_RESULT -> "tool";
            default -> message.type().name().toLowerCase();
        };
    }
}
