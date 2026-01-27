package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.vo.config.AppKnowledgeConfig;
import org.dromara.ai.domain.vo.config.AppModelConfig;
import org.dromara.ai.domain.vo.config.AppParametersConfig;
import org.dromara.ai.domain.vo.config.AppWorkflowConfig;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * AI应用对象 km_app
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_app", autoResultMap = true)
public class KmApp extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "app_id")
    private Long appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用描述
     */
    private String description;

    /**
     * 应用图标
     */
    private String icon;

    /**
     * 应用类型（1基础对话 2工作流）
     */
    private String appType;

    /**
     * 状态（0草稿 1发布）
     */
    private String status;

    /**
     * 开场白
     */
    private String prologue;

    /**
     * 模型配置
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private AppModelConfig modelSetting;

    /**
     * 知识库配置
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private AppKnowledgeConfig knowledgeSetting;

    /**
     * 工作流配置(元配置)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private AppWorkflowConfig workflowConfig;

    /**
     * 前端画布数据(JSON)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String graphData;

    /**
     * 后端执行DSL(JSON)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String dslData;

    /**
     * 应用参数配置(全局/接口/会话)
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private AppParametersConfig parameters;

    /**
     * 关联LLM模型ID
     */
    private Long modelId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否启用执行详情（0禁用 1启用）
     */
    private String enableExecutionDetail;

    /**
     * 公开访问（0关闭 1开启）
     */
    private String publicAccess;

    /**
     * 删除标志
     */
    private String delFlag;

}
