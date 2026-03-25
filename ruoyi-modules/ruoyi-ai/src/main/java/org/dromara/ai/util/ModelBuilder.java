package org.dromara.ai.util;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GeminiHarmCategory;
import dev.langchain4j.model.googleai.GeminiHarmBlockThreshold;
import dev.langchain4j.model.googleai.GeminiSafetySetting;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.vo.KmModelProviderVo;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI模型构建器工具类
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ModelBuilder {

    private final KmAiProperties aiProperties;

    private final IKmModelProviderService kmModelServiceImpl;

    private final Map<Long, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();
    private final Map<Long, ScoringModel> scoringModelCache = new ConcurrentHashMap<>();

    // 将默认超时时间从60秒增加到300秒，以适应DeepSeek等带有长推理过程的模型
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);

    /**
     * 构建聊天模型
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @return 聊天模型实例
     */
    public ChatLanguageModel buildChatModel(KmModel model, String providerKey) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.config_empty"));
        }

        log.info("构建聊天模型: providerKey={}, modelKey={}", providerKey, model.getModelKey());

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot", "doubao", "siliconflow" -> buildOpenAiModel(model);
            case "ollama", "vllm" -> buildOllamaModel(model);
            case "bailian", "zhipu", "qwen" -> buildQwenModel(model);
            case "gemini" -> buildGeminiModel(model);
            default -> throw new ServiceException(MessageUtils.message("ai.msg.model.unsupported_provider", providerKey));
        };
    }

    /**
     * 构建聊天模型(带参数)
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @param temperature 温度参数 (0.0-2.0)
     * @param maxTokens   最大token数
     * @return 聊天模型实例
     */
    public ChatLanguageModel buildChatModel(KmModel model, String providerKey, Double temperature, Integer maxTokens) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.config_empty"));
        }

        log.info("构建聊天模型(带参数): providerKey={}, modelKey={}, temperature={}, maxTokens={}",
                providerKey, model.getModelKey(), temperature, maxTokens);

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot", "doubao", "siliconflow" -> buildOpenAiModel(model, temperature, maxTokens);
            case "ollama", "vllm" -> buildOllamaModel(model, temperature, maxTokens);
            case "bailian", "zhipu", "qwen" -> buildQwenModel(model, temperature, maxTokens);
            case "gemini" -> buildGeminiModel(model, temperature, maxTokens);
            default -> throw new ServiceException(MessageUtils.message("ai.msg.model.unsupported_provider", providerKey));
        };
    }

    /**
     * 构建流式聊天模型
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @return 流式聊天模型实例
     */
    public StreamingChatLanguageModel buildStreamingChatModel(KmModel model, String providerKey) {
        return buildStreamingChatModel(model, providerKey, null, null);
    }

    /**
     * 构建流式聊天模型（带参数）
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @param temperature 温度参数 (0.0-2.0)
     * @param maxTokens   最大token数
     * @return 流式聊天模型实例
     */
    public StreamingChatLanguageModel buildStreamingChatModel(KmModel model, String providerKey,
            Double temperature, Integer maxTokens) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.config_empty"));
        }

        log.info("构建流式聊天模型: providerKey={}, modelKey={}, temperature={}, maxTokens={}",
                providerKey, model.getModelKey(), temperature, maxTokens);

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot", "doubao", "siliconflow" -> buildOpenAiStreamingModel(model, temperature, maxTokens);
            case "ollama", "vllm" -> buildOllamaStreamingModel(model, temperature, maxTokens);
            case "bailian", "zhipu", "qwen" -> buildQwenStreamingModel(model, temperature, maxTokens);
            case "gemini" -> buildGeminiStreamingModel(model, temperature, maxTokens);
            default -> throw new ServiceException(MessageUtils.message("ai.msg.model.unsupported_provider", providerKey));
        };
    }

    /**
     * 构建OpenAI类型模型
     */
    private ChatLanguageModel buildOpenAiModel(KmModel model) {
        var builder = OpenAiChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(model.getApiBase())) {
            builder.baseUrl(model.getApiBase());
        } else {
            // 获取provider的默认apiBase
            KmModelProviderVo providerVo = kmModelServiceImpl.queryById(model.getProviderId());
            builder.baseUrl(providerVo.getDefaultEndpoint());
        }

        return builder.build();
    }

    /**
     * 构建OpenAI类型模型(带参数)
     */
    private ChatLanguageModel buildOpenAiModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = OpenAiChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(model.getApiBase())) {
            builder.baseUrl(model.getApiBase());
        } else {
            // 获取provider的默认apiBase
            KmModelProviderVo providerVo = kmModelServiceImpl.queryById(model.getProviderId());
            builder.baseUrl(providerVo.getDefaultEndpoint());
        }
        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建OpenAI类型流式模型
     */
    private StreamingChatLanguageModel buildOpenAiStreamingModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(model.getApiBase())) {
            builder.baseUrl(model.getApiBase());
        } else {
            // 获取provider的默认apiBase
            KmModelProviderVo providerVo = kmModelServiceImpl.queryById(model.getProviderId());
            builder.baseUrl(providerVo.getDefaultEndpoint());
        }
        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建Ollama类型模型
     */
    private ChatLanguageModel buildOllamaModel(KmModel model) {
        String baseUrl = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : "http://localhost:11434";

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT)
                .build();
    }

    /**
     * 构建Ollama类型模型(带参数)
     */
    private ChatLanguageModel buildOllamaModel(KmModel model, Double temperature, Integer maxTokens) {
        String baseUrl = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : "http://localhost:11434";

        var builder = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.numPredict(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建Ollama类型流式模型
     */
    private StreamingChatLanguageModel buildOllamaStreamingModel(KmModel model, Double temperature, Integer maxTokens) {
        String baseUrl = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : "http://localhost:11434";

        var builder = OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.numPredict(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建通义千问类型模型
     */
    private ChatLanguageModel buildQwenModel(KmModel model) {
        return QwenChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .build();
    }

    /**
     * 构建通义千问类型模型(带参数)
     */
    private ChatLanguageModel buildQwenModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = QwenChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey());

        if (temperature != null) {
            builder.temperature(temperature.floatValue());
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建通义千问类型流式模型
     */
    private StreamingChatLanguageModel buildQwenStreamingModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = QwenStreamingChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey());

        if (temperature != null) {
            builder.temperature(temperature.floatValue());
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建Gemini类型模型
     */
    private ChatLanguageModel buildGeminiModel(KmModel model) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .safetySettings(Collections.singletonMap(
                        GeminiHarmCategory.HARM_CATEGORY_HATE_SPEECH, GeminiHarmBlockThreshold.BLOCK_NONE))
                .logRequestsAndResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT)
                .build();
    }

    /**
     * 构建Gemini类型模型(带参数)
     */
    private ChatLanguageModel buildGeminiModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = GoogleAiGeminiChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .safetySettings(Collections.singletonMap(
                        GeminiHarmCategory.HARM_CATEGORY_HATE_SPEECH, GeminiHarmBlockThreshold.BLOCK_NONE))
                .logRequestsAndResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxOutputTokens(maxTokens);
        }

        return builder.build();
    }

    /**
     * 构建Gemini类型流式模型
     */
    private StreamingChatLanguageModel buildGeminiStreamingModel(KmModel model, Double temperature, Integer maxTokens) {
        var builder = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .safetySettings(Collections.singletonList(
                        new GeminiSafetySetting(GeminiHarmCategory.HARM_CATEGORY_HATE_SPEECH,
                                GeminiHarmBlockThreshold.BLOCK_NONE)))
                .logRequestsAndResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxOutputTokens(maxTokens);
        }

        return builder.build();
    }

    public EmbeddingModel buildEmbeddingModel(KmModel model, String providerKey) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.config_empty"));
        }

        // 使用缓存，避免重复构建
        if (model.getModelId() != null && embeddingModelCache.containsKey(model.getModelId())) {
            return embeddingModelCache.get(model.getModelId());
        }

        log.info("构建向量化模型: providerKey={}, modelKey={}", providerKey, model.getModelKey());

        EmbeddingModel embeddingModel = switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot", "doubao", "vllm", "zhipu", "siliconflow" -> buildOpenAiEmbeddingModel(model);
            case "ollama" -> buildOllamaEmbeddingModel(model);
            case "qwen", "bailian" -> buildQwenEmbeddingModel(model);
            case "local" -> buildLocalEmbeddingModel(model);
            default -> throw new ServiceException(MessageUtils.message("ai.msg.model.unsupported_provider", providerKey));
        };

        if (model.getModelId() != null) {
            embeddingModelCache.put(model.getModelId(), embeddingModel);
        }

        return embeddingModel;
    }

    private EmbeddingModel buildLocalEmbeddingModel(KmModel model) {
        if ("bge-small-zh".equalsIgnoreCase(model.getModelKey())) {
            return new BgeSmallZhEmbeddingModel();
        }
        throw new ServiceException(MessageUtils.message("ai.msg.embedding.unsupported_local_model", model.getModelKey()));
    }

    private EmbeddingModel buildOpenAiEmbeddingModel(KmModel model) {
        var builder = OpenAiEmbeddingModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(model.getApiBase())) {
            builder.baseUrl(model.getApiBase());
        } else {
            KmModelProviderVo providerVo = kmModelServiceImpl.queryById(model.getProviderId());
            builder.baseUrl(providerVo.getDefaultEndpoint());
        }
        return builder.build();
    }

    private EmbeddingModel buildOllamaEmbeddingModel(KmModel model) {
        String baseUrl = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : "http://localhost:11434";

        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT)
                .build();
    }

    private EmbeddingModel buildQwenEmbeddingModel(KmModel model) {
        return QwenEmbeddingModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .build();
    }

    /**
     * 构建重排序模型 (ScoringModel)
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @return 重排序模型实例
     */
    public ScoringModel buildScoringModel(KmModel model, String providerKey) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.config_empty"));
        }

        // 使用缓存
        if (model.getModelId() != null && scoringModelCache.containsKey(model.getModelId())) {
            return scoringModelCache.get(model.getModelId());
        }

        log.info("构建重排序模型: providerKey={}, modelKey={}", providerKey, model.getModelKey());

        ScoringModel scoringModel = switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "siliconflow" -> buildOpenAiScoringModel(model);
            case "local" -> buildLocalScoringModel(model);
            default -> throw new ServiceException(MessageUtils.message("ai.msg.model.unsupported_provider", providerKey));
        };

        if (model.getModelId() != null) {
            scoringModelCache.put(model.getModelId(), scoringModel);
        }

        return scoringModel;
    }

    private ScoringModel buildLocalScoringModel(KmModel model) {
        if (StrUtil.isBlank(model.getModelKey())) {
            throw new ServiceException(MessageUtils.message("ai.msg.rerank.local_path_required"));
        }

        String modelPath;
        String tokenizerPath;

        // 如果是内置模型，从配置文件获取绝对路径
        if ("bge-reranker-v2-m3".equals(model.getModelKey())) {
            modelPath = aiProperties.getReranker().getModelPath();
            tokenizerPath = aiProperties.getReranker().getTokenizerPath();
        } else {
            // 否则认为 ModelKey 就是路径
            modelPath = model.getModelKey();
            int dotIndex = modelPath.lastIndexOf(".");
            if (dotIndex > 0) {
                tokenizerPath = modelPath.substring(0, dotIndex) + ".tokenizer.json";
            } else {
                tokenizerPath = modelPath + ".tokenizer.json";
            }
        }

        if (StrUtil.isBlank(modelPath) || StrUtil.isBlank(tokenizerPath)) {
            throw new ServiceException(MessageUtils.message("ai.msg.rerank.local_path_not_configured"));
        }

        try {
            log.info("加载本地重排序模型: {}", modelPath);
            return new OnnxScoringModel(modelPath, tokenizerPath);
        } catch (Exception e) {
            log.error("构建本地重排序模型失败: {}", e.getMessage());
            throw new ServiceException(MessageUtils.message("ai.msg.rerank.build_failed", e.getMessage()));
        }
    }

    private ScoringModel buildOpenAiScoringModel(KmModel model) {
        // 获取 API Base，优先使用模型自定义，否则查询供应商默认地址
        String apiBase = model.getApiBase();
        if (StrUtil.isBlank(apiBase)) {
            KmModelProviderVo providerVo = kmModelServiceImpl.queryById(model.getProviderId());
            if (providerVo != null) {
                apiBase = providerVo.getDefaultEndpoint();
            }
        }
        return OpenAiCompatibleScoringModel.builder()
                .apiKey(model.getApiKey())
                .baseUrl(apiBase)
                .modelName(model.getModelKey())
                .timeout(DEFAULT_TIMEOUT)
                .build();
    }
}
