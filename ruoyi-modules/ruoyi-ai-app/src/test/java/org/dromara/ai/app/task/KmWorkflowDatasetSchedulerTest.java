package org.dromara.ai.app.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.service.etl.DatasetProcessType;
import org.dromara.ai.workflow.workflow.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KmWorkflowDatasetSchedulerTest {

    @Mock
    private KmDatasetMapper datasetMapper;
    @Mock
    private KmDocumentMapper documentMapper;
    @Mock
    private KmAppMapper appMapper;
    @Mock
    private WorkflowExecutor workflowExecutor;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KmWorkflowDatasetScheduler scheduler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSchedule_NoDatasets() {
        when(datasetMapper.selectList(any())).thenReturn(Arrays.asList());
        scheduler.schedule();
        verify(documentMapper, never()).selectCount(any());
    }

    @Test
    void testSchedule_DatasetWithNoConfig() {
        KmDataset dataset = new KmDataset();
        dataset.setId(100L);
        dataset.setProcessType(DatasetProcessType.WORKFLOW_FILE);
        when(datasetMapper.selectList(any())).thenReturn(Arrays.asList(dataset));

        scheduler.schedule();
        verify(documentMapper, never()).selectCount(any());
    }

    @Test
    void testSchedule_FullConcurrency() {
        KmDataset dataset = new KmDataset();
        dataset.setId(100L);
        dataset.setProcessType(DatasetProcessType.WORKFLOW_FILE);
        Map<String, Object> config = new HashMap<>();
        config.put("appId", 1L);
        config.put("maxConcurrency", 2);
        dataset.setConfig(config);

        when(datasetMapper.selectList(any())).thenReturn(Arrays.asList(dataset));
        when(documentMapper.selectCount(any())).thenReturn(2L);

        scheduler.schedule();
        verify(documentMapper, never()).selectList(any());
    }

    @Test
    void testSchedule_TriggerWorkflow() throws Exception {
        // Arrange
        KmDataset dataset = new KmDataset();
        dataset.setId(100L);
        dataset.setProcessType(DatasetProcessType.WORKFLOW_FILE);
        Map<String, Object> config = new HashMap<>();
        config.put("appId", 1L);
        config.put("maxConcurrency", 2);
        dataset.setConfig(config);

        when(datasetMapper.selectList(any())).thenReturn(Arrays.asList(dataset));
        when(documentMapper.selectCount(any())).thenReturn(1L); // 1 processing, 1 slot left

        KmDocument pendingDoc = new KmDocument();
        pendingDoc.setId(500L);
        pendingDoc.setEmbeddingStatus(0);
        pendingDoc.setStatusMeta(new HashMap<>());
        when(documentMapper.selectList(any())).thenReturn(Arrays.asList(pendingDoc));

        KmApp app = new KmApp();
        app.setAppId(1L);
        app.setDslData("{}");
        when(appMapper.selectById(1L)).thenReturn(app);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        // Mock the CAS update success
        when(documentMapper.update(any(), any())).thenReturn(1);

        // Act
        scheduler.schedule();

        // Assert
        // We wait a bit because triggerWorkflowAsync starts a thread
        Thread.sleep(1000);
        verify(workflowExecutor, times(1)).executeWorkflow(any(), any());
    }
}
