package org.dromara.ai.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

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
     * 全局参数
     */
    private List<ParamDefinition> globalParams;

    /**
     * 接口参数
     */
    private List<ParamDefinition> interfaceParams;

    /**
     * 会话参数
     */
    private List<ParamDefinition> sessionParams;
}
