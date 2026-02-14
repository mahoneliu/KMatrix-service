package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmWorkflowTemplate;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 工作流模板业务对象 km_workflow_template
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmWorkflowTemplate.class, reverseConvertGenerate = false)
public class KmWorkflowTemplateBo extends BaseEntity {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空", groups = { EditGroup.class })
    private Long templateId;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String templateName;

    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String templateCode;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板图标
     */
    private String icon;

    /**
     * 模板分类
     */
    private String category;

    /**
     * 作用域类型 (0系统级)
     */
    private String scopeType;

    /**
     * 工作流DSL配置
     */
    private String workflowConfig;

    /**
     * 前端画布数据
     */
    private String graphData;

    /**
     * 后端执行DSL
     */
    private String dslData;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

}
