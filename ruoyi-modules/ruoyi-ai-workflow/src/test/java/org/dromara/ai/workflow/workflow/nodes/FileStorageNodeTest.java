package org.dromara.ai.workflow.workflow.nodes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.storage.mapper.KmTempFileMapper;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.dromara.system.service.ISysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStorageNodeTest {

    @Mock
    private KmDocumentMapper documentMapper;
    @Mock
    private KmDatasetMapper datasetMapper;
    @Mock
    private KmTempFileMapper tempFileMapper;
    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private FileStorageNode node;

    private NodeContext context;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
    }

    @Test
    void testExecute_SuccessWithTempFile() throws Exception {
        // Prepare input
        KmWorkflowFile wf = new KmWorkflowFile();
        wf.setName("test.jpg");
        wf.setExtension("jpg");
        wf.setSize(1024L);
        wf.setTempFileId(100L);
        context.setInput("file", wf);
        context.setInput("datasetId", 1L);

        // Mock dependencies
        KmDataset dataset = new KmDataset();
        dataset.setKbId(200L);
        when(datasetMapper.selectById(1L)).thenReturn(dataset);

        KmTempFile tempFile = new KmTempFile();
        tempFile.setFilePath("temp/test.jpg");
        tempFile.setStoreType(1);
        tempFile.setOssId(300L);
        when(tempFileMapper.selectById(100L)).thenReturn(tempFile);

        // Execute
        NodeOutput output = node.execute(context);

        // Verify
        assertNotNull(output.getOutput(WorkflowState.KEY_DOCUMENT_ID));
        verify(documentMapper, times(1)).insert(any(KmDocument.class));
    }

    @Test
    void testExecute_MissingInput() {
        assertThrows(IllegalArgumentException.class, () -> node.execute(context));
    }
    
    @Test
    void testExecute_DatasetNotFound() {
        KmWorkflowFile wf = new KmWorkflowFile();
        context.setInput("file", wf);
        context.setInput("datasetId", 999L);
        when(datasetMapper.selectById(999L)).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> node.execute(context));
    }
}
