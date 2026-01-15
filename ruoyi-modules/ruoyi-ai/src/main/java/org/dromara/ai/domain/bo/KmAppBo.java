package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmApp;
import org.dromara.ai.domain.vo.config.AppKnowledgeConfig;
import org.dromara.ai.domain.vo.config.AppModelConfig;
import org.dromara.ai.domain.vo.config.AppParametersConfig;
import org.dromara.ai.domain.vo.config.AppWorkflowConfig;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI应用业务对象 km_app
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmApp.class, reverseConvertGenerate = false)
public class KmAppBo extends BaseEntity {

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    @Size(min = 0, max = 64, message = "应用名称长度不能超过64个字符")
    private String appName;

    /**
     * 应用描述
     */
    @Size(min = 0, max = 500, message = "应用描述长度不能超过500个字符")
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
    private AppModelConfig modelSetting;

    /**
     * 知识库配置
     */
    private AppKnowledgeConfig knowledgeSetting;

    /**
     * 工作流配置(元配置)
     */
    private AppWorkflowConfig workflowConfig;

    /**
     * 前端画布数据(JSON)
     */
    private String graphData;

    /**
     * 后端执行DSL(JSON)
     */
    private String dslData;

    /**
     * 应用参数配置(全局/接口/会话)
     */
    private AppParametersConfig parameters;

    /**
     * 关联LLM模型ID
     */
    private Long modelId;

    /**
     * 关联知识库ID集合（逗号分隔，用于快速设置或兼容前端）
     * 实际存储在 km_app_knowledge
     */
    private String knowledgeIds;

    /**
     * 备注
     */
    private String remark;

}
