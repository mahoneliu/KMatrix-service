package org.dromara.ai.knowledge.service.etl;

import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.domain.bo.ChunkResult;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class WorkflowEtlHandlerTest {

    @Mock
    private KmDocumentMapper documentMapper;

    @InjectMocks
    private WorkflowEtlHandler workflowEtlHandler;

    @Test
    void testGetProcessType() {
        assertEquals(DatasetProcessType.WORKFLOW_FILE, workflowEtlHandler.getProcessType());
    }

    @Test
    void testProcess() {
        // Arrange
        KmDocument document = new KmDocument();
        document.setId(1L);
        document.setEmbeddingStatus(2); // Initially something else
        document.setStatusMeta(new HashMap<>());

        KmDataset dataset = new KmDataset();
        dataset.setId(100L);

        // Act
        List<ChunkResult> result = workflowEtlHandler.process(document, dataset);

        // Assert
        assertNull(result, "WorkflowEtlHandler should return null to skip default ETL");
        
        // Verify updateById was called with status 0
        verify(documentMapper, times(1)).updateById(argThat((KmDocument update) -> 
            update.getId().equals(1L) && 
            update.getEmbeddingStatus() == 0
        ));
    }
}
