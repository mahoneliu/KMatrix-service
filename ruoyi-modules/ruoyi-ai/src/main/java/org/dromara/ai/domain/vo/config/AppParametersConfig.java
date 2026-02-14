package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

/**
 * 应用参数配置
 *
 * @author Mahone
 * @date 2026-01-14
 */
@Data
public class AppParametersConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ParamDefinition> appParams;

    /**
     * 接口参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ParamDefinition> interfaceParams;

    /**
     * 会话参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ParamDefinition> sessionParams;
}
