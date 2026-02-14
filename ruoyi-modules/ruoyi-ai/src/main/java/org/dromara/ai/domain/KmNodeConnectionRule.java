package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 节点连接规则对象 km_node_connection_rule
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_node_connection_rule")
public class KmNodeConnectionRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId
    private Long ruleId;

    /**
     * 源节点类型
     */
    private String sourceNodeType;

    /**
     * 目标节点类型
     */
    private String targetNodeType;

    /**
     * 规则类型 (0允许连接/1禁止连接)
     */
    private String ruleType;

    /**
     * 优先级 (数值越大优先级越高)
     */
    private Integer priority;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

}
