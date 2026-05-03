package org.dromara.ai.execution.registry.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 注册源同步结果视图对象
 *
 * @author Mahone
 */
@Data
public class SyncResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 注册源 ID
     */
    private Long sourceId;

    /**
     * 注册源名称
     */
    private String sourceName;

    /**
     * 本次同步条目数量
     */
    private Integer syncCount;

    /**
     * 同步状态（success=成功，failed=失败，running=同步中）
     */
    private String syncStatus;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 错误信息（同步失败时填充）
     */
    private String errorMessage;

}
