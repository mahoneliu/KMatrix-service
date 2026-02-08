package org.dromara.ai.workflow.nodes.nodeUtils;

/**
 * 参数类型转换异常
 *
 * @author Mahone
 * @date 2026-02-08
 */
public class ParamConversionException extends RuntimeException {

    private final String paramKey;
    private final String sourceType;
    private final String targetType;
    private final Object sourceValue;

    public ParamConversionException(String paramKey, String sourceType, String targetType, Object sourceValue) {
        super(String.format("参数 [%s] 类型转换失败: 无法将 [%s] 类型的值 '%s' 转换为 [%s] 类型",
                paramKey, sourceType, sourceValue, targetType));
        this.paramKey = paramKey;
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceValue = sourceValue;
    }

    public ParamConversionException(String paramKey, String sourceType, String targetType, Object sourceValue,
            Throwable cause) {
        super(String.format("参数 [%s] 类型转换失败: 无法将 [%s] 类型的值 '%s' 转换为 [%s] 类型",
                paramKey, sourceType, sourceValue, targetType), cause);
        this.paramKey = paramKey;
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceValue = sourceValue;
    }

    public String getParamKey() {
        return paramKey;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public Object getSourceValue() {
        return sourceValue;
    }
}
