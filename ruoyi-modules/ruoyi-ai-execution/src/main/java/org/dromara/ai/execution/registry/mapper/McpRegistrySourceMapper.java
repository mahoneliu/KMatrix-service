package org.dromara.ai.execution.registry.mapper;

import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;
import org.dromara.ai.execution.registry.domain.vo.McpRegistrySourceVO;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * MCP 注册源配置 Mapper 接口
 *
 * @author Mahone
 */
public interface McpRegistrySourceMapper extends BaseMapperPlus<KmMcpRegistrySource, McpRegistrySourceVO> {

    /**
     * 查询所有启用状态的注册源（is_enabled='1' 且 del_flag='0'）
     *
     * @return 启用的注册源列表
     */
    List<KmMcpRegistrySource> selectEnabledSources();

}
