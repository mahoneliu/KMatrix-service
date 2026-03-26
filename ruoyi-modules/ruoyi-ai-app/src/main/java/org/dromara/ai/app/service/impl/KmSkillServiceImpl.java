package org.dromara.ai.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.app.domain.KmSkill;
import org.dromara.ai.app.domain.bo.KmSkillBo;
import org.dromara.ai.app.domain.vo.KmSkillVo;
import org.dromara.ai.app.mapper.KmSkillMapper;
import org.dromara.ai.app.service.IKmSkillService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 技能管理Service业务层处理
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@RequiredArgsConstructor
@Service
public class KmSkillServiceImpl implements IKmSkillService {

    private final KmSkillMapper baseMapper;

    /**
     * 查询技能管理
     */
    @Override
    public KmSkillVo queryById(Long skillId) {
        return baseMapper.selectVoById(skillId);
    }

    /**
     * 查询技能管理列表
     */
    @Override
    public TableDataInfo<KmSkillVo> queryPageList(KmSkillBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmSkill> lqw = buildQueryWrapper(bo);
        Page<KmSkillVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询所有技能管理列表
     */
    @Override
    public List<KmSkillVo> queryList(KmSkillBo bo) {
        LambdaQueryWrapper<KmSkill> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmSkill> buildQueryWrapper(KmSkillBo bo) {
        LambdaQueryWrapper<KmSkill> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getSkillName()), KmSkill::getSkillName, bo.getSkillName());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), KmSkill::getStatus, bo.getStatus());
        return lqw;
    }

    /**
     * 新增技能管理
     */
    @Override
    public Boolean insertByBo(KmSkillBo bo) {
        KmSkill add = MapstructUtils.convert(bo, KmSkill.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSkillId(add.getSkillId());
        }
        return flag;
    }

    /**
     * 修改技能管理
     */
    @Override
    public Boolean updateByBo(KmSkillBo bo) {
        KmSkill update = MapstructUtils.convert(bo, KmSkill.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KmSkill entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除技能管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
