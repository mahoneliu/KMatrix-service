package org.dromara.ai.app.util;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j ChatMessage 序列化工具类
 * <p>
 * 提供 ChatMessage 列表与 JSON 字符串之间的互转能力，
 * 支持所有消息类型：UserMessage、AiMessage、SystemMessage、ToolExecutionResultMessage 等。
 * <p>
 * 注意：LangChain4j 1.x 的 {@code dev.langchain4j.data.message.ChatMessageSerializer} 仅支持
 * 单条消息，此工具类在其基础上封装了列表操作，并增加了兼容性处理。
 *
 * @author KMatrix
 */
@Slf4j
@UtilityClass
public class ChatMessageJsonSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将单条 ChatMessage 序列化为 JSON 字符串
     *
     * @param message 待序列化的消息
     * @return JSON 字符串，失败时返回 null
     */
    public static String toJson(ChatMessage message) {
        if (message == null) {
            return null;
        }
        try {
            // 使用 LangChain4j 官方序列化器
            return dev.langchain4j.data.message.ChatMessageSerializer.messageToJson(message);
        } catch (Exception e) {
            log.warn("ChatMessage 序列化失败: type={}, error={}", message.type(), e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为 ChatMessage
     *
     * @param json 待反序列化的 JSON 字符串
     * @return ChatMessage 对象，失败时返回 null
     */
    public static ChatMessage fromJson(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return dev.langchain4j.data.message.ChatMessageDeserializer.messageFromJson(json);
        } catch (Exception e) {
            log.warn("ChatMessage 反序列化失败: json={}, error={}", json, e.getMessage());
            return null;
        }
    }

    /**
     * 从 ChatMessage 中提取纯文本内容（用于兼容 content 字段的 UI 展示）
     *
     * @param message 消息对象
     * @return 文本内容，无法提取时返回空字符串
     */
    public static String extractTextContent(ChatMessage message) {
        if (message == null) {
            return "";
        }
        switch (message.type()) {
            case USER -> {
                UserMessage userMsg = (UserMessage) message;
                // 多模态消息：拼接所有文本部分
                StringBuilder sb = new StringBuilder();
                for (Content content : userMsg.contents()) {
                    if (content instanceof TextContent tc) {
                        sb.append(tc.text());
                    }
                }
                return sb.toString();
            }
            case AI -> {
                AiMessage aiMsg = (AiMessage) message;
                return aiMsg.text() != null ? aiMsg.text() : "";
            }
            case SYSTEM -> {
                SystemMessage sysMsg = (SystemMessage) message;
                return sysMsg.text();
            }
            case TOOL_EXECUTION_RESULT -> {
                ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) message;
                return toolMsg.text() != null ? toolMsg.text() : "";
            }
            default -> {
                return message.toString();
            }
        }
    }

    /**
     * 根据 role 字符串和 content 文本回退构建简单的 ChatMessage（用于兼容旧版记录）
     *
     * @param role    角色 (user/assistant/system)
     * @param content 文本内容
     * @return 对应的 ChatMessage，无法识别时返回 null
     */
    public static ChatMessage buildFromRoleAndContent(String role, String content) {
        if (StrUtil.isBlank(role) || content == null) {
            return null;
        }
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AiMessage(content);
            case "system" -> new SystemMessage(content);
            default -> {
                log.warn("无法识别的消息角色: {}", role);
                yield null;
            }
        };
    }
}
