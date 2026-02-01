package org.dromara.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * 目前使用本地 ONNX 模型 (AllMiniLmL6V2)
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
