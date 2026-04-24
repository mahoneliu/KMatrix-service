package org.dromara.ai.execution.core;

import dev.langchain4j.data.message.Content;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 工具执行结果，支持混合内容（文本 + 媒体）
 * <p>
 * 工具可以返回纯文本字符串，也可以返回包含图片/文件等富媒体内容的 Content 列表。
 * 如果 {@code contents} 非空，将优先使用 contents 传给大模型；
 * 否则使用 {@code text} 作为纯文本结果。
 * </p>
 *
 * @author KMatrix
 */
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
     * 执行是否成功
     */
    @Builder.Default
    private final boolean success = true;

    /**
     * 错误信息（当 success=false 时有效）
     */
    private final String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private final Long durationMs;

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
     * 快速创建错误结果
     */
    public static ToolResult ofError(String errorMessage) {
        return ToolResult.builder().success(false).errorMessage(errorMessage).build();
    }

    /**
     * 是否包含富媒体内容
     */
    public boolean hasContents() {
        return contents != null && !contents.isEmpty();
    }
}
