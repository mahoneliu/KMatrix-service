package org.dromara.ai.workflow.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 节点连接规则查询业务对象
 *
 * @author Mahone
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KmNodeConnectionRuleQueryBo extends BaseEntity {

    /** 源节点类型（可选过滤） */
    private String sourceNodeType;

    /** 目标节点类型（可选过滤） */
    private String targetNodeType;

    /** 规则类型：0=允许，1=禁止（可选过滤） */
    private String ruleType;

    /** 启用状态：1=启用，0=停用（可选过滤） */
    private String isEnabled;
}
