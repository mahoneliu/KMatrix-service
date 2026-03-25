package org.dromara.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI模块配置属性
 *
 * @author Mahone
 * @date 2026-01-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class KmAiProperties {

    /**
     * 是否开启对话日志
     */
    private boolean logChat = false;

    /**
     * 是否开启全系统统一向量模型。
     * true: 全系统强制使用一个默认向量模型；
     * false: 允许每个知识库独立配置不同的向量模型。
     */
    private boolean unifiedEmbeddingModel = true;

    /**
     * 文件存储配置
     */
    private FileStore fileStore = new FileStore();

    /**
     * 重排序配置
     */
    private Reranker reranker = new Reranker();

    /**
     * 文件存储配置
     */
    @Data
    public static class FileStore {
        /**
         * 存储类型: 1-OSS, 2-本地文件
         */
        private Integer type = 1;

        /**
         * 本地文件存储路径(仅当 type=2 时生效)
         */
        private String localPath = "./uploads";
    }

    /**
     * 重排序配置
     */
    @Data
    public static class Reranker {
        /**
         * ONNX 模型文件路径
         */
        private String modelPath;

        /**
         * Tokenizer 文件路径
         */
        private String tokenizerPath;
    }

}
