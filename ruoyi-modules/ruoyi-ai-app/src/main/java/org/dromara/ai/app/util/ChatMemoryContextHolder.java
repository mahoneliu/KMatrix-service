package org.dromara.ai.app.util;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天记忆上下文持有者（基于 ThreadLocal）
 * <p>
 * 解决 {@code ChatMemoryStore} SPI 接口无法传递业务元数据的问题。
 * 在调用 LangChain4j 进行对话前，通过 {@link #set(ChatMemoryContext)} 注入当前请求的业务上下文；
 * {@code KmChatMemoryStore} 在持久化消息时通过 {@link #get()} 取出并绑定到数据库记录。
 * <p>
 * 使用规范：
 * <pre>
 * try {
 *     ChatMemoryContextHolder.set(...);
 *     // 执行 LangChain4j 对话
 * } finally {
 *     ChatMemoryContextHolder.clear(); // 必须在 finally 中清理，避免线程池复用导致的上下文泄漏
 * }
 * </pre>
 *
 * @author KMatrix
 */
@Slf4j
public class ChatMemoryContextHolder {

    private static final ThreadLocal<ChatMemoryContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前线程的记忆上下文
     */
    public static void set(ChatMemoryContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程的记忆上下文
     *
     * @return 当前上下文，未设置时返回 null
     */
    public static ChatMemoryContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程的记忆上下文（防止内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 聊天记忆业务上下文模型
     */
    @Data
    @Builder
    public static class ChatMemoryContext {

        /**
         * 会话ID（km_chat_message.session_id）
         */
        private Long sessionId;

        /**
         * 当前用户ID
         */
        private Long userId;

        /**
         * 工作流实例ID（可选，非工作流场景为 null）
         */
        private Long instanceId;

        /**
         * 请求唯一标识（用于关联前端请求）
         */
        private String requestId;
    }
}
