package org.dromara.ai.api.domain.vo.config;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 应用参数配置（全局/接口/会话）
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
    private List<ParamDefinition> appParams;

    /**
     * 接口参数
     */
    private List<ParamDefinition> interfaceParams;

    /**
     * 会话参数
     */
    private List<ParamDefinition> sessionParams;
}
