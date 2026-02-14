package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;

/**
 * 应用工作流配置
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
public class AppWorkflowConfig implements Serializable {

    /**
     * 这是预留的配置类，目前工作流主要数据存储在 KmWorkflow 表中。
     * 这里可以存储一些应用级别的工作流元数据或者默认配置。
     */

    /**
     * 是否启用调试模式
     */
    private Boolean enableDebug;

    /**
     * 历史记录保留天数
     */
    private Integer historyRetentionDays;
}
