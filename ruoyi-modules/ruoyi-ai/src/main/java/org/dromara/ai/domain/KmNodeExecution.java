package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.dromara.ai.domain.enums.NodeExecutionStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 节点执行记录对象 km_node_execution
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
@TableName(value = "km_node_execution")
public class KmNodeExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long executionId;

    /**
     * 实例ID
     */
    private Long instanceId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态
     */
    private NodeExecutionStatus status;

    /**
     * 输入参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputParams;

    /**
     * 输出参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputParams;

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

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 输入token数
     */
    private Integer inputTokens;

    /**
     * 输出token数
     */
    private Integer outputTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 执行耗时(毫秒)
     */
    private Long durationMs;
}
