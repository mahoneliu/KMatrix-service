package org.dromara.ai.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class KmNodeDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点显示名称
     */
    private String label;

    /**
     * 节点图标
     */
    private String icon;

    /**
     * 节点颜色
     */
    private String color;

    /**
     * 节点分类 (basic, ai, logic, action)
     */
    private String category;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 是否为系统节点
     */
    private Boolean isSystem;

    /**
     * 输入参数定义
     */
    private List<NodeParamDefinitionVo> inputParams;

    /**
     * 输出参数定义
     */
    private List<NodeParamDefinitionVo> outputParams;
}
