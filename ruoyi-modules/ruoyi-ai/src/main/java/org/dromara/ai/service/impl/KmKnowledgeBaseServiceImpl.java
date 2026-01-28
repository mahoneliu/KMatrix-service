package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmKnowledgeBase;
import org.dromara.ai.domain.bo.KmKnowledgeBaseBo;
import org.dromara.ai.domain.vo.KmKnowledgeBaseVo;
import org.dromara.ai.mapper.KmKnowledgeBaseMapper;
import org.dromara.ai.service.IKmKnowledgeBaseService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@RequiredArgsConstructor
@Service
public class KmKnowledgeBaseServiceImpl implements IKmKnowledgeBaseService {

    private final KmKnowledgeBaseMapper baseMapper;

    /**
     * 查询知识库
     */
    @Override
    public KmKnowledgeBaseVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public TableDataInfo<KmKnowledgeBaseVo> queryPageList(KmKnowledgeBaseBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = buildQueryWrapper(bo);
        Page<KmKnowledgeBaseVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public List<KmKnowledgeBaseVo> queryList(KmKnowledgeBaseBo bo) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmKnowledgeBase> buildQueryWrapper(KmKnowledgeBaseBo bo) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = new LambdaQueryWrapper<>();
        lqw.like(StringUtils.isNotBlank(bo.getName()), KmKnowledgeBase::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), KmKnowledgeBase::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPermissionLevel()), KmKnowledgeBase::getPermissionLevel,
                bo.getPermissionLevel());
        lqw.orderByDesc(KmKnowledgeBase::getCreateTime);
        return lqw;
    }

    /**
     * 新增知识库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(KmKnowledgeBaseBo bo) {
        KmKnowledgeBase add = MapstructUtils.convert(bo, KmKnowledgeBase.class);
        // 设置所属用户
        add.setOwnerId(LoginHelper.getUserId());
        // 默认状态
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("ACTIVE");
        }
        if (StringUtils.isBlank(add.getPermissionLevel())) {
            add.setPermissionLevel("PRIVATE");
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            return add.getId();
        }
        return null;
    }

    /**
     * 修改知识库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(KmKnowledgeBaseBo bo) {
        KmKnowledgeBase update = MapstructUtils.convert(bo, KmKnowledgeBase.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 校验并批量删除知识库信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO: 校验逻辑，例如知识库下是否存在数据集
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
