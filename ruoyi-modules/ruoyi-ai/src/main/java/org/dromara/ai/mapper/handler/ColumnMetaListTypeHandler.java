package org.dromara.ai.mapper.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.dromara.ai.domain.KmDatabaseMeta;

import java.sql.*;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis TypeHandler for converting JSON string to
 * List<KmDatabaseMeta.ColumnMeta>
 *
 * @author Mahone
 * @date 2026-01-27
 */
public class ColumnMetaListTypeHandler extends BaseTypeHandler<List<KmDatabaseMeta.ColumnMeta>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<KmDatabaseMeta.ColumnMeta> parameter,
            JdbcType jdbcType) throws SQLException {
        String json = JSONUtil.toJsonStr(parameter);
        if (isPostgreSQL(ps)) {
            try {
                Object pgObject = Class.forName("org.postgresql.util.PGobject").getDeclaredConstructor().newInstance();
                pgObject.getClass().getMethod("setType", String.class).invoke(pgObject, "jsonb");
                pgObject.getClass().getMethod("setValue", String.class).invoke(pgObject, json);
                ps.setObject(i, pgObject);
                return;
            } catch (Exception e) {
                ps.setObject(i, json, Types.OTHER);
            }
        } else {
            ps.setObject(i, json, Types.OTHER);
        }
    }

    /**
     * 判断当前数据源是否为 PostgreSQL
     */
    private boolean isPostgreSQL(PreparedStatement ps) {
        try {
            String url = ps.getConnection().getMetaData().getURL();
            return StrUtil.containsIgnoreCase(url, "postgresql");
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<KmDatabaseMeta.ColumnMeta> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<KmDatabaseMeta.ColumnMeta> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<KmDatabaseMeta.ColumnMeta> getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<KmDatabaseMeta.ColumnMeta> parseJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, KmDatabaseMeta.ColumnMeta.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
