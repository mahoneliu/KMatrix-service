package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmNodeDefinition;
import org.dromara.ai.domain.vo.NodeParamDefinitionVo;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;

import java.util.List;

/**
 * 节点定义业务对象 km_node_definition
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmNodeDefinition.class, reverseConvertGenerate = false)
public class KmNodeDefinitionBo extends BaseEntity {

    /**
     * 节点定义ID
     */
    private Long nodeDefId;

    /**
     * 节点类型标识 (如 LLM_CHAT)
     */
    @NotBlank(message = "{ai.val.node.type_required}")
    private String nodeType;

    /**
     * 节点显示名称
     */
    @NotBlank(message = "{ai.val.node.name_required}")
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
    @NotBlank(message = "{ai.val.node.category_required}")
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
     * 是否允许自定义输入参数 (0否/1是)
     */
    private String allowCustomInputParams;

    /**
     * 是否允许自定义输出参数 (0否/1是)
     */
    private String allowCustomOutputParams;

    /**
     * 输入参数定义列表
     */
    @AutoMapping(ignore = true)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<NodeParamDefinitionVo> inputParams;

    /**
     * 输出参数定义列表
     */
    @AutoMapping(ignore = true)
    @TableField(typeHandler = JacksonTypeHandler.class)
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
