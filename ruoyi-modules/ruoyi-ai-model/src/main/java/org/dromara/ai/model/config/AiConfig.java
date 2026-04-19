package org.dromara.ai.model.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * AI 模块配置类
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Configuration
public class AiConfig {

    /**
     * 配置 Embedding 模型Bean
     * 支持根据配置切换供应商，避免 ONNX 启动依赖报错
     */
    @Lazy
    @Bean
    public EmbeddingModel embeddingModel(KmAiProperties properties) {
        KmAiProperties.Embedding config = properties.getEmbedding();
        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "local";
        String modelKey = config.getModelKey();

        return switch (provider) {
            case "local" -> new BgeSmallZhV15EmbeddingModel();
            case "openai" -> OpenAiEmbeddingModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(modelKey)
                    .baseUrl(config.getApiBase())
                    .build();
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(config.getApiBase() != null ? config.getApiBase() : "http://localhost:11434")
                    .modelName(modelKey)
                    .build();
            case "qwen" -> QwenEmbeddingModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(modelKey)
                    .build();
            default -> throw new RuntimeException("Unsupported system default embedding provider: " + provider);
        };
    }
}
