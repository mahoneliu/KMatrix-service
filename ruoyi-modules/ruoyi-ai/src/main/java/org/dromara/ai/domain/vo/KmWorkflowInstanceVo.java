package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmWorkflowInstance;
import org.dromara.ai.domain.enums.WorkflowInstanceStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 工作流实例视图对象 km_workflow_instance
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
@AutoMapper(target = KmWorkflowInstance.class)
public class KmWorkflowInstanceVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private Long instanceId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 工作流配置快照(JSON)
     */
    private String workflowConfig;

    /**
     * 实例状态
     */
    private WorkflowInstanceStatus status;

    /**
     * 当前执行节点ID
     */
    private String currentNode;

    /**
     * 全局状态数据
     */
    private Map<String, Object> globalState;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

}
