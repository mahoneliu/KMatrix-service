package org.dromara.ai.workflow.workflow.nodes;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class IntentClassifierNodeTest {

    @Mock
    private KmModelMapper modelMapper;
    @Mock
    private KmModelProviderMapper providerMapper;
    @Mock
    private ModelBuilder modelBuilder;

    @InjectMocks
    private IntentClassifierNode intentClassifierNode;

    private NodeContext context;
    private KmModel model;
    private KmModelProvider provider;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
        context.setConfig("modelId", 1L);
        context.setInput("instruction", "Help me with the weather");

        List<Map<String, String>> intents = new ArrayList<>();
        Map<String, String> intent1 = new HashMap<>();
        intent1.put("name", "weather");
        intent1.put("description", "Query weather information");
        intents.add(intent1);

        Map<String, String> intent2 = new HashMap<>();
        intent2.put("name", "knowledge");
        intent2.put("description", "Query knowledge base");
        intents.add(intent2);

        context.setConfig("intents", intents);

        model = new KmModel();
        model.setModelId(1L);
        model.setProviderId(2L);

        provider = new KmModelProvider();
        provider.setProviderId(2L);
        provider.setProviderKey("test-provider");

        when(modelMapper.selectById(1L)).thenReturn(model);
        when(providerMapper.selectById(2L)).thenReturn(provider);
    }

    @Test
    void testExecute_SuccessfulClassification() throws Exception {
        // Arrange
        ChatModel chatModel = mock(ChatModel.class);
        when(modelBuilder.buildChatModel(any(), any())).thenReturn(chatModel);

        ChatResponse aiResponse = ChatResponse.builder().aiMessage(AiMessage.from("weather")).build();
        when(chatModel.chat(any(List.class))).thenReturn(aiResponse);

        // Act
        NodeOutput output = intentClassifierNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals("weather", output.getOutput("intent"));
        assertEquals("intent-0", output.getOutput("routeKey"));
    }

    @Test
    void testExecute_DefaultElse() throws Exception {
        // Arrange
        ChatModel chatModel = mock(ChatModel.class);
        when(modelBuilder.buildChatModel(any(), any())).thenReturn(chatModel);

        ChatResponse aiResponse = ChatResponse.builder().aiMessage(AiMessage.from("Something random")).build();
        when(chatModel.chat(any(List.class))).thenReturn(aiResponse);

        // Act
        NodeOutput output = intentClassifierNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals("else", output.getOutput("intent"));
        assertEquals("else", output.getOutput("routeKey"));
    }
}
