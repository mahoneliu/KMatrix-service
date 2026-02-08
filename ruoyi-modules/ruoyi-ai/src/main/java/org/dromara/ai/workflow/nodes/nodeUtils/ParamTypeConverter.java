package org.dromara.ai.workflow.nodes.nodeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.vo.config.ParamDefinition;
import org.dromara.ai.workflow.nodes.nodeUtils.ParamTypeCompatibility.ParamDataType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 参数类型转换器
 * 根据参数定义自动进行类型转换
 *
 * @author Mahone
 * @date 2026-02-08
 */
@Slf4j
public class ParamTypeConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ParamTypeConverter() {
        // 工具类禁止实例化
    }

    /**
     * 根据参数定义列表转换输入参数
     *
     * @param inputs    原始输入参数
     * @param paramDefs 参数定义列表
     * @return 转换后的参数
     */
    public static Map<String, Object> convertInputs(
            Map<String, Object> inputs,
            List<ParamDefinition> paramDefs) {

        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }

        if (paramDefs == null || paramDefs.isEmpty()) {
            return inputs; // 没有参数定义，不进行转换
        }

        // 构建参数定义映射
        Map<String, ParamDefinition> defMap = new HashMap<>();
        for (ParamDefinition def : paramDefs) {
            defMap.put(def.getKey(), def);
        }

        Map<String, Object> result = new HashMap<>(inputs);

        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            ParamDefinition def = defMap.get(key);
            if (def == null) {
                continue; // 没有定义的参数，保持原样
            }

            String targetType = def.getType();
            if (targetType == null) {
                continue;
            }

            try {
                Object converted = convertValue(value, targetType, key, def.getRequired());
                result.put(key, converted);
            } catch (ParamConversionException e) {
                // 转换失败
                if (Boolean.TRUE.equals(def.getRequired())) {
                    // 必填参数抛异常
                    throw e;
                } else {
                    // 非必填参数使用默认值
                    log.warn("参数 [{}] 转换失败，使用默认值: {}", key, def.getDefaultValue());
                    result.put(key, parseDefaultValue(def.getDefaultValue(), targetType));
                }
            }
        }

        return result;
    }

    /**
     * 转换单个值到目标类型
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @param paramKey   参数键（用于错误信息）
     * @param required   是否必填
     * @return 转换后的值
     */
    public static Object convertValue(Object value, String targetType, String paramKey, Boolean required) {
        if (value == null) {
            return null;
        }

        String sourceType = detectType(value);
        ParamTypeCompatibility compatibility = ParamTypeCompatibility.check(sourceType, targetType);

        if (compatibility == ParamTypeCompatibility.INCOMPATIBLE) {
            throw new ParamConversionException(paramKey, sourceType, targetType, value);
        }

        ParamDataType target = ParamDataType.fromString(targetType);

        try {
            switch (target) {
                case STRING:
                    return convertToString(value);
                case NUMBER:
                    return convertToNumber(value);
                case BOOLEAN:
                    return convertToBoolean(value);
                case OBJECT:
                    return convertToObject(value);
                case ARRAY:
                    return convertToArray(value);
                case DATETIME:
                    return convertToDatetime(value);
                default:
                    return value;
            }
        } catch (ParamConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new ParamConversionException(paramKey, sourceType, targetType, value, e);
        }
    }

    /**
     * 检测值的类型
     */
    public static String detectType(Object value) {
        if (value == null) {
            return "string";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Map) {
            return "object";
        }
        if (value instanceof List || value instanceof Object[]) {
            return "array";
        }
        if (value instanceof LocalDateTime || value instanceof Date) {
            return "datetime";
        }
        return "string";
    }

    // ========== 类型转换方法 ==========

    private static String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof Date) {
            return value.toString();
        }
        return value.toString();
    }

    private static Number convertToNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return null;
            }
            try {
                if (str.contains(".")) {
                    return Double.parseDouble(str);
                }
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new RuntimeException("无法将字符串转换为数字: " + str);
            }
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        throw new RuntimeException("无法转换为数字类型");
    }

    private static Boolean convertToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String str = ((String) value).trim().toLowerCase();
            if ("true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str)) {
                return true;
            }
            if ("false".equals(str) || "0".equals(str) || "no".equals(str) || "off".equals(str)) {
                return false;
            }
            throw new RuntimeException("无法将字符串转换为布尔值: " + str);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        throw new RuntimeException("无法转换为布尔类型");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertToObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return new HashMap<>();
            }
            try {
                return OBJECT_MAPPER.readValue(str, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("无法将字符串解析为对象: " + str);
            }
        }
        throw new RuntimeException("无法转换为对象类型");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> convertToArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return (List<Object>) value;
        }
        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return new ArrayList<>();
            }
            // 尝试解析 JSON 数组
            if (str.startsWith("[")) {
                try {
                    return OBJECT_MAPPER.readValue(str, List.class);
                } catch (JsonProcessingException e) {
                    // 解析失败，尝试按逗号分隔
                }
            }
            // 按逗号分隔
            return new ArrayList<>(Arrays.asList(str.split(",")));
        }
        throw new RuntimeException("无法转换为数组类型");
    }

    private static Object convertToDatetime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime || value instanceof Date) {
            return value;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e1) {
                try {
                    return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e2) {
                    throw new RuntimeException("无法将字符串解析为日期时间: " + str);
                }
            }
        }
        if (value instanceof Number) {
            // 假设是时间戳
            long timestamp = ((Number) value).longValue();
            return new Date(timestamp);
        }
        throw new RuntimeException("无法转换为日期时间类型");
    }

    /**
     * 解析默认值
     */
    private static Object parseDefaultValue(String defaultValue, String targetType) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return getTypeDefault(targetType);
        }

        ParamDataType target = ParamDataType.fromString(targetType);
        try {
            switch (target) {
                case STRING:
                    return defaultValue;
                case NUMBER:
                    if (defaultValue.contains(".")) {
                        return Double.parseDouble(defaultValue);
                    }
                    return Long.parseLong(defaultValue);
                case BOOLEAN:
                    return Boolean.parseBoolean(defaultValue);
                case OBJECT:
                    return OBJECT_MAPPER.readValue(defaultValue, Map.class);
                case ARRAY:
                    return OBJECT_MAPPER.readValue(defaultValue, List.class);
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            log.warn("解析默认值失败: {}", defaultValue, e);
            return getTypeDefault(targetType);
        }
    }

    /**
     * 获取类型的零值
     */
    private static Object getTypeDefault(String targetType) {
        ParamDataType target = ParamDataType.fromString(targetType);
        switch (target) {
            case STRING:
                return "";
            case NUMBER:
                return 0;
            case BOOLEAN:
                return false;
            case OBJECT:
                return new HashMap<>();
            case ARRAY:
                return new ArrayList<>();
            default:
                return null;
        }
    }
}
