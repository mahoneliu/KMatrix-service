package org.dromara.ai.service.impl;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.ai.domain.bo.KmDataSourceBo;
import org.dromara.ai.domain.vo.KmDataSourceVo;
import org.dromara.ai.mapper.KmDataSourceMapper;
import org.dromara.ai.service.IKmDataSourceService;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据源配置Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmDataSourceServiceImpl implements IKmDataSourceService {

    private final KmDataSourceMapper baseMapper;
    // 注入dynamic-datasource的数据源信息(可选)
    private final Map<String, DataSource> dataSourceMap;

    @Override
    public List<KmDataSourceVo> queryList(KmDataSourceBo bo) {
        LambdaQueryWrapper<KmDataSource> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getDataSourceName()), KmDataSource::getDataSourceName, bo.getDataSourceName());
        lqw.eq(StrUtil.isNotBlank(bo.getSourceType()), KmDataSource::getSourceType, bo.getSourceType());
        lqw.eq(StrUtil.isNotBlank(bo.getIsEnabled()), KmDataSource::getIsEnabled, bo.getIsEnabled());
        lqw.orderByDesc(KmDataSource::getCreateTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public KmDataSourceVo queryById(Long dataSourceId) {
        return baseMapper.selectVoById(dataSourceId);
    }

    @Override
    public Boolean insertByBo(KmDataSourceBo bo) {
        KmDataSource add = MapstructUtils.convert(bo, KmDataSource.class);
        // TODO: 密码加密处理
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmDataSourceBo bo) {
        KmDataSource update = MapstructUtils.convert(bo, KmDataSource.class);
        // TODO: 密码加密处理
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean testConnection(Long dataSourceId) {
        KmDataSource ds = baseMapper.selectById(dataSourceId);
        if (ds == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.datasource.not_found"));
        }

        if ("DYNAMIC".equals(ds.getSourceType())) {
            // 使用dynamic-datasource的数据源
            DataSource dataSource = dataSourceMap.get(ds.getDsKey());
            if (dataSource == null) {
                throw new RuntimeException("动态数据源不存在: " + ds.getDsKey());
            }
            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(5);
            } catch (Exception e) {
                log.error("数据源连接测试失败", e);
                throw new RuntimeException("连接失败: " + e.getMessage());
            }
        } else {
            // 手工录入的数据源
            try {
                Class.forName(ds.getDriverClassName());
                try (Connection conn = DriverManager.getConnection(
                        ds.getJdbcUrl(), ds.getUsername(), ds.getPassword())) {
                    return conn.isValid(5);
                }
            } catch (Exception e) {
                log.error("数据源连接测试失败", e);
                throw new RuntimeException("连接失败: " + e.getMessage());
            }
        }
    }

    @Override
    public List<String> getDynamicDataSourceKeys() {
        // 返回dynamic-datasource配置的所有数据源key
        return new ArrayList<>(dataSourceMap.keySet());
    }

}
