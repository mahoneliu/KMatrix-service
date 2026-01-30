package org.dromara.ai.workflow.condition;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 变量引用
 * 用于指定条件表达式中的变量来源
 *
 * @author Mahone
 * @date 2026-01-19
 */
@Data
public class VariableRef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 来源类型: global / node
     */
    private String sourceType;

    /**
     * 来源键
     * - 当 sourceType=global 时: app / interface / session
     * - 当 sourceType=node 时: 节点ID
     */
    private String sourceKey;

    /**
     * 参数键
     */
    private String sourceParam;
}
