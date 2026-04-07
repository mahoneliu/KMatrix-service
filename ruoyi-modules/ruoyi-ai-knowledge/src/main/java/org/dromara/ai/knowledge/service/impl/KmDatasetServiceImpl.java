package org.dromara.ai.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.bo.KmDatasetBo;
import org.dromara.ai.knowledge.domain.vo.KmDatasetVo;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.service.IKmDatasetService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据集Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@RequiredArgsConstructor
@Service
public class KmDatasetServiceImpl implements IKmDatasetService {

    private final KmDatasetMapper baseMapper;

    /**
     * 查询数据集
     */
    @Override
    public KmDatasetVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询数据集列表
     */
    @Override
    public TableDataInfo<KmDatasetVo> queryPageList(KmDatasetBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmDataset> lqw = buildQueryWrapper(bo);
        Page<KmDatasetVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库下的数据集列表
     */
    @Override
    public List<KmDatasetVo> queryListByKbId(Long kbId) {
        LambdaQueryWrapper<KmDataset> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KmDataset::getKbId, kbId);
        lqw.orderByAsc(KmDataset::getCreateTime);
        return baseMapper.selectVoListWithStats(lqw);
    }

    private LambdaQueryWrapper<KmDataset> buildQueryWrapper(KmDatasetBo bo) {
        LambdaQueryWrapper<KmDataset> lqw = new LambdaQueryWrapper<>();
        lqw.eq(bo.getKbId() != null, KmDataset::getKbId, bo.getKbId());
        lqw.like(bo.getName() != null, KmDataset::getName, bo.getName());
        lqw.orderByDesc(KmDataset::getCreateTime);
        return lqw;
    }

    /**
     * 新增数据集
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(KmDatasetBo bo) {
        KmDataset add = MapstructUtils.convert(bo, KmDataset.class);
        add.setConfig(buildConfig(bo));
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            return add.getId();
        }
        return null;
    }

    /**
     * 修改数据集
     * 注意：MyBatis-Plus updateById 对带 typeHandler 的字段不可靠，
     * 改用专用 Mapper 方法显式更新 config 列
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(KmDatasetBo bo) {
        KmDataset update = MapstructUtils.convert(bo, KmDataset.class);
        // 先更新普通字段（config 设为 null 避免 updateById 误操作）
        update.setConfig(null);
        baseMapper.updateById(update);
        // 再单独更新 config，确保 typeHandler 正确处理
        Map<String, Object> newConfig = buildConfig(bo);
        baseMapper.updateConfig(bo.getId(), newConfig);
        return true;
    }

    /**
     * 根据 Bo 构建 config Map：
     * 保留现有 config 中其他字段，合并 workflowId / maxConcurrency
     */
    private Map<String, Object> buildConfig(KmDatasetBo bo) {
        Map<String, Object> config = bo.getConfig() != null
                ? new HashMap<>(bo.getConfig())
                : new HashMap<>();
        // 统一使用 appId key（调度器读取 config.appId）
        config.remove("workflowId");
        if (bo.getWorkflowId() != null) {
            config.put("appId", bo.getWorkflowId());
        } else {
            config.remove("appId");
        }
        if (bo.getMaxConcurrency() != null) {
            config.put("maxConcurrency", bo.getMaxConcurrency());
        } else {
            config.remove("maxConcurrency");
        }
        return config;
    }

    /**
     * 校验并批量删除数据集信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 检查是否包含系统预设数据集
            for (Long id : ids) {
                KmDataset dataset = baseMapper.selectById(id);
                if (dataset != null && Boolean.TRUE.equals(dataset.getIsSystem())) {
                    throw new RuntimeException("系统预设数据集不可删除: " + dataset.getName());
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
