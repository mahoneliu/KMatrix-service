package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmWorkflowTemplate;
import org.dromara.ai.domain.bo.KmWorkflowTemplateBo;
import org.dromara.ai.domain.vo.KmWorkflowTemplateVo;
import org.dromara.ai.mapper.KmWorkflowTemplateMapper;
import org.dromara.ai.service.IKmWorkflowTemplateService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 工作流模板Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-26
 */
@RequiredArgsConstructor
@Service
public class KmWorkflowTemplateServiceImpl implements IKmWorkflowTemplateService {

    private final KmWorkflowTemplateMapper baseMapper;

    /**
     * 查询工作流模板
     */
    @Override
    public KmWorkflowTemplateVo queryById(Long templateId) {
        return baseMapper.selectVoById(templateId);
    }

    /**
     * 查询工作流模板列表
     */
    @Override
    public TableDataInfo<KmWorkflowTemplateVo> queryPageList(KmWorkflowTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = buildQueryWrapper(bo);
        Page<KmWorkflowTemplateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询工作流模板列表
     */
    @Override
    public List<KmWorkflowTemplateVo> queryList(KmWorkflowTemplateBo bo) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmWorkflowTemplate> buildQueryWrapper(KmWorkflowTemplateBo bo) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = new LambdaQueryWrapper<>();
        lqw.like(StringUtils.isNotBlank(bo.getTemplateName()), KmWorkflowTemplate::getTemplateName,
                bo.getTemplateName());
        lqw.eq(StringUtils.isNotBlank(bo.getTemplateCode()), KmWorkflowTemplate::getTemplateCode, bo.getTemplateCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), KmWorkflowTemplate::getCategory, bo.getCategory());
        lqw.eq(StringUtils.isNotBlank(bo.getScopeType()), KmWorkflowTemplate::getScopeType, bo.getScopeType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsEnabled()), KmWorkflowTemplate::getIsEnabled, bo.getIsEnabled());
        return lqw;
    }

    /**
     * 新增工作流模板
     */
    @Override
    public Boolean insertByBo(KmWorkflowTemplateBo bo) {
        KmWorkflowTemplate add = MapstructUtils.convert(bo, KmWorkflowTemplate.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setTemplateId(add.getTemplateId());
        }
        return flag;
    }

    /**
     * 修改工作流模板
     */
    @Override
    public Boolean updateByBo(KmWorkflowTemplateBo bo) {
        KmWorkflowTemplate update = MapstructUtils.convert(bo, KmWorkflowTemplate.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KmWorkflowTemplate entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除工作流模板信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
