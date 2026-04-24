package org.dromara.ai.execution.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.execution.domain.KmMcpServer;
import org.dromara.ai.execution.domain.vo.KmMcpServerVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * MCP Server Mapper接口
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Mapper
public interface KmMcpServerMapper extends BaseMapperPlus<KmMcpServer, KmMcpServerVo> {

}
