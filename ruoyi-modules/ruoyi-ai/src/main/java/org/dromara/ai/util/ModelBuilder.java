package org.dromara.ai.util;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.KmModel;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
        if (model == null || StrUtil.isBlank(providerKey)) {
            throw new ServiceException("模型配置或供应商标识为空");
        }

        log.info("构建流式聊天模型: providerKey={}, modelKey={}", providerKey, model.getModelKey());

        return switch (providerKey.toLowerCase()) {
            case "openai", "deepseek", "moonshot" -> buildOpenAiStreamingModel(model);
            case "ollama", "vllm" -> buildOllamaStreamingModel(model);
            case "bailian", "zhipu", "qwen" -> buildQwenStreamingModel(model);
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
    private StreamingChatLanguageModel buildOpenAiStreamingModel(KmModel model) {
        var builder = OpenAiStreamingChatModel.builder()
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
    private StreamingChatLanguageModel buildOllamaStreamingModel(KmModel model) {
        String baseUrl = StrUtil.isNotBlank(model.getApiBase())
                ? model.getApiBase()
                : "http://localhost:11434";

        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model.getModelKey())
                .logRequests(aiProperties.isLogChat())
                .logResponses(aiProperties.isLogChat())
                .timeout(DEFAULT_TIMEOUT)
                .build();
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
    private StreamingChatLanguageModel buildQwenStreamingModel(KmModel model) {
        return QwenStreamingChatModel.builder()
                .apiKey(model.getApiKey())
                .modelName(model.getModelKey())
                .build();
    }
}
