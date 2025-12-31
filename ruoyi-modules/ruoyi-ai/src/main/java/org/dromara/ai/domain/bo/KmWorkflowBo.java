package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmWorkflow;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotNull;

/**
 * 工作流业务对象 km_workflow
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmWorkflow.class, reverseConvertGenerate = false)
public class KmWorkflowBo extends BaseEntity {

    /**
     * 工作流ID
     */
    private Long flowId;

    /**
     * 所属应用ID
     */
    @NotNull(message = "所属应用不能为空")
    private Long appId;

    /**
     * 前端画布数据(JSON)
     */
    private String graphData;

    /**
     * 后端执行DSL(JSON)
     */
    private String dslData;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否激活
     */
    private String isActive;

    /**
     * 备注
     */
    private String remark;

}
