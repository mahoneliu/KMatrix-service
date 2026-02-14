package org.dromara.ai.workflow.nodes.condition;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 条件分支
 * 表示一个 IF / ELSE IF 分支
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Data
public class ConditionBranch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分支名称（用于显示和输出）
     */
    private String name;

    /**
     * 分支条件配置
     */
    private ConditionGroup condition;

    /**
     * Handle ID (前端自动生成，用于连线)
     */
    private String handleId;
}
