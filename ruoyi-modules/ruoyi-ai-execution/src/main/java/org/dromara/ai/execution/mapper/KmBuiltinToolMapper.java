package org.dromara.ai.execution.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.execution.domain.KmBuiltinTool;
import org.dromara.ai.execution.domain.vo.KmBuiltinToolVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 内置 Python 工具 Mapper接口
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Mapper
public interface KmBuiltinToolMapper extends BaseMapperPlus<KmBuiltinTool, KmBuiltinToolVo> {

}
