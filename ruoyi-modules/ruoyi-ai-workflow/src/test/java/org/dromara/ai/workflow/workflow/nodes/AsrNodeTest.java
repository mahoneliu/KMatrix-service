package org.dromara.ai.workflow.workflow.nodes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.system.service.ISysOssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AsrNodeTest {

    @Mock
    private KmModelMapper modelMapper;
    @Mock
    private KmModelProviderMapper providerMapper;
    @Mock
    private ModelBuilder modelBuilder;
    @Mock
    private ObjectProvider<ISysOssService> sysOssServiceProvider;
    @Mock
    private ObjectProvider<IKmFileService> kmFileServiceProvider;
    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private AsrNode node;

    private NodeContext context;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
    }

    @Test
    void testExecute_Success() throws Exception {
        // Setup NodeConfig
        Map<String, Object> nodeConfig = Map.of("modelId", 1L);
        context.setNodeConfig(nodeConfig);

        // Input data
        KmWorkflowFile wf = new KmWorkflowFile();
        wf.setName("audio.mp3");
        wf.setType("audio");
        wf.setUrl("http://oss.com/audio.mp3");
        context.setInput("files", Collections.singletonList(wf));

        // Mock Model & Provider
        KmModel model = new KmModel();
        model.setProviderId(10L);
        when(modelMapper.selectById(1L)).thenReturn(model);

        KmModelProvider provider = new KmModelProvider();
        provider.setProviderKey("doubao");
        when(providerMapper.selectById(10L)).thenReturn(provider);

        // Mock ModelBuilder
        when(modelBuilder.buildChatModel(any(), anyString(), anyDouble(), anyInt())).thenReturn(chatModel);

        // Mock dev.langchain4j.model.chat.response.ChatResponse
        AiMessage aiMsg = dev.langchain4j.data.message.AiMessage.from("Recognized text");
        dev.langchain4j.model.chat.response.ChatResponse response = dev.langchain4j.model.chat.response.ChatResponse.builder().aiMessage(aiMsg).build();
        when(chatModel.chat(any(dev.langchain4j.model.chat.request.ChatRequest.class))).thenReturn(response);

        // Execute
        NodeOutput output = node.execute(context);

        // Verify
        assertEquals("Recognized text", output.getOutput("transcription"));
    }

    @Test
    void testExecute_MissingModelId() {
        assertThrows(RuntimeException.class, () -> node.execute(context));
    }
}
