package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.bo.KmModelProviderBo;
import org.dromara.ai.domain.vo.KmModelProviderVo;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.service.IKmModelProviderService;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型供应商Service业务层处理
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmModelProviderServiceImpl implements IKmModelProviderService {

    private final KmModelProviderMapper baseMapper;

    @Override
    public List<KmModelProviderVo> queryList(KmModelProviderBo bo) {
        LambdaQueryWrapper<KmModelProvider> lqw = Wrappers.lambdaQuery();
        // 按排序字段升序排列
        lqw.orderByAsc(KmModelProvider::getSort);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public KmModelProviderVo queryById(Long providerId) {
        return baseMapper.selectVoById(providerId);
    }

    @Override
    public Boolean updateByBo(KmModelProviderBo bo) {
        KmModelProvider provider = MapstructUtils.convert(bo, KmModelProvider.class);
        return baseMapper.updateById(provider) > 0;
    }
}
