package org.dromara.ai.execution.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.execution.domain.KmBuiltinTool;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 内置 Python 工具业务对象 km_builtin_tool
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmBuiltinTool.class, reverseConvertGenerate = false)
public class KmBuiltinToolBo extends BaseEntity {

    /**
     * 工具 ID
     */
    private Long toolId;

    /**
     * 工具名称（英文标识，用作 LLM Tool function name）
     */
    @NotBlank(message = "工具名称不能为空")
    @Size(max = 64, message = "工具名称长度不能超过64个字符")
    private String toolName;

    /**
     * 工具描述（提供给 LLM 的功能说明）
     */
    @Size(max = 128, message = "描述长度不能超过128个字符")
    private String spec;

    /**
     * 启动参数定义（JSON Array）
     */
    private String initParams;

    /**
     * 输入参数 JSON Schema（供 LLM 解析字段）
     */
    private String inputSchema;

    /**
     * 输出参数 JSON Schema（供工作流动态展开出参）
     */
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
     * 备注
     */
    private String remark;

}
