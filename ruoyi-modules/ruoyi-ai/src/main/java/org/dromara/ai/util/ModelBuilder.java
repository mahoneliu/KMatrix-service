package org.dromara.ai.util;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmModel;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

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

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * 构建聊天模型
     *
     * @param model       模型配置
     * @param providerKey 供应商标识
     * @return 聊天模型实例
     */
    public ChatLanguageModel buildChatModel(KmModel model, String providerKey) {
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException("模型配置或供应商标识为空");
        }

        log.info("构建聊天模型: providerKey={}, modelKey={}", providerKey, model.getModelKey());

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot" -> buildOpenAiModel(model);
            case "ollama", "vllm" -> buildOllamaModel(model);
            case "bailian", "zhipu", "qwen" -> buildQwenModel(model);
            case "gemini" -> buildGeminiModel(model);
            default -> throw new ServiceException("不支持的模型供应商: " + providerKey);
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
            throw new ServiceException("模型配置或供应商标识为空");
        }

        log.info("构建流式聊天模型: providerKey={}, modelKey={}, temperature={}, maxTokens={}",
                providerKey, model.getModelKey(), temperature, maxTokens);

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot" -> buildOpenAiStreamingModel(model, temperature, maxTokens);
            case "ollama", "vllm" -> buildOllamaStreamingModel(model, temperature, maxTokens);
            case "bailian", "zhipu", "qwen" -> buildQwenStreamingModel(model, temperature, maxTokens);
            case "gemini" -> buildGeminiStreamingModel(model, temperature, maxTokens);
            default -> throw new ServiceException("不支持的模型供应商: " + providerKey);
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
}
