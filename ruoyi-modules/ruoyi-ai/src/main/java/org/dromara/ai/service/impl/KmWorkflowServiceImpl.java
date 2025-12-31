package org.dromara.ai.service.impl;

import org.dromara.common.core.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmWorkflow;
import org.dromara.ai.domain.bo.KmWorkflowBo;
import org.dromara.ai.domain.vo.KmWorkflowVo;
import org.dromara.ai.mapper.KmWorkflowMapper;
import org.dromara.ai.service.IKmWorkflowService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 工作流定义Service业务层处理
 *
 * @author Mahone
 * @date 2025-12-27
 */
@RequiredArgsConstructor
@Service
public class KmWorkflowServiceImpl implements IKmWorkflowService {

    private final KmWorkflowMapper baseMapper;

    /**
     * 查询工作流定义
     */
    @Override
    public KmWorkflowVo queryById(Long flowId) {
        return baseMapper.selectVoById(flowId);
    }

    /**
     * 查询工作流定义列表
     */
    @Override
    public TableDataInfo<KmWorkflowVo> queryPageList(KmWorkflowBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmWorkflow> lqw = buildQueryWrapper(bo);
        Page<KmWorkflowVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询工作流定义列表
     */
    @Override
    public List<KmWorkflowVo> queryList(KmWorkflowBo bo) {
        LambdaQueryWrapper<KmWorkflow> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmWorkflow> buildQueryWrapper(KmWorkflowBo bo) {
        LambdaQueryWrapper<KmWorkflow> lqw = new LambdaQueryWrapper<>();
        lqw.eq(bo.getAppId() != null, KmWorkflow::getAppId, bo.getAppId());
        lqw.like(StringUtils.isNotBlank(bo.getRemark()), KmWorkflow::getRemark, bo.getRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getIsActive()), KmWorkflow::getIsActive, bo.getIsActive());
        return lqw;
    }

    /**
     * 新增工作流定义
     */
    @Override
    public Boolean insertByBo(KmWorkflowBo bo) {
        KmWorkflow add = MapstructUtils.convert(bo, KmWorkflow.class);
        add.setFlowId(null);
        return baseMapper.insert(add) > 0;
    }

    /**
     * 修改工作流定义
     */
    @Override
    public Boolean updateByBo(KmWorkflowBo bo) {
        KmWorkflow update = MapstructUtils.convert(bo, KmWorkflow.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 校验并批量删除工作流定义信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO: 校验逻辑
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
