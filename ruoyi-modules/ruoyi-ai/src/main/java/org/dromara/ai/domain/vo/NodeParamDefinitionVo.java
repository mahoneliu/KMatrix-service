package org.dromara.ai.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * 节点参数定义 VO
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
public class NodeParamDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数键名
     */
    private String key;

    /**
     * 参数显示名称
     */
    private String label;

    /**
     * 参数数据类型 (string, number, boolean, object, array)
     */
    private String type;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 参数描述
     */
    private String description;
}
