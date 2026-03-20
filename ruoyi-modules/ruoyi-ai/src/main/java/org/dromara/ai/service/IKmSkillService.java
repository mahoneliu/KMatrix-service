package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmSkillBo;
import org.dromara.ai.domain.vo.KmSkillVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 技能管理Service接口
 *
 * @author KMatrix
 * @date 2026-03-21
 */
public interface IKmSkillService {

    /**
     * 查询技能管理
     */
    KmSkillVo queryById(Long skillId);

    /**
     * 查询技能管理列表
     */
    TableDataInfo<KmSkillVo> queryPageList(KmSkillBo bo, PageQuery pageQuery);

    /**
     * 查询所有技能管理列表
     */
    List<KmSkillVo> queryList(KmSkillBo bo);

    /**
     * 新增技能管理
     */
    Boolean insertByBo(KmSkillBo bo);

    /**
     * 修改技能管理
     */
    Boolean updateByBo(KmSkillBo bo);

    /**
     * 校验并批量删除技能管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
