package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmWorkflow;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流视图对象 km_workflow
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@AutoMapper(target = KmWorkflow.class)
public class KmWorkflowVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    private Long flowId;

    /**
     * 所属应用ID
     */
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
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 备注
     */
    private String remark;

}
