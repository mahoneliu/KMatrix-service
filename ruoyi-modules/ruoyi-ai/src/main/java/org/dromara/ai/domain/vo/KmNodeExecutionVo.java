package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.dromara.ai.domain.KmNodeExecution;
import org.dromara.ai.domain.enums.NodeExecutionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 节点执行记录视图对象
 *
 * @author Mahone
 * @date 2026-01-03
 */
@Data
@AutoMapper(target = KmNodeExecution.class)
public class KmNodeExecutionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行ID
     */
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
     * 节点名称 (由业务逻辑填充)
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态
     */
    private NodeExecutionStatus status;

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
     * 输入参数
     */
    @JsonProperty("inputs")
    private Map<String, Object> inputParams;

    /**
     * 输出参数
     */
    @JsonProperty("outputs")
    private Map<String, Object> outputParams;

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

    /**
     * 获取token使用统计(前端兼容格式)
     * 前端期望的格式: { inputTokenCount, outputTokenCount, totalTokenCount }
     */
    public Map<String, Object> getTokenUsage() {
        // 只有当至少有一个token字段不为null时才返回tokenUsage对象
        if (inputTokens == null && outputTokens == null && totalTokens == null) {
            return null;
        }

        Map<String, Object> tokenUsage = new HashMap<>();
        if (inputTokens != null) {
            tokenUsage.put("inputTokenCount", inputTokens);
        }
        if (outputTokens != null) {
            tokenUsage.put("outputTokenCount", outputTokens);
        }
        if (totalTokens != null) {
            tokenUsage.put("totalTokenCount", totalTokens);
        }
        return tokenUsage;
    }
}
