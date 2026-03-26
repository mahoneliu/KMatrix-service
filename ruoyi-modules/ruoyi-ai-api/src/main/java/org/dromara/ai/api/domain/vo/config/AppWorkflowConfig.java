package org.dromara.ai.api.domain.vo.config;

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
     * 是否启用调试模式
     */
    private Boolean enableDebug;

    /**
     * 历史记录保留天数
     */
    private Integer historyRetentionDays;
}
