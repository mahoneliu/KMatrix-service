package org.dromara.ai.execution.skill.executor;

import org.dromara.ai.execution.core.ExecutionContext;
import org.dromara.ai.execution.core.ToolBinding;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 顺序技能执行器
 * <p>
 * 按绑定顺序逐个执行工具，汇总所有结果。
 *
 * @author KMatrix
 */
@Slf4j
public class SequentialSkillExecutor implements SkillExecutor {

    @Override
    public SkillResult execute(List<ToolBinding> bindings, String arguments, ExecutionContext context) {
        long startMs = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();

        for (ToolBinding binding : bindings) {
            try {
                log.info("顺序执行技能内工具: {}, 参数: {}", binding.getToolName(), arguments);
                String result = binding.getExecutor().execute(arguments);
                sb.append("[").append(binding.getToolName()).append("]: ")
                        .append(result).append("\n");
            } catch (Exception ex) {
                sb.append("[").append(binding.getToolName()).append(" 执行失败]: ")
                        .append(ex.getMessage()).append("\n");
                log.error("技能内工具执行失败: toolName={}", binding.getToolName(), ex);
            }
        }

        long duration = System.currentTimeMillis() - startMs;
        return SkillResult.ok(sb.toString().trim(), duration);
    }
}
