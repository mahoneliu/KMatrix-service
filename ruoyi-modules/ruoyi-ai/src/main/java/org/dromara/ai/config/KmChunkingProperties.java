package org.dromara.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库分块配置属性
 * 绑定 application.yml 中的 km.chunking 配置项，提供系统级别的分块默认参数
 *
 * @author Mahone
 * @date 2026-02-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "km.chunking")
public class KmChunkingProperties {

    /**
     * 子块大小（字符数）
     * 默认 200，可被数据集级别配置覆盖
     */
    private int childChunkSize = 200;

    /**
     * 子块重叠大小（字符数）
     * 默认 20，可被数据集级别配置覆盖
     */
    private int childChunkOverlap = 20;
}
