package org.dromara.ai.app.validator;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.config.CustomParamConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 自定义参数验证器
 * <p>
 * 实现以下验证规则：
 * <ul>
 *   <li>参数名长度限制：2-50 字符</li>
 *   <li>参数值大小限制：≤10KB（序列化后）</li>
 *   <li>嵌套对象深度限制：≤3 层</li>
 *   <li>敏感参数名检测：password, token, secret, key 等</li>
 *   <li>白名单验证：从配置或数据库加载允许的参数名</li>
 * </ul>
 *
 * @author KMatrix Team
 * @date 2026-04-09
 */
@Slf4j
@Component
public class CustomParamValidator implements ConstraintValidator<ValidCustomParam, Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Autowired
    private CustomParamConfig config;

    /**
     * 缓存编译后的正则表达式（避免重复编译）
     */
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * 初始化校验器
     */
    @Override
    public void initialize(ValidCustomParam annotation) {
        log.debug("初始化 CustomParamValidator，配置：whitelistEnabled={}, sensitiveCheckEnabled={}, depthCheckEnabled={}, sizeCheckEnabled={}",
                annotation.whitelistEnabled(), annotation.sensitiveCheckEnabled(),
                annotation.depthCheckEnabled(), annotation.sizeCheckEnabled());
    }

    /**
     * 校验对象是否符合自定义参数规范
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // 空值由@NotNull等注解处理
        }

        try {
            if (value instanceof Map) {
                validateMap((Map<?, ?>) value, 0, context);
                return true;
            } else if (value instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
                validateMapEntry(entry, 0, context);
                return true;
            } else if (value.getClass().isAnnotationPresent(ValidCustomParam.class)) {
                // 验证 JavaBean 对象
                validateBean(value, 0, context);
                return true;
            }
        } catch (Exception e) {
            log.error("自定义参数验证异常", e);
            buildViolation(context, "VALIDATION_ERROR", "验证过程异常：" + e.getMessage(), null);
            return false;
        }

        return true;
    }

    /**
     * 验证 Map 类型的参数
     */
    private void validateMap(Map<?, ?> map, int depth, ConstraintValidatorContext context) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toString() : null;
            Object val = entry.getValue();

            if (StrUtil.isBlank(key)) {
                log.warn("参数名不能为空");
                buildViolation(context, "PARAM_NAME_EMPTY", "参数名不能为空", null);
                return;
            }

            // 逐层验证
            validateParamName(key, depth, context);
            validateParamValue(key, val, depth, context);
        }
    }

    /**
     * 验证 Map.Entry 类型的参数
     */
    private void validateMapEntry(Map.Entry<?, ?> entry, int depth, ConstraintValidatorContext context) {
        String key = entry.getKey() != null ? entry.getKey().toString() : null;
        Object val = entry.getValue();

        if (StrUtil.isBlank(key)) {
            log.warn("参数名不能为空");
            buildViolation(context, "PARAM_NAME_EMPTY", "参数名不能为空", null);
            return;
        }

        validateParamName(key, depth, context);
        validateParamValue(key, val, depth, context);
    }

    /**
     * 验证 JavaBean 对象的字段
     */
    private void validateBean(Object bean, int depth, ConstraintValidatorContext context) {
        if (bean == null) {
            return;
        }

        Class<?> clazz = bean.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(ValidCustomParam.class)) {
                    field.setAccessible(true);
                    try {
                        Object fieldValue = field.get(bean);
                        if (fieldValue != null) {
                            String fieldName = field.getName();
                            validateParamName(fieldName, depth, context);
                            validateParamValue(fieldName, fieldValue, depth, context);
                        }
                    } catch (IllegalAccessException e) {
                        log.warn("无法访问字段：{}", field.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 验证参数名
     */
    private void validateParamName(String paramName, int depth, ConstraintValidatorContext context) {
        // 1. 参数名长度检查
        validateParamNameLength(paramName, context);

        // 2. 敏感参数名检测
        if (config.isSensitiveCheckEnabled() || config.isSensitiveCheckEnabled()) {
            validateSensitiveParam(paramName, context);
        }

        // 3. 白名单验证
        if (config.isWhitelistEnabled() || config.isWhitelistEnabled()) {
            validateWhitelist(paramName, context);
        }

        // 4. 受保护参数检查
        if (config.isProtected(paramName)) {
            log.warn("尝试修改受保护参数：{}", paramName);
            buildViolation(context, "PARAM_PROTECTED", "受保护参数不可修改：" + paramName, paramName);
        }
    }

    /**
     * 验证参数名长度
     */
    private void validateParamNameLength(String paramName, ConstraintValidatorContext context) {
        int minLength = config.getNameLength().getMin();
        int maxLength = config.getNameLength().getMax();
        int actualLength = paramName.length();

        if (actualLength < minLength) {
            log.warn("参数名长度过短：{} < {}", paramName, minLength);
            buildViolation(context, "PARAM_NAME_TOO_SHORT",
                    String.format("参数名长度过短：%s < %d 字符", paramName, minLength), paramName);
            return;
        }

        if (actualLength > maxLength) {
            log.warn("参数名长度过长：{} > {}", paramName, maxLength);
            buildViolation(context, "PARAM_NAME_TOO_LONG",
                    String.format("参数名长度过长：%s > %d 字符", paramName, maxLength), paramName);
            return;
        }
    }

    /**
     * 验证敏感参数名
     */
    private void validateSensitiveParam(String paramName, ConstraintValidatorContext context) {
        List<String> patterns = config.getSensitivePatterns();

        for (String patternStr : patterns) {
            Pattern pattern = getCompiledPattern(patternStr);
            if (pattern == null) {
                continue;
            }

            if (pattern.matcher(paramName).matches()) {
                log.warn("检测到敏感参数名：{} (匹配模式：{})", paramName, patternStr);
                buildViolation(context, "PARAM_SENSITIVE",
                        String.format("敏感参数名：%s (匹配模式：%s)", paramName, patternStr), paramName);
                return;
            }
        }
    }

    /**
     * 验证白名单
     */
    private void validateWhitelist(String paramName, ConstraintValidatorContext context) {
        List<String> whitelist = config.getWhitelist();
        String paramNameLower = paramName.toLowerCase();

        // 检查精确匹配
        for (String whiteListedParam : whitelist) {
            if (paramNameLower.equals(whiteListedParam.toLowerCase())) {
                return; // 匹配成功
            }
        }

        // 检查前缀匹配
        for (String whiteListedParam : whitelist) {
            if (paramNameLower.startsWith(whiteListedParam.toLowerCase())) {
                return; // 前缀匹配成功
            }
        }

        // 检查受保护参数
        if (config.isProtected(paramName)) {
            return; // 受保护参数自动通过
        }

        log.warn("参数名不在白名单中：{}", paramName);
        buildViolation(context, "PARAM_NOT_IN_WHITELIST",
                String.format("参数名不在白名单中：%s", paramName), paramName);
    }

    /**
     * 验证参数值
     */
    private void validateParamValue(String paramName, Object value, int depth, ConstraintValidatorContext context) {
        // 1. 嵌套深度检查
        if (config.isDepthCheckEnabled() || config.isDepthCheckEnabled()) {
            validateDepth(paramName, value, depth, context);
        }

        // 2. 参数值大小检查
        if (config.isSizeCheckEnabled() || config.isSizeCheckEnabled()) {
            validateSize(paramName, value, context);
        }
    }

    /**
     * 验证嵌套深度
     */
    private void validateDepth(String paramName, Object value, int currentDepth, ConstraintValidatorContext context) {
        int maxDepth = config.getMaxDepth();

        // 判断是否为嵌套对象/Map
        if (value instanceof Map || value.getClass().isAnnotationPresent(ValidCustomParam.class) ||
                !value.getClass().isPrimitive() && !isSimpleType(value.getClass())) {
            int newDepth = currentDepth + 1;

            if (newDepth > maxDepth) {
                log.warn("嵌套深度超限：参数={} 深度={} > 最大深度={}", paramName, newDepth, maxDepth);
                buildViolation(context, "PARAM_DEPTH_EXCEEDED",
                        String.format("嵌套深度超限：%s 深度=%d > 最大深度=%d", paramName, newDepth, maxDepth), paramName);
                return;
            }

            // 递归验证嵌套对象
            if (value instanceof Map) {
                validateMap((Map<?, ?>) value, newDepth, context);
            } else if (value.getClass().isAnnotationPresent(ValidCustomParam.class)) {
                validateBean(value, newDepth, context);
            }
        }
    }

    /**
     * 验证参数值大小（序列化后）
     */
    private void validateSize(String paramName, Object value, ConstraintValidatorContext context) {
        int maxSize = config.getValueMaxSize();

        try {
            byte[] serialized = OBJECT_MAPPER.writeValueAsBytes(value);
            long actualSize = serialized.length;

            if (actualSize > maxSize) {
                log.warn("参数值大小超限：参数={} 大小={} bytes > 最大={} bytes", paramName, actualSize, maxSize);
                buildViolation(context, "PARAM_SIZE_EXCEEDED",
                        String.format("参数值大小超限：%s 大小=%d bytes > 最大=%d bytes", paramName, actualSize, maxSize), paramName);
                return;
            }
        } catch (JsonProcessingException e) {
            log.error("序列化参数值失败：{}", paramName, e);
            buildViolation(context, "SERIALIZATION_ERROR",
                    String.format("序列化参数值失败：%s", paramName), paramName);
        }
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Integer.class || clazz == Integer.TYPE ||
                clazz == Long.class || clazz == Long.TYPE ||
                clazz == Double.class || clazz == Double.TYPE ||
                clazz == Float.class || clazz == Float.TYPE ||
                clazz == Boolean.class || clazz == Boolean.TYPE ||
                clazz == Short.class || clazz == Short.TYPE ||
                clazz == Character.class || clazz == Character.TYPE ||
                clazz == Byte.class || clazz == Byte.TYPE ||
                clazz == java.util.Date.class ||
                clazz == java.time.LocalDate.class ||
                clazz == java.time.LocalDateTime.class;
    }

    /**
     * 获取编译后的 Pattern（带缓存）
     */
    private Pattern getCompiledPattern(String patternStr) {
        if (patternStr == null || patternStr.isEmpty()) {
            return null;
        }

        return patternCache.computeIfAbsent(patternStr, key -> {
            try {
                return Pattern.compile(key, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                log.error("正则表达式编译失败：{}", key, e);
                return null;
            }
        });
    }

    /**
     * 构建违反约束的上下文
     */
    private void buildViolation(ConstraintValidatorContext context, String errorType,
                                String message, String paramName) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
