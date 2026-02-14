package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;

/**
 * 参数定义
 *
 * @author Mahone
 * @date 2026-01-14
 */
@Data
public class ParamDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参数键名
     */
    private String key;

    /**
     * 参数名称
     */
    private String label;

    /**
     * 数据类型(string/number/boolean/object/array)
     */
    private String type;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 参数描述
     */
    private String description;
}
