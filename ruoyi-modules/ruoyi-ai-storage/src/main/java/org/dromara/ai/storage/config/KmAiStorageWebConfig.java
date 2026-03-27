package org.dromara.ai.storage.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

/**
 * AI 存储模块网路配置
 * 用于映射本地文件存储路径到 Web 访问前缀
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Configuration
@RequiredArgsConstructor
public class KmAiStorageWebConfig implements WebMvcConfigurer {

    private final KmAiStorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (storageProperties.getType() == 2) {
            String localPath = storageProperties.getLocalPath();
            String localPrefix = storageProperties.getLocalPrefix();

            // 确保以 / 结尾
            if (!localPrefix.endsWith("/")) {
                localPrefix += "/";
            }
            if (!localPrefix.startsWith("/")) {
                localPrefix = "/" + localPrefix;
            }

            // 转换绝对路径为 Spring Resource 格式
            File file = new File(localPath);
            String absolutePath = file.getAbsolutePath().replace("\\", "/");
            if (!absolutePath.endsWith("/")) {
                absolutePath += "/";
            }

            String location = "file:" + absolutePath;

            registry.addResourceHandler(localPrefix + "**")
                    .addResourceLocations(location);
        }
    }
}
