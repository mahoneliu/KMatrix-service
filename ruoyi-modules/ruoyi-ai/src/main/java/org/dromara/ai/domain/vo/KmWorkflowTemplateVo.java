package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmWorkflowTemplate;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流模板视图对象 km_workflow_template
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@AutoMapper(target = KmWorkflowTemplate.class)
public class KmWorkflowTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码 (唯一标识)
     */
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
     * 模板分类 (客服/营销/知识问答等)
     */
    private String category;

    /**
     * 作用域类型 (0系统级)
     */
    private String scopeType;

    /**
     * 工作流DSL配置 (WorkflowConfig JSON)
     */
    private String workflowConfig;

    /**
     * 前端画布数据 (Vue Flow JSON)
     */
    private String graphData;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 父版本ID
     */
    private Long parentVersionId;

    /**
     * 是否已发布 (0否/1是)
     */
    private String isPublished;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    private String updateByName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
