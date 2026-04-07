package org.dromara.ai.workflow.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.workflow.domain.KmNodeConnectionRule;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 节点连接规则业务对象（新增/编辑）
 *
 * @author Mahone
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmNodeConnectionRule.class, reverseConvertGenerate = false)
public class KmNodeConnectionRuleBo extends BaseEntity {

    /** 规则ID（编辑时必填） */
    private Long ruleId;

    /** 源节点类型 */
    @NotBlank(message = "源节点类型不能为空")
    private String sourceNodeType;

    /** 目标节点类型 */
    @NotBlank(message = "目标节点类型不能为空")
    private String targetNodeType;

    /** 规则类型：0=允许，1=禁止 */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /** 优先级，范围 0~999，默认 10 */
    @Min(value = 0, message = "优先级最小值为 0")
    @Max(value = 999, message = "优先级最大值为 999")
    private Integer priority;

    /** 启用状态：1=启用，0=停用 */
    private String isEnabled;

    /** 备注 */
    private String remark;
}
