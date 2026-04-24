package org.dromara.ai.execution.skill.executor;

import org.dromara.ai.execution.core.ExecutionContext;
import org.dromara.ai.execution.core.ToolBinding;

import java.util.List;

/**
 * 技能执行器接口
 *
 * @author KMatrix
 */
public interface SkillExecutor {

    /**
     * 执行技能
     *
     * @param bindings  技能内部的工具绑定列表
     * @param arguments 调用参数（JSON 字符串）
     * @param context   执行上下文
     * @return 执行结果
     */
    SkillResult execute(List<ToolBinding> bindings, String arguments, ExecutionContext context);

    /**
     * 技能执行结果
     */
    record SkillResult(
            boolean success,
            String text,
            String errorMessage,
            long durationMs
    ) {
        public static SkillResult ok(String text, long durationMs) {
            return new SkillResult(true, text, null, durationMs);
        }

        public static SkillResult fail(String errorMessage, long durationMs) {
            return new SkillResult(false, null, errorMessage, durationMs);
        }
    }
}
