package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmNodeExecution;
import org.dromara.ai.domain.enums.NodeExecutionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 节点执行记录业务对象 km_node_execution
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Data
@AutoMapper(target = KmNodeExecution.class, reverseConvertGenerate = false)
public class KmNodeExecutionBo {

    /**
     * 执行ID
     */
    private Long executionId;

    /**
     * 实例ID
     */
    @NotNull(message = "实例ID不能为空")
    private Long instanceId;

    /**
     * 节点ID
     */
    @NotBlank(message = "节点ID不能为空")
    private String nodeId;

    /**
     * 节点类型
     */
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    /**
     * 执行状态
     */
    private NodeExecutionStatus status;

    /**
     * 输入参数
     */
    private Map<String, Object> inputParams;

    /**
     * 输出参数
     */
    private Map<String, Object> outputParams;

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
