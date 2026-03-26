package org.dromara.ai.app.domain.bo;

import org.dromara.ai.app.domain.KmSkill;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 技能管理业务对象 km_skill
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmSkill.class, reverseConvertGenerate = false)
public class KmSkillBo extends BaseEntity {

    /**
     * 技能ID
     */
    @NotNull(message = "技能ID不能为空", groups = { EditGroup.class })
    private Long skillId;

    /**
     * 技能名称
     */
    @NotBlank(message = "技能名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String skillName;

    /**
     * 技能说明（提供给大模型参考）
     */
    private String spec;

    /**
     * 绑定的工具配置集合 JSON string
     */
    private String toolBindings;

    /**
     * 输入参数 JSON Schema string
     */
    private String inputSchema;

    /**
     * 输出参数 JSON Schema string
     */
    private String outputSchema;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

}
