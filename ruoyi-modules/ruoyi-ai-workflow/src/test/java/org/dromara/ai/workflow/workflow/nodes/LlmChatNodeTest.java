package org.dromara.ai.workflow.workflow.nodes;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.tool.IToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class LlmChatNodeTest {

    @Mock
    private KmModelMapper modelMapper;
    @Mock
    private KmModelProviderMapper providerMapper;
    @Mock
    private ModelBuilder modelBuilder;
    @Mock
    private IToolProvider toolProviderService;
    @Mock
    private ObjectProvider<org.dromara.ai.workflow.workflow.nodes.chat.IChatMessageProvider> chatMessageProvider;
    @Mock
    private org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowNodeUtils workflowNodeUtils;

    @InjectMocks
    private LlmChatNode llmChatNode;

    private NodeContext context;
    private KmModel model;
    private KmModelProvider provider;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
        context.setConfig("modelId", 1L);
        context.setInput("userInput", "Hello");

        model = new KmModel();
        model.setModelId(1L);
        model.setProviderId(2L);
        model.setApiBase("http://api.test");

        provider = new KmModelProvider();
        provider.setProviderId(2L);
        provider.setProviderKey("test-provider");
        provider.setDefaultEndpoint("http://default.test");

        when(modelMapper.selectById(1L)).thenReturn(model);
        when(providerMapper.selectById(2L)).thenReturn(provider);
    }

    @Test
    void testExecute_SimpleText() throws Exception {
        // Arrange
        ChatModel chatModel = mock(ChatModel.class);
        when(modelBuilder.buildChatModel(any(), any(), any(), any())).thenReturn(chatModel);

        ChatResponse aiResponse = ChatResponse.builder().aiMessage(dev.langchain4j.data.message.AiMessage.from("AI Hello")).build();
        when(chatModel.chat(any(ChatRequest.class))).thenReturn(aiResponse);

        // Act
        NodeOutput output = llmChatNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals("AI Hello", output.getOutput("dev.langchain4j.model.chat.response.ChatResponse"));
    }

    @Test
    void testExecute_MultimodalJson() throws Exception {
        // Arrange
        context.setConfig("enableMultimodal", true);
        // JSON input: [{"type":"text", "text":"Analyze this:"}, {"type":"image",
        // "url":"http://test.com/img.jpg"}]
        context.setInput("userInput",
                "[{\"type\":\"text\", \"text\":\"Analyze this:\"}, {\"type\":\"image\", \"url\":\"http://test.com/img.jpg\"}]");

        ChatModel chatModel = mock(ChatModel.class);
        when(modelBuilder.buildChatModel(any(), any(), any(), any())).thenReturn(chatModel);

        ChatResponse aiResponse = ChatResponse.builder().aiMessage(dev.langchain4j.data.message.AiMessage.from("Image analyzed")).build();
        when(chatModel.chat(any(ChatRequest.class))).thenReturn(aiResponse);

        // Act
        NodeOutput output = llmChatNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals("Image analyzed", output.getOutput("dev.langchain4j.model.chat.response.ChatResponse"));
    }
}
