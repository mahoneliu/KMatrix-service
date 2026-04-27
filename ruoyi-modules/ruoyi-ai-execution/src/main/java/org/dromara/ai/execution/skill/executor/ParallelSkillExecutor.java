package org.dromara.ai.execution.skill.executor;

import org.dromara.ai.execution.core.ExecutionContext;
import org.dromara.ai.execution.core.ToolBinding;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 并行技能执行器
 * <p>
 * 使用线程池并行执行所有工具，汇总结果。
 *
 * @author KMatrix
 */
@Slf4j
public class ParallelSkillExecutor implements SkillExecutor {

    private final ExecutorService executorService;

    public ParallelSkillExecutor() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public ParallelSkillExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public SkillResult execute(List<ToolBinding> bindings, String arguments, ExecutionContext context) {
        long startMs = System.currentTimeMillis();

        List<Future<String>> futures = new ArrayList<>();
        for (ToolBinding binding : bindings) {
            futures.add(executorService.submit(() -> {
                try {
                    log.info("并行执行技能内工具: {}", binding.getToolName());
                    return "[" + binding.getToolName() + "]: " + binding.getExecutor().execute(arguments);
                } catch (Exception ex) {
                    log.error("技能内工具执行失败: toolName={}", binding.getToolName(), ex);
                    return "[" + binding.getToolName() + " 执行失败]: " + ex.getMessage();
                }
            }));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(60, TimeUnit.SECONDS);
                sb.append(result).append("\n");
            } catch (TimeoutException e) {
                String toolName = bindings.get(i).getToolName();
                sb.append("[").append(toolName).append(" 执行超时]\n");
                log.warn("技能内工具执行超时: toolName={}", toolName);
            } catch (Exception e) {
                String toolName = bindings.get(i).getToolName();
                sb.append("[").append(toolName).append(" 执行异常]: ").append(e.getMessage()).append("\n");
                log.error("技能内工具执行异常: toolName={}", toolName, e);
            }
        }

        long duration = System.currentTimeMillis() - startMs;
        return SkillResult.ok(sb.toString().trim(), duration);
    }
}
