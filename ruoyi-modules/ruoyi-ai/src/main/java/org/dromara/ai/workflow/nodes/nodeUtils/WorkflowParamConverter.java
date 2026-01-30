package org.dromara.ai.workflow.nodes.nodeUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流参数类型转换工具类
 * 提供类型安全的参数转换方法,解决 Object 类型使用不便的问题
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Slf4j
public class WorkflowParamConverter {

    private WorkflowParamConverter() {
        // 工具类,禁止实例化
    }

    /**
     * 转换为 String 类型
     */
    public static String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * 转换为 String 类型 (无默认值)
     */
    public static String asString(Object value) {
        return asString(value, null);
    }

    /**
     * 转换为 Integer 类型
     */
    public static Integer asInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert {} to Integer, using default value: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转换为 Integer 类型 (无默认值)
     */
    public static Integer asInt(Object value) {
        return asInt(value, null);
    }

    /**
     * 转换为 Long 类型
     */
    public static Long asLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert {} to Long, using default value: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转换为 Long 类型 (无默认值)
     */
    public static Long asLong(Object value) {
        return asLong(value, null);
    }

    /**
     * 转换为 Double 类型
     */
    public static Double asDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert {} to Double, using default value: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转换为 Double 类型 (无默认值)
     */
    public static Double asDouble(Object value) {
        return asDouble(value, null);
    }

    /**
     * 转换为 Boolean 类型
     */
    public static Boolean asBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().toLowerCase();
        if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) {
            return true;
        }
        if ("false".equals(str) || "0".equals(str) || "no".equals(str)) {
            return false;
        }
        log.warn("Failed to convert {} to Boolean, using default value: {}", value, defaultValue);
        return defaultValue;
    }

    /**
     * 转换为 Boolean 类型 (无默认值)
     */
    public static Boolean asBoolean(Object value) {
        return asBoolean(value, null);
    }

    /**
     * 转换为 String List
     */
    @SuppressWarnings("unchecked")
    public static List<String> asStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (value instanceof String) {
            // 尝试解析逗号分隔的字符串
            String str = (String) value;
            if (str.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(str.split(","));
        }
        log.warn("Failed to convert {} to List<String>, returning empty list", value);
        return new ArrayList<>();
    }

    /**
     * 转换为 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value == null) {
            return new HashMap<>();
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        log.warn("Failed to convert {} to Map, returning empty map", value);
        return new HashMap<>();
    }

    /**
     * 泛型转换 (使用 Class 类型)
     */
    @SuppressWarnings("unchecked")
    public static <T> T as(Object value, Class<T> clazz, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            if (clazz.isInstance(value)) {
                return (T) value;
            }
            // 尝试基础类型转换
            if (clazz == String.class) {
                return (T) asString(value);
            }
            if (clazz == Integer.class) {
                return (T) asInt(value);
            }
            if (clazz == Long.class) {
                return (T) asLong(value);
            }
            if (clazz == Double.class) {
                return (T) asDouble(value);
            }
            if (clazz == Boolean.class) {
                return (T) asBoolean(value);
            }
            log.warn("Unsupported conversion from {} to {}, using default value", value.getClass(), clazz);
            return defaultValue;
        } catch (Exception e) {
            log.warn("Failed to convert {} to {}, using default value: {}", value, clazz, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 泛型转换 (无默认值)
     */
    public static <T> T as(Object value, Class<T> clazz) {
        return as(value, clazz, null);
    }
}
