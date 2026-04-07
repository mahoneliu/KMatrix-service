package org.dromara.ai.workflow.workflow.nodes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
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
class OcrNodeTest {

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
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private OcrNode node;

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
        wf.setName("image.jpg");
        wf.setType("image");
        wf.setUrl("http://oss.com/image.jpg");
        context.setInput("files", Collections.singletonList(wf));

        // Mock Model & Provider
        KmModel model = new KmModel();
        model.setProviderId(10L);
        when(modelMapper.selectById(1L)).thenReturn(model);

        KmModelProvider provider = new KmModelProvider();
        provider.setProviderKey("doubao");
        when(providerMapper.selectById(10L)).thenReturn(provider);

        // Mock ModelBuilder
        when(modelBuilder.buildChatModel(any(), anyString(), anyDouble(), anyInt())).thenReturn(chatLanguageModel);

        // Mock response
        AiMessage aiMsg = AiMessage.from("Extracted text");
        Response<AiMessage> response = Response.from(aiMsg);
        when(chatLanguageModel.generate(anyList())).thenReturn(response);

        // Execute
        NodeOutput output = node.execute(context);

        // Verify
        assertEquals("Extracted text", output.getOutput("text"));
    }
}
