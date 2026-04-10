package org.dromara.ai.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义参数验证配置类
 *
 * @author KMatrix Team
 * @date 2026-04-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "custom-param")
public class CustomParamConfig {

    /**
     * 参数名白名单
     * 支持前缀匹配，如 "ext_" 表示所有以 ext_ 开头的参数
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * 敏感参数名模式列表（正则表达式）
     * 用于检测 password, token, secret, key 等敏感字段
     * 默认包含常见敏感词模式
     */
    private List<String> sensitivePatterns = List.of(
            ".*password.*",
            ".*pwd.*",
            ".*token.*",
            ".*secret.*",
            ".*key.*",
            ".*private.*",
            ".*credential.*",
            ".*api_key.*",
            ".*access_key.*",
            ".*secret_key.*"
    );

    /**
     * 受保护参数列表
     * 这些参数不可被覆盖，优先保留
     * 用于系统级配置参数
     */
    private Set<String> protectedParams = new HashSet<>(Set.of(
            "appToken",
            "appId",
            "primaryColor",
            "theme",
            "mode",
            "ext_"
    ));

    /**
     * 参数名长度限制
     */
    private NameLength nameLength = new NameLength();

    /**
     * 参数值大小限制（字节）
     */
    private int valueMaxSize = 10240; // 10KB

    /**
     * 嵌套对象深度限制
     */
    private int maxDepth = 3;

    /**
     * 是否启用白名单模式
     * true: 只允许白名单中的参数
     * false: 允许所有非敏感参数（黑名单模式）
     */
    private boolean whitelistMode = true;

    /**
     * 是否启用敏感词检测
     */
    private boolean enableSensitiveCheck = true;

    /**
     * 是否启用嵌套深度检测
     */
    private boolean enableDepthCheck = true;

    /**
     * 是否启用参数值大小检测
     */
    private boolean enableSizeCheck = true;

    /**
     * 参数名长度配置
     */
    @Data
    public static class NameLength {
        /**
         * 最小长度
         */
        private int min = 2;

        /**
         * 最大长度
         */
        private int max = 50;
    }

    /**
     * 获取受保护参数列表（转为小写用于匹配）
     */
    public Set<String> getProtectedParamsLowercase() {
        return protectedParams.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * 检查参数名是否为受保护参数
     */
    public boolean isProtected(String paramName) {
        return protectedParams.contains(paramName) ||
                getProtectedParamsLowercase().contains(paramName.toLowerCase());
    }

    public boolean isSensitiveCheckEnabled() {
        return enableSensitiveCheck;
    }

    public boolean isWhitelistEnabled() {
        return whitelistMode;
    }

    public boolean isDepthCheckEnabled() {
        return enableDepthCheck;
    }

    public boolean isSizeCheckEnabled() {
        return enableSizeCheck;
    }
}
