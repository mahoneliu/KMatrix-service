package org.dromara.ai.workflow.workflow.nodes.tool;

import dev.langchain4j.data.message.Content;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 工具执行结果，支持混合内容（文本 + 媒体）
 *
 * @deprecated 已迁移至 {@link org.dromara.ai.execution.core.ToolResult}
 * @author KMatrix
 */
@Deprecated
@Getter
@Builder
public class ToolResult {

    /**
     * 纯文本结果（兼容旧版）
     */
    private final String text;

    /**
     * 富媒体内容列表（文本+图片+文件等混合）
     */
    private final List<Content> contents;

    /**
     * 快速创建纯文本结果
     */
    public static ToolResult ofText(String text) {
        return ToolResult.builder().text(text).build();
    }

    /**
     * 快速创建富媒体结果
     */
    public static ToolResult ofContents(String text, List<Content> contents) {
        return ToolResult.builder().text(text).contents(contents).build();
    }

    /**
     * 是否包含富媒体内容
     */
    public boolean hasContents() {
        return contents != null && !contents.isEmpty();
    }
}
