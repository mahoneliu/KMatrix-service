package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 工作流节点定义对象 km_node_definition
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_node_definition")
public class KmNodeDefinition extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 节点定义ID
     */
    @TableId
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
     * 输入参数定义 (JSON Array)
     */
    private String inputParams;

    /**
     * 输出参数定义 (JSON Array)
     */
    private String outputParams;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 父版本ID (用于追溯历史)
     */
    private Long parentVersionId;

}
