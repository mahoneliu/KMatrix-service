package org.dromara.ai.execution.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.execution.domain.KmSkill;
import org.dromara.ai.execution.domain.vo.KmSkillVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 技能管理Mapper接口
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Mapper
public interface KmSkillMapper extends BaseMapperPlus<KmSkill, KmSkillVo> {

}
