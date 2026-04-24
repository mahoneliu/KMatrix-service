package org.dromara.ai.app.util;

import dev.langchain4j.data.message.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatMessageJsonSerializer 单元测试
 * 验证各类消息类型的序列化/反序列化正确性
 */
@DisplayName("ChatMessageJsonSerializer 单元测试")
class ChatMessageJsonSerializerTest {

    @Test
    @DisplayName("UserMessage 纯文本：序列化后可完整还原")
    void testUserMessageRoundTrip() {
        UserMessage original = new UserMessage("你好，请帮我介绍一下 KMatrix");
        String json = ChatMessageJsonSerializer.toJson(original);

        assertThat(json).isNotBlank();

        ChatMessage restored = ChatMessageJsonSerializer.fromJson(json);
        assertThat(restored).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) restored).singleText()).isEqualTo("你好，请帮我介绍一下 KMatrix");
    }

    @Test
    @DisplayName("AiMessage 纯文本：序列化后可完整还原")
    void testAiMessageRoundTrip() {
        AiMessage original = new AiMessage("KMatrix 是一个基于本地知识库的 AI 工作流平台");
        String json = ChatMessageJsonSerializer.toJson(original);

        assertThat(json).isNotBlank();

        ChatMessage restored = ChatMessageJsonSerializer.fromJson(json);
        assertThat(restored).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) restored).text()).isEqualTo("KMatrix 是一个基于本地知识库的 AI 工作流平台");
    }

    @Test
    @DisplayName("SystemMessage：序列化后可完整还原")
    void testSystemMessageRoundTrip() {
        SystemMessage original = new SystemMessage("你是一个专业的 AI 助手");
        String json = ChatMessageJsonSerializer.toJson(original);

        assertThat(json).isNotBlank();

        ChatMessage restored = ChatMessageJsonSerializer.fromJson(json);
        assertThat(restored).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) restored).text()).isEqualTo("你是一个专业的 AI 助手");
    }

    @Test
    @DisplayName("AiMessage 含 ToolExecutionRequest：工具调用信息不丢失")
    void testAiMessageWithToolExecutionRequest() {
        AiMessage original = AiMessage.from(
                "调用工具",
                List.of(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("call_001")
                        .name("web_search")
                        .arguments("{\"query\": \"KMatrix AI\"}")
                        .build())
        );
        String json = ChatMessageJsonSerializer.toJson(original);

        assertThat(json).isNotBlank();

        ChatMessage restored = ChatMessageJsonSerializer.fromJson(json);
        assertThat(restored).isInstanceOf(AiMessage.class);
        AiMessage restoredAi = (AiMessage) restored;
        assertThat(restoredAi.toolExecutionRequests()).hasSize(1);
        assertThat(restoredAi.toolExecutionRequests().get(0).name()).isEqualTo("web_search");
    }

    @Test
    @DisplayName("toJson 传入 null：返回 null，不抛出异常")
    void testToJsonWithNull() {
        String result = ChatMessageJsonSerializer.toJson(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fromJson 传入 null 或空字符串：返回 null，不抛出异常")
    void testFromJsonWithNullOrBlank() {
        assertThat(ChatMessageJsonSerializer.fromJson(null)).isNull();
        assertThat(ChatMessageJsonSerializer.fromJson("")).isNull();
        assertThat(ChatMessageJsonSerializer.fromJson("  ")).isNull();
    }

    @Test
    @DisplayName("buildFromRoleAndContent：正确识别 user/assistant/system 角色")
    void testBuildFromRoleAndContent() {
        ChatMessage userMsg = ChatMessageJsonSerializer.buildFromRoleAndContent("user", "用户消息");
        assertThat(userMsg).isInstanceOf(UserMessage.class);

        ChatMessage aiMsg = ChatMessageJsonSerializer.buildFromRoleAndContent("assistant", "AI 回复");
        assertThat(aiMsg).isInstanceOf(AiMessage.class);

        ChatMessage sysMsg = ChatMessageJsonSerializer.buildFromRoleAndContent("system", "系统提示");
        assertThat(sysMsg).isInstanceOf(SystemMessage.class);
    }

    @Test
    @DisplayName("buildFromRoleAndContent：未知角色返回 null")
    void testBuildFromRoleAndContentUnknownRole() {
        ChatMessage result = ChatMessageJsonSerializer.buildFromRoleAndContent("unknown_role", "content");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("extractTextContent：正确提取各类消息的文本")
    void testExtractTextContent() {
        assertThat(ChatMessageJsonSerializer.extractTextContent(new UserMessage("用户输入")))
                .isEqualTo("用户输入");
        assertThat(ChatMessageJsonSerializer.extractTextContent(new AiMessage("AI 输出")))
                .isEqualTo("AI 输出");
        assertThat(ChatMessageJsonSerializer.extractTextContent(new SystemMessage("系统提示")))
                .isEqualTo("系统提示");
        assertThat(ChatMessageJsonSerializer.extractTextContent(null))
                .isEqualTo("");
    }
}
