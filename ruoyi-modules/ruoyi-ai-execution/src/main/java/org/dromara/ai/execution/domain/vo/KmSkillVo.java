package org.dromara.ai.execution.domain.vo;

import org.dromara.ai.execution.domain.KmSkill;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 技能管理视图对象 km_skill
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Data
@AutoMapper(target = KmSkill.class)
public class KmSkillVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能说明（提供给大模型参考）
     */
    private String spec;

    /**
     * 绑定的工具配置集合 JSON
     */
    private String toolBindings;

    /**
     * 输入参数 JSON Schema
     */
    private String inputSchema;

    /**
     * 输出参数 JSON Schema
     */
    private String outputSchema;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 备注
     */
    private String remark;

}
