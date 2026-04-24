package org.dromara.ai.execution.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内置 Python 工具对象 km_builtin_tool
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_builtin_tool", autoResultMap = true)
public class KmBuiltinTool extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工具 ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "tool_id")
    private Long toolId;

    /**
     * 工具名称（英文标识，用作 LLM Tool function name）
     */
    private String toolName;

    /**
     * 工具描述（提供给 LLM 的说明）
     */
    private String spec;

    /**
     * 启动参数定义（JSON Array）
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String initParams;

    /**
     * 输入参数 JSON Schema（供 LLM 解析字段）
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String inputSchema;

    /**
     * 输出参数 JSON Schema（供工作流动态展开出参）
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private String outputSchema;

    /**
     * Python 脚本内容
     */
    private String pythonCode;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志
     */
    private String delFlag;

}
