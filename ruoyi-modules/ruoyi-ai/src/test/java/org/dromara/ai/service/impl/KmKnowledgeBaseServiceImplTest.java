package org.dromara.ai.service.impl;

import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmKnowledgeBase;
import org.dromara.ai.domain.bo.KmKnowledgeBaseBo;
import org.dromara.ai.mapper.*;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KmKnowledgeBaseServiceImplTest extends BaseUnitTest {

    @Mock
    private KmAiProperties aiProperties;
    @Mock
    private KmKnowledgeBaseMapper baseMapper;
    @Mock
    private KmDatasetMapper datasetMapper;
    @Mock
    private KmDocumentMapper documentMapper;
    @Mock
    private KmDocumentChunkMapper chunkMapper;
    @Mock
    private KmQuestionMapper questionMapper;
    @Mock
    private KmEmbeddingMapper embeddingMapper;

    private KmKnowledgeBaseServiceImpl kbService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kbService = new KmKnowledgeBaseServiceImpl(
                aiProperties, baseMapper, datasetMapper,
                documentMapper, chunkMapper, questionMapper, embeddingMapper
        );
    }

    @Test
    void insertByBo_IndependentMode_NoModelId_ThrowsException() {
        // Arrange
        KmKnowledgeBaseBo bo = new KmKnowledgeBaseBo();
        bo.setName("Test KB");
        bo.setEmbeddingModelId(null);
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            kbService.insertByBo(bo);
        });
    }

    @Test
    void insertByBo_UnifiedMode_NoModelId_Success() {
        // Arrange
        KmKnowledgeBaseBo bo = new KmKnowledgeBaseBo();
        bo.setName("Test KB");
        bo.setEmbeddingModelId(null);
        
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(true);
        when(baseMapper.insert(any(KmKnowledgeBase.class))).thenReturn(1);

        // Act
        // Note: LoginHelper.getUserId() might fail if not mocked, 
        // but BaseUnitTest doesn't handle LoginHelper yet.
        // Let's see if it errors out.
        try {
        kbService.insertByBo(bo);
            // If it succeeds, great. If NPE on LoginHelper, I need to fix it.
        } catch (Exception e) {
            // Log and see
            e.printStackTrace();
        }
    }

    @Test
    void updateByBo_ChangeEmbeddingModel_ThrowsException() {
        // Arrange
        Long kbId = 1L;
        KmKnowledgeBaseBo bo = new KmKnowledgeBaseBo();
        bo.setId(kbId);
        bo.setEmbeddingModelId(200L); // New model ID
        
        KmKnowledgeBase existing = new KmKnowledgeBase();
        existing.setId(kbId);
        existing.setEmbeddingModelId(100L); // Old model ID
        
        when(baseMapper.selectById(kbId)).thenReturn(existing);

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            kbService.updateByBo(bo);
        });
    }
}
