package org.dromara.ai.util;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ModelBuilder 单元测试
 * 覆盖 buildEmbeddingModel 的缓存机制和各 provider 的构建逻辑
 */
@DisplayName("ModelBuilder 单元测试")
class ModelBuilderTest extends BaseUnitTest {

    @Mock
    private KmAiProperties aiProperties;

    @Mock
    private IKmModelProviderService kmModelServiceImpl;

    private ModelBuilder modelBuilder;

    private Map<Long, EmbeddingModel> embeddingModelCache;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        modelBuilder = new ModelBuilder(aiProperties, kmModelServiceImpl);

        // 通过反射获取内部缓存 Map，以便验证缓存行为
        Field cacheField = ModelBuilder.class.getDeclaredField("embeddingModelCache");
        cacheField.setAccessible(true);
        //noinspection unchecked
        embeddingModelCache = (Map<Long, EmbeddingModel>) cacheField.get(modelBuilder);
    }

    // ==================== 参数校验测试 ====================

    @Test
    @DisplayName("buildEmbeddingModel - 入参为null时应抛出异常")
    void buildEmbeddingModel_NullModel_ThrowsException() {
        assertThrows(ServiceException.class, () ->
            modelBuilder.buildEmbeddingModel(null, "openai"));
    }

    @Test
    @DisplayName("buildEmbeddingModel - providerKey为空时应抛出异常")
    void buildEmbeddingModel_BlankProviderKey_ThrowsException() {
        KmModel model = buildModel(1L, "text-embedding-ada-002", "sk-test", "https://api.openai.com/v1");
        assertThrows(ServiceException.class, () ->
            modelBuilder.buildEmbeddingModel(model, ""));
    }

    @Test
    @DisplayName("buildEmbeddingModel - 不支持的 providerKey 应抛出异常")
    void buildEmbeddingModel_UnsupportedProvider_ThrowsException() {
        KmModel model = buildModel(99L, "unknown-model", "sk-test", "https://api.unknown.com");
        assertThrows(ServiceException.class, () ->
            modelBuilder.buildEmbeddingModel(model, "unsupported_provider_xyz"));
    }

    // ==================== 缓存行为测试 ====================

    @Test
    @DisplayName("buildEmbeddingModel - 第一次调用应构建并缓存模型")
    void buildEmbeddingModel_FirstCall_BuildsAndCachesModel() {
        Long modelId = 10L;
        KmModel model = buildModel(modelId, "text-embedding-ada-002", "sk-test-key",
            "https://api.openai.com/v1");

        assertFalse(embeddingModelCache.containsKey(modelId), "构建前缓存应为空");

        EmbeddingModel result = modelBuilder.buildEmbeddingModel(model, "openai");

        assertNotNull(result, "构建结果不应为null");
        assertTrue(embeddingModelCache.containsKey(modelId), "构建后缓存应包含该模型ID");
        assertSame(embeddingModelCache.get(modelId), result, "缓存的对象应与返回的对象相同");
    }

    @Test
    @DisplayName("buildEmbeddingModel - 相同 modelId 的第二次调用应直接返回缓存实例")
    void buildEmbeddingModel_SecondCallSameId_ReturnsCachedInstance() {
        Long modelId = 20L;
        EmbeddingModel mockCachedModel = mock(EmbeddingModel.class);
        embeddingModelCache.put(modelId, mockCachedModel);

        KmModel model = buildModel(modelId, "text-embedding-ada-002", "sk-any", "https://any.api");

        EmbeddingModel result = modelBuilder.buildEmbeddingModel(model, "openai");

        assertSame(mockCachedModel, result, "应直接返回缓存的模型实例，而非重新构建");
    }

    @Test
    @DisplayName("buildEmbeddingModel - modelId 为 null 时不应缓存")
    void buildEmbeddingModel_NullModelId_DoesNotCache() {
        KmModel model = buildModel(null, "text-embedding-ada-002", "sk-test",
            "https://api.openai.com/v1");

        EmbeddingModel result = modelBuilder.buildEmbeddingModel(model, "openai");

        assertNotNull(result);
        assertTrue(embeddingModelCache.isEmpty(), "modelId 为 null 时不应写入缓存");
    }

    // ==================== Provider 构建测试 ====================

    @Test
    @DisplayName("buildEmbeddingModel - openai provider 应成功构建")
    void buildEmbeddingModel_OpenAiProvider_BuildsSuccessfully() {
        KmModel model = buildModel(30L, "text-embedding-3-small", "sk-openai",
            "https://api.openai.com/v1");
        assertDoesNotThrow(() -> modelBuilder.buildEmbeddingModel(model, "openai"));
    }

    @Test
    @DisplayName("buildEmbeddingModel - siliconflow provider 应成功构建")
    void buildEmbeddingModel_SiliconFlowProvider_BuildsSuccessfully() {
        KmModel model = buildModel(31L, "BAAI/bge-m3", "sk-silicon",
            "https://api.siliconflow.cn/v1");
        assertDoesNotThrow(() -> modelBuilder.buildEmbeddingModel(model, "siliconflow"));
    }

    @Test
    @DisplayName("buildEmbeddingModel - ollama provider 应成功构建")
    void buildEmbeddingModel_OllamaProvider_BuildsSuccessfully() {
        KmModel model = buildModel(32L, "nomic-embed-text", null, "http://localhost:11434");
        assertDoesNotThrow(() -> modelBuilder.buildEmbeddingModel(model, "ollama"));
    }

    @Test
    @DisplayName("buildEmbeddingModel - local provider 应成功构建内置 ONNX 模型")
    void buildEmbeddingModel_LocalProvider_BuildsBgeSmallZh() {
        KmModel model = buildModel(33L, "bge-small-zh", null, null);
        EmbeddingModel result = modelBuilder.buildEmbeddingModel(model, "local");
        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private KmModel buildModel(Long id, String modelKey, String apiKey, String apiBase) {
        KmModel model = new KmModel();
        model.setModelId(id);
        model.setModelKey(modelKey);
        model.setApiKey(apiKey);
        model.setApiBase(apiBase);
        model.setProviderId(1L);
        return model;
    }
}
