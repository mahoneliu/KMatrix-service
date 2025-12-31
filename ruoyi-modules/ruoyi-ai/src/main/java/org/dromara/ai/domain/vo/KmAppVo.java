package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmApp;
import org.dromara.ai.domain.vo.config.AppKnowledgeConfig;
import org.dromara.ai.domain.vo.config.AppModelConfig;
import org.dromara.ai.domain.vo.config.AppWorkflowConfig;

import java.io.Serializable;
import java.util.Date;

/**
 * AI应用视图对象 km_app
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@AutoMapper(target = KmApp.class)
public class KmAppVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
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
    private AppModelConfig modelSetting;

    /**
     * 知识库配置
     */
    private AppKnowledgeConfig knowledgeSetting;

    /**
     * 工作流配置
     */
    private AppWorkflowConfig workflowConfig;

    /**
     * 关联LLM模型ID
     */
    private Long modelId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
