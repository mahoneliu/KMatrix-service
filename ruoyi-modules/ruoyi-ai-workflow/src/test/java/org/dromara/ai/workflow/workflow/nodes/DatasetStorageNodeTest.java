package org.dromara.ai.workflow.workflow.nodes;

import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.service.IKmEmbeddingService;
import org.dromara.ai.knowledge.service.IKmEtlService;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DatasetStorageNodeTest {

    @Mock
    private KmDocumentMapper documentMapper;

    @Mock
    private KmDatasetMapper datasetMapper;

    @Mock
    private IKmEtlService etlService;

    @Mock
    private IKmEmbeddingService embeddingService;

    @InjectMocks
    private DatasetStorageNode datasetStorageNode;

    private NodeContext context;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
    }

    @Test
    void testExecute_Success() throws Exception {
        // Arrange
        Long documentId = 1L;
        String text = "This is a test text for storage node.";
        context.setInput("documentId", documentId);
        context.setInput("text", text);
        context.setConfig("chunkSize", 300);
        context.setConfig("overlap", 30);

        KmDocument document = new KmDocument();
        document.setId(documentId);
        document.setKbId(100L);
        document.setDatasetId(200L);
        document.setStatusMeta(new HashMap<>());

        when(documentMapper.selectById(documentId)).thenReturn(document);

        KmDataset dataset = new KmDataset();
        dataset.setId(200L);
        when(datasetMapper.selectById(200L)).thenReturn(dataset);

        List<String> chunks = Arrays.asList("chunk1", "chunk2");
        when(etlService.splitText(text, 300, 30)).thenReturn(chunks);

        // Act
        NodeOutput output = datasetStorageNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals(2, output.getOutput("chunkCount"));

        // Verify embeddingService is called
        verify(embeddingService, times(1)).embedAndStoreChunks(eq(documentId), eq(100L), anyList());
        
        // Verify document status updated to 2
        ArgumentCaptor<KmDocument> captor = ArgumentCaptor.forClass(KmDocument.class);
        verify(documentMapper, times(1)).updateById((KmDocument) captor.capture());
        KmDocument updatedDoc = captor.getValue();
        assertEquals(1L, updatedDoc.getId());
        assertEquals(2, updatedDoc.getEmbeddingStatus());
    }

    @Test
    void testExecute_MissingInputText() {
        // Arrange
        context.setInput("documentId", 1L);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            datasetStorageNode.execute(context);
        });
        assertTrue(exception.getMessage().contains("requires input: text"));
    }

    @Test
    void testExecute_EmptyChunks() throws Exception {
        // Arrange
        Long documentId = 1L;
        String text = "empty";
        context.setInput("documentId", documentId);
        context.setInput("text", text);

        KmDocument document = new KmDocument();
        document.setId(documentId);
        document.setDatasetId(200L);
        document.setStatusMeta(new HashMap<>());

        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(datasetMapper.selectById(200L)).thenReturn(new KmDataset());

        when(etlService.splitText(eq(text), anyInt(), anyInt())).thenReturn(null);

        // Act
        NodeOutput output = datasetStorageNode.execute(context);

        // Assert
        assertNotNull(output);
        assertEquals(0, output.getOutput("chunkCount"));

        // Verify embeddingService is not called
        verify(embeddingService, never()).embedAndStoreChunks(any(), any(), anyList());
    }
}
