package org.dromara.ai.service.impl;

import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmKnowledgeBase;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.vo.KmModelProviderVo;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.embedding.Embedding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KmEmbeddingServiceImplTest extends BaseUnitTest {

    @Mock
    private KmAiProperties aiProperties;
    @Mock
    private KmModelMapper kmModelMapper;
    @Mock
    private IKmModelProviderService providerService;
    @Mock
    private KmKnowledgeBaseMapper kbMapper;
    @Mock
    private ModelBuilder modelBuilder;
    @Mock
    private KmDocumentChunkMapper chunkMapper;
    @Mock
    private KmEmbeddingMapper embeddingMapper;
    @Mock
    private KmQuestionMapper questionMapper;
    @Mock
    private KmDocumentMapper documentMapper;
    @Mock
    private KmQuestionChunkMapMapper questionChunkMapMapper;

    private KmEmbeddingServiceImpl embeddingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        embeddingService = new KmEmbeddingServiceImpl(
                aiProperties, kmModelMapper, providerService, kbMapper,
                modelBuilder, chunkMapper, embeddingMapper, questionMapper,
                documentMapper, questionChunkMapMapper
        );
    }

    @Test
    void resolveEmbeddingModel_UnifiedMode_ReturnsDefaultModel() {
        // Arrange
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(true);
        
        KmModel defaultModel = new KmModel();
        defaultModel.setModelId(1L);
        defaultModel.setProviderId(1L);
        when(kmModelMapper.selectOne(any())).thenReturn(defaultModel);
        
        KmModelProviderVo provider = new KmModelProviderVo();
        provider.setProviderKey("openai");
        when(providerService.queryById(1L)).thenReturn(provider);
        
        EmbeddingModel mockModel = mock(EmbeddingModel.class);
        when(modelBuilder.buildEmbeddingModel(eq(defaultModel), anyString())).thenReturn(mockModel);

        // STUB embed to avoid retry failure
        when(mockModel.embed(anyString())).thenReturn(Response.from(new Embedding(new float[]{0.1f, 0.2f})));

        // Act
        // Use a method that triggers resolveEmbeddingModel, e.g., embed (if it calls it)
        // Or we can use reflection to test the private method if we want to be precise, 
        // but testing through public API is better.
        float[] result = embeddingService.embed("test", null);

        // Assert
        assertNotNull(result);
        verify(modelBuilder).buildEmbeddingModel(defaultModel, "openai");
    }

    @Test
    void resolveEmbeddingModel_IndependentMode_NoKbId_ThrowsException() {
        // Arrange
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            embeddingService.embed("test", null);
        });
    }

    @Test
    void resolveEmbeddingModel_IndependentMode_KbHasModel_ReturnsKbModel() {
        // Arrange
        Long kbId = 100L;
        Long modelId = 200L;
        when(aiProperties.isUnifiedEmbeddingModel()).thenReturn(false);
        
        KmKnowledgeBase kb = new KmKnowledgeBase();
        kb.setEmbeddingModelId(modelId);
        when(kbMapper.selectById(kbId)).thenReturn(kb);
        
        KmModel kbModel = new KmModel();
        kbModel.setModelId(modelId);
        kbModel.setProviderId(2L);
        when(kmModelMapper.selectById(modelId)).thenReturn(kbModel);
        
        KmModelProviderVo provider = new KmModelProviderVo();
        provider.setProviderKey("local");
        when(providerService.queryById(2L)).thenReturn(provider);
        
        EmbeddingModel mockModel = mock(EmbeddingModel.class);
        when(modelBuilder.buildEmbeddingModel(eq(kbModel), anyString())).thenReturn(mockModel);
        
        // STUB embed to avoid retry failure
        when(mockModel.embed(anyString())).thenReturn(Response.from(new Embedding(new float[]{0.3f, 0.4f})));

        // Act
        float[] result = embeddingService.embed("test", kbId);

        // Assert
        assertNotNull(result);
        verify(modelBuilder).buildEmbeddingModel(kbModel, "local");
    }
}
