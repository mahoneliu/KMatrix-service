package org.dromara.ai.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 存储模块配置属性
 * 配置前缀 ai.file-store，与 KmAiProperties.FileStore 共享同一 yml 节点
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.file-store")
public class KmAiStorageProperties {

    /**
     * 存储类型: 1-OSS, 2-本地文件
     */
    private Integer type = 1;

    /**
     * 本地文件存储路径(仅当 type=2 时生效)
     */
    private String localPath = "./uploads";

    /**
     * 本地文件访问前缀
     */
    private String localPrefix = "/ai/storage/file";
}
