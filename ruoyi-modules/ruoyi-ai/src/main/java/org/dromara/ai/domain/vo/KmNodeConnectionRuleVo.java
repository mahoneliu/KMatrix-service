package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmNodeConnectionRule;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 节点连接规则视图对象 km_node_connection_rule
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Data
@AutoMapper(target = KmNodeConnectionRule.class)
public class KmNodeConnectionRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
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

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    private String updateByName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
