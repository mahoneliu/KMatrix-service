package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmBuiltinTool;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 内置 Python 工具视图对象 km_builtin_tool
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Data
@AutoMapper(target = KmBuiltinTool.class)
public class KmBuiltinToolVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具 ID
     */
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String spec;

    /**
     * 启动参数定义（JSON Array）
     */
    private String initParams;

    /**
     * 输入参数 JSON Schema
     */
    private String inputSchema;

    /**
     * 输出参数 JSON Schema
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新时间
     */
    private Date updateTime;

}
