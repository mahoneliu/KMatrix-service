package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;

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
    private AppWorkflowConfig workflowConfig;

    // 关联
    private Long modelId;
}
