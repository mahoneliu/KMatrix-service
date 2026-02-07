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
     * 文件存储配置
     */
    private FileStore fileStore = new FileStore();

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

}
