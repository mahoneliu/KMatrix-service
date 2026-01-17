package org.dromara.ai.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.dromara.ai.domain.KmNodeDefinition;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;

@Data
@AutoMapper(target = KmNodeDefinition.class, reverseConvertGenerate = false)
public class KmNodeDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点定义ID
     */
    private Long nodeDefId;

    /**
     * 节点类型标识 (如 LLM_CHAT)
     */
    private String nodeType;

    /**
     * 节点显示名称
     */
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
     * 输入参数定义
     */
    @AutoMapping(ignore = true)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<NodeParamDefinitionVo> inputParams;

    /**
     * 输出参数定义
     */
    @AutoMapping(ignore = true)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<NodeParamDefinitionVo> outputParams;
}
