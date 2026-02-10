package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 工作流模板对象 km_workflow_template
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_workflow_template", autoResultMap = true)
public class KmWorkflowTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId
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
    @TableField(typeHandler = JsonTypeHandler.class)
    private String workflowConfig;

    /**
     * 前端画布数据 (Vue Flow JSON)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String graphData;

    /**
     * 后端执行DSL (JSON)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String dslData;

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

}
