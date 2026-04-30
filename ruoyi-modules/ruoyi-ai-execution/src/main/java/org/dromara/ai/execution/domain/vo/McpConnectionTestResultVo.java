package org.dromara.ai.execution.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 连接测试结果 VO
 *
 * @author Mahone
 * @date 2026-04-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpConnectionTestResultVo {

    /** 是否连接成功 */
    private boolean success;

    /** 发现的工具列表 */
    private List<McpToolVo> tools;

    /** 错误信息（失败时填充） */
    private String errorMessage;

    /** 耗时（毫秒） */
    private long elapsedMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpToolVo {
        private String name;
        private String description;
    }
}
