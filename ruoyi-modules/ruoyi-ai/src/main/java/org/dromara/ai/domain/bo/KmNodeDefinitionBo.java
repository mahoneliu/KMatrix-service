package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.vo.NodeParamDefinitionVo;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.List;

/**
 * 节点定义业务对象 km_node_definition
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KmNodeDefinitionBo extends BaseEntity {

    /**
     * 节点定义ID
     */
    private Long nodeDefId;

    /**
     * 节点类型标识 (如 LLM_CHAT)
     */
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    /**
     * 节点显示名称
     */
    @NotBlank(message = "节点名称不能为空")
    private String nodeLabel;

    /**
     * 节点图标 (Iconify)
     */
    private String nodeIcon;

    /**
     * 节点颜色 (HEX)
     */
    private String nodeColor;

    /**
     * 节点分类 (basic/ai/logic/action)
     */
    @NotBlank(message = "节点分类不能为空")
    private String category;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 是否系统节点 (0否/1是)
     */
    private String isSystem;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

    /**
     * 输入参数定义列表
     */
    private List<NodeParamDefinitionVo> inputParams;

    /**
     * 输出参数定义列表
     */
    private List<NodeParamDefinitionVo> outputParams;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 备注
     */
    private String remark;

}
