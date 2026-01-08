package org.dromara.ai.domain.query;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 节点定义查询对象
 *
 * @author Mahone
 * @date 2026-01-08
 */
@Data
public class KmNodeDefinitionQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点类型标识
     */
    private String nodeType;

    /**
     * 节点显示名称
     */
    private String nodeLabel;

    /**
     * 节点分类 (basic/ai/logic/action)
     */
    private String category;

    /**
     * 是否系统节点 (0否/1是)
     */
    private String isSystem;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

}
