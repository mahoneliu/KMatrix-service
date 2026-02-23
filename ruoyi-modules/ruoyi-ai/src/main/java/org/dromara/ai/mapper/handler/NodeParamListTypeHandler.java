package org.dromara.ai.mapper.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.dromara.ai.domain.vo.NodeParamDefinitionVo;

import java.sql.*;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis TypeHandler for converting JSON string to List<NodeParamDefinitionVo>
 *
 * @author Mahone
 * @date 2026-01-14
 */
public class NodeParamListTypeHandler extends BaseTypeHandler<List<NodeParamDefinitionVo>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<NodeParamDefinitionVo> parameter,
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
    public List<NodeParamDefinitionVo> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<NodeParamDefinitionVo> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<NodeParamDefinitionVo> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private List<NodeParamDefinitionVo> parseJson(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, NodeParamDefinitionVo.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
