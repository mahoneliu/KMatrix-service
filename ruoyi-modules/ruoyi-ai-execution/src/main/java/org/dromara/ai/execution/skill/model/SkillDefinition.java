package org.dromara.ai.execution.skill.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 技能定义模型
 * <p>
 * 描述技能的编排方式、执行模式和内部工具绑定。
 *
 * @author KMatrix
 */
@Data
@Builder
public class SkillDefinition {

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能说明
     */
    private String spec;

    /**
     * 输入参数 JSON Schema
     */
    private String inputSchema;

    /**
     * 内部工具引用列表
     */
    private List<ToolRef> toolRefs;

    /**
     * 执行模式
     */
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;

    /**
     * 工具引用
     */
    @Data
    @Builder
    public static class ToolRef {
        private String type;
        private Long id;
    }

    /**
     * 执行模式枚举
     */
    public enum ExecutionMode {
        /** 顺序执行 */
        SEQUENTIAL,
        /** 并行执行 */
        PARALLEL,
        /** 条件执行（根据上一步结果决定是否继续） */
        CONDITIONAL
    }
}
