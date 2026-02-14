package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import org.dromara.ai.domain.enums.WorkflowInstanceStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 工作流实例对象 km_workflow_instance
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
@TableName(value = "km_workflow_instance", autoResultMap = true)
public class KmWorkflowInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(typeHandler = JsonTypeHandler.class)
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
    @TableField(typeHandler = JsonTypeHandler.class)
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
