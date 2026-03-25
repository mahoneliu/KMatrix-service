package org.dromara.ai.service.impl;

import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.mapper.KmQuestionMapper;
import org.dromara.ai.service.IKmEmbeddingService;
import org.dromara.ai.service.IKmRerankService;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KmRetrievalServiceImplTest extends BaseUnitTest {

    @Mock
    private KmDatasetMapper datasetMapper;
    @Mock
    private KmEmbeddingMapper embeddingMapper;
    @Mock
    private KmQuestionMapper questionMapper;
    @Mock
    private IKmRerankService rerankService;
    @Mock
    private KmAiProperties aiProperties;
    @Mock
    private IKmEmbeddingService embeddingService;

    private KmRetrievalServiceImpl retrievalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        retrievalService = new KmRetrievalServiceImpl(
                datasetMapper, embeddingMapper, questionMapper,
                rerankService, aiProperties, embeddingService
        );
    }

    @Test
    void search_IndependentMode_NoKbIds_ThrowsException() {
        // Arrange
        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery("test query");
        bo.setMode("VECTOR");
        bo.setKbIds(Collections.emptyList());
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);

        // Act & Assert
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            retrievalService.search(bo);
        });
        assertEquals("ai.msg.embedding.unified_mode_no_kb", exception.getMessage());
    }

    @Test
    void search_IndependentMode_MultipleKbIds_ThrowsException() {
        // Arrange
        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery("test query");
        bo.setMode("VECTOR");
        bo.setKbIds(List.of(1L, 2L));
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);

        // Act & Assert
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            retrievalService.search(bo);
        });
        assertEquals("ai.msg.embedding.unified_mode_no_kb", exception.getMessage());
    }

    @Test
    void search_IndependentMode_SingleKbId_Proceeds() {
        // Arrange
        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery("test query");
        bo.setMode("VECTOR");
        bo.setKbIds(List.of(1L));
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);
        when(embeddingService.embed(anyString(), anyLong())).thenReturn(new float[]{0.1f});
        when(embeddingMapper.vectorSearch(anyString(), anyList(), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // Act
        retrievalService.search(bo);

        // Assert
        verify(embeddingService).embed(eq("test query"), eq(1L));
        verify(embeddingMapper).vectorSearch(anyString(), eq(List.of(1L)), anyInt(), anyDouble());
    }

    @Test
    void search_UnifiedMode_ProceedsWithoutKbIds() {
        // Arrange
        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery("test query");
        bo.setMode("VECTOR");
        bo.setKbIds(Collections.emptyList());
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(true);
        when(embeddingService.embed(anyString(), isNull())).thenReturn(new float[]{0.1f});
        when(embeddingMapper.vectorSearch(anyString(), anyList(), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        // Act
        retrievalService.search(bo);

        // Assert
        verify(embeddingService).embed(eq("test query"), isNull());
    }
}
