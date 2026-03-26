package org.dromara.ai.app.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import org.dromara.ai.api.domain.vo.config.AppModelConfig;
import org.dromara.ai.api.domain.vo.config.AppKnowledgeConfig;
import org.dromara.ai.api.domain.vo.config.AppWorkflowConfig;

/**
 * 应用配置快照
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
public class AppSnapshot implements Serializable {

    // 基础信息
    private String appName;
    private String description;
    private String icon;
    private String appType;
    private String prologue;

    // 配置
    private AppModelConfig modelSetting;
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

    // 关联
    private Long modelId;
}
