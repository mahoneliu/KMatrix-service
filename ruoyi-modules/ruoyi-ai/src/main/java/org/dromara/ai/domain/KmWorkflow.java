package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流定义对象 km_workflow
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@TableName("km_workflow")
public class KmWorkflow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 是否激活（1是 0否）
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
