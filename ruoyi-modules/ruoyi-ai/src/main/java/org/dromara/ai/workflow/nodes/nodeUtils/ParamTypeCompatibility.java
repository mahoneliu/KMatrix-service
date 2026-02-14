package org.dromara.ai.workflow.nodes.nodeUtils;

/**
 * 参数类型兼容性枚举
 * 定义类型转换的兼容性规则
 *
 * @author Mahone
 * @date 2026-02-08
 */
public enum ParamTypeCompatibility {

    /**
     * 完全兼容 - 可以安全转换
     */
    COMPATIBLE,

    /**
     * 有条件兼容 - 转换可能失败（如 string -> number，需要字符串是合法数字）
     */
    CONDITIONAL,

    /**
     * 不兼容 - 不能转换
     */
    INCOMPATIBLE;

    /**
     * 参数数据类型
     */
    public enum ParamDataType {
        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array"),
        DATETIME("datetime");

        private final String value;

        ParamDataType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * 从字符串解析类型
         */
        public static ParamDataType fromString(String type) {
            if (type == null) {
                return STRING; // 默认为字符串类型
            }
            for (ParamDataType t : values()) {
                if (t.value.equalsIgnoreCase(type)) {
                    return t;
                }
            }
            return STRING;
        }
    }

    /**
     * 类型兼容性矩阵
     * 行: 目标类型, 列: 源类型
     * 顺序: STRING, NUMBER, BOOLEAN, OBJECT, ARRAY, DATETIME
     */
    private static final ParamTypeCompatibility[][] COMPATIBILITY_MATRIX = {
            // 目标类型: STRING
            { COMPATIBLE, COMPATIBLE, COMPATIBLE, INCOMPATIBLE, INCOMPATIBLE, COMPATIBLE },
            // 目标类型: NUMBER
            { CONDITIONAL, COMPATIBLE, CONDITIONAL, INCOMPATIBLE, INCOMPATIBLE, INCOMPATIBLE },
            // 目标类型: BOOLEAN
            { CONDITIONAL, CONDITIONAL, COMPATIBLE, INCOMPATIBLE, INCOMPATIBLE, INCOMPATIBLE },
            // 目标类型: OBJECT
            { CONDITIONAL, INCOMPATIBLE, INCOMPATIBLE, COMPATIBLE, INCOMPATIBLE, INCOMPATIBLE },
            // 目标类型: ARRAY
            { CONDITIONAL, INCOMPATIBLE, INCOMPATIBLE, INCOMPATIBLE, COMPATIBLE, INCOMPATIBLE },
            // 目标类型: DATETIME
            { CONDITIONAL, CONDITIONAL, INCOMPATIBLE, INCOMPATIBLE, INCOMPATIBLE, COMPATIBLE }
    };

    /**
     * 检查类型兼容性
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 兼容性级别
     */
    public static ParamTypeCompatibility check(String sourceType, String targetType) {
        ParamDataType source = ParamDataType.fromString(sourceType);
        ParamDataType target = ParamDataType.fromString(targetType);
        return check(source, target);
    }

    /**
     * 检查类型兼容性
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 兼容性级别
     */
    public static ParamTypeCompatibility check(ParamDataType sourceType, ParamDataType targetType) {
        if (sourceType == null || targetType == null) {
            return COMPATIBLE; // 未知类型视为兼容
        }
        return COMPATIBILITY_MATRIX[targetType.ordinal()][sourceType.ordinal()];
    }

    /**
     * 是否可以转换（兼容或有条件兼容）
     */
    public static boolean canConvert(String sourceType, String targetType) {
        ParamTypeCompatibility compat = check(sourceType, targetType);
        return compat != INCOMPATIBLE;
    }

    /**
     * 是否完全兼容
     */
    public static boolean isFullyCompatible(String sourceType, String targetType) {
        return check(sourceType, targetType) == COMPATIBLE;
    }

    /**
     * 获取兼容性描述信息
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 描述信息
     */
    public static String getCompatibilityMessage(String sourceType, String targetType) {
        ParamTypeCompatibility compat = check(sourceType, targetType);
        switch (compat) {
            case COMPATIBLE:
                return null; // 兼容无需提示
            case CONDITIONAL:
                return String.format("类型 [%s] 转换到 [%s] 可能失败，请确保数据格式正确", sourceType, targetType);
            case INCOMPATIBLE:
                return String.format("类型 [%s] 无法转换到 [%s]", sourceType, targetType);
            default:
                return null;
        }
    }
}
