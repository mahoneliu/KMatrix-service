package org.dromara.ai.app.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 技能抽象管理对象 km_skill
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_skill", autoResultMap = true)
public class KmSkill extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 技能ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "skill_id")
    private Long skillId;

    /**
     * 技能名称（英文标识，用作 LLM Function name）
     */
    private String skillName;

    /**
     * 技能说明（提供给大模型和用户参考，与 tool.spec 保持一致）
     */
    private String spec;

    /**
     * 绑定的工具配置集合 JSON Array [{type:"builtin",id:1}, ...]
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String toolBindings;

    /**
     * 输入参数 JSON Schema
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String inputSchema;

    /**
     * 输出参数 JSON Schema
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String outputSchema;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志
     */
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
