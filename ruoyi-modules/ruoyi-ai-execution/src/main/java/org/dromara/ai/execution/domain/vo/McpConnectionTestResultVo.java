package org.dromara.ai.execution.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * MCP 连接测试结果视图对象
 *
 * @author Kiro
 */
@Data
public class McpConnectionTestResultVo {

    /**
     * 是否连接成功
     */
    private boolean success;

    /**
     * 工具列表（连接成功时返回）
     */
    private List<McpToolVo> tools;

    /**
     * 错误信息（连接失败时返回）
     */
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    private long elapsedMs;

}
