package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.ai.domain.KmDatabaseMeta;
import org.dromara.ai.domain.bo.KmDatabaseMetaBo;
import org.dromara.ai.domain.vo.KmDatabaseMetaVo;
import org.dromara.ai.mapper.KmDataSourceMapper;
import org.dromara.ai.mapper.KmDatabaseMetaMapper;
import org.dromara.ai.service.IKmDatabaseMetaService;
import org.dromara.ai.util.DdlParser;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库元数据Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmDatabaseMetaServiceImpl implements IKmDatabaseMetaService {

    private final KmDatabaseMetaMapper baseMapper;
    private final KmDataSourceMapper dataSourceMapper;
    private final Map<String, DataSource> dataSourceMap;

    @Override
    public List<KmDatabaseMetaVo> queryList(KmDatabaseMetaBo bo) {
        LambdaQueryWrapper<KmDatabaseMeta> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getDataSourceId() != null, KmDatabaseMeta::getDataSourceId, bo.getDataSourceId());
        lqw.like(StrUtil.isNotBlank(bo.getTableName()), KmDatabaseMeta::getTableName, bo.getTableName());
        lqw.orderByAsc(KmDatabaseMeta::getTableName);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public KmDatabaseMetaVo queryById(Long metaId) {
        return baseMapper.selectVoById(metaId);
    }

    @Override
    public List<KmDatabaseMetaVo> queryByDataSourceId(Long dataSourceId) {
        LambdaQueryWrapper<KmDatabaseMeta> lqw = Wrappers.lambdaQuery();
        lqw.eq(KmDatabaseMeta::getDataSourceId, dataSourceId);
        lqw.orderByAsc(KmDatabaseMeta::getTableName);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public Boolean insertByBo(KmDatabaseMetaBo bo) {
        KmDatabaseMeta add = MapstructUtils.convert(bo, KmDatabaseMeta.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmDatabaseMetaBo bo) {
        KmDatabaseMeta update = MapstructUtils.convert(bo, KmDatabaseMeta.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmDatabaseMetaVo> parseDdlAndSave(Long dataSourceId, String ddlContent) {
        log.info("解析DDL并保存元数据, dataSourceId={}", dataSourceId);

        // 解析DDL
        List<KmDatabaseMeta> metas = DdlParser.parse(ddlContent);
        if (metas.isEmpty()) {
            throw new RuntimeException("DDL解析失败，未找到有效的表定义");
        }

        // 保存元数据
        List<KmDatabaseMetaVo> result = new ArrayList<>();
        for (KmDatabaseMeta meta : metas) {
            meta.setDataSourceId(dataSourceId);
            meta.setMetaSourceType("DDL");
            meta.setDdlContent(ddlContent);

            // 检查是否已存在，存在则更新
            LambdaQueryWrapper<KmDatabaseMeta> lqw = Wrappers.lambdaQuery();
            lqw.eq(KmDatabaseMeta::getDataSourceId, dataSourceId);
            lqw.eq(KmDatabaseMeta::getTableName, meta.getTableName());
            KmDatabaseMeta existing = baseMapper.selectOne(lqw);

            if (existing != null) {
                meta.setMetaId(existing.getMetaId());
                baseMapper.updateById(meta);
            } else {
                baseMapper.insert(meta);
            }

            result.add(MapstructUtils.convert(meta, KmDatabaseMetaVo.class));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmDatabaseMetaVo> syncFromDatabase(Long dataSourceId, List<String> tableNames) {
        log.info("从数据库同步元数据, dataSourceId={}, tableNames={}", dataSourceId, tableNames);

        KmDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) {
            throw new RuntimeException("数据源不存在");
        }

        Connection conn = null;
        try {
            // 获取数据库连接
            if ("DYNAMIC".equals(ds.getSourceType())) {
                DataSource dataSource = dataSourceMap.get(ds.getDsKey());
                if (dataSource == null) {
                    throw new RuntimeException("动态数据源不存在: " + ds.getDsKey());
                }
                conn = dataSource.getConnection();
            } else {
                Class.forName(ds.getDriverClassName());
                conn = DriverManager.getConnection(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword());
            }

            DatabaseMetaData metaData = conn.getMetaData();
            List<KmDatabaseMetaVo> result = new ArrayList<>();

            // 获取表列表
            ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" });
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                // 如果指定了表名列表，则只同步指定的表
                if (tableNames != null && !tableNames.isEmpty() && !tableNames.contains(tableName)) {
                    continue;
                }

                String tableComment = tables.getString("REMARKS");

                // 获取列信息
                List<KmDatabaseMeta.ColumnMeta> columns = new ArrayList<>();
                ResultSet cols = metaData.getColumns(null, null, tableName, "%");
                while (cols.next()) {
                    KmDatabaseMeta.ColumnMeta col = new KmDatabaseMeta.ColumnMeta();
                    col.setColumnName(cols.getString("COLUMN_NAME"));
                    col.setColumnType(cols.getString("TYPE_NAME"));
                    col.setColumnComment(cols.getString("REMARKS"));
                    col.setIsNullable("YES".equals(cols.getString("IS_NULLABLE")));
                    columns.add(col);
                }
                cols.close();

                // 获取主键信息
                ResultSet pks = metaData.getPrimaryKeys(null, null, tableName);
                List<String> pkColumns = new ArrayList<>();
                while (pks.next()) {
                    pkColumns.add(pks.getString("COLUMN_NAME"));
                }
                pks.close();

                // 标记主键列
                for (KmDatabaseMeta.ColumnMeta col : columns) {
                    col.setIsPrimaryKey(pkColumns.contains(col.getColumnName()));
                }

                // 保存元数据
                KmDatabaseMeta meta = new KmDatabaseMeta();
                meta.setDataSourceId(dataSourceId);
                meta.setMetaSourceType("JDBC");
                meta.setTableName(tableName);
                meta.setTableComment(tableComment);
                meta.setColumns(columns);

                // 检查是否已存在
                LambdaQueryWrapper<KmDatabaseMeta> lqw = Wrappers.lambdaQuery();
                lqw.eq(KmDatabaseMeta::getDataSourceId, dataSourceId);
                lqw.eq(KmDatabaseMeta::getTableName, tableName);
                KmDatabaseMeta existing = baseMapper.selectOne(lqw);

                if (existing != null) {
                    meta.setMetaId(existing.getMetaId());
                    baseMapper.updateById(meta);
                } else {
                    baseMapper.insert(meta);
                }

                result.add(MapstructUtils.convert(meta, KmDatabaseMetaVo.class));
            }
            tables.close();

            return result;

        } catch (Exception e) {
            log.error("从数据库同步元数据失败", e);
            throw new RuntimeException("同步失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("关闭连接失败", e);
                }
            }
        }
    }

}
