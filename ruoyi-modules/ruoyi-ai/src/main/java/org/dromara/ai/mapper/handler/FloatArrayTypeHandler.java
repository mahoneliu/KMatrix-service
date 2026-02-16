package org.dromara.ai.mapper.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;
import java.sql.*;
import java.util.Arrays;

/**
 * Float数组类型处理器
 * 用于将 float[] 转换为 PostgreSQL 的 vector 类型
 *
 * @author Mahone
 * @date 2026-01-29
 */
@MappedTypes(float[].class)
public class FloatArrayTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        // 将 float[] 转换为 PostgreSQL vector 格式字符串: [1.0,2.0,3.0]
        String vectorString = Arrays.toString(parameter);
        // 使用 PGobject 设置 vector 类型
        PGobject pGobject = new PGobject();
        pGobject.setType("vector");
        pGobject.setValue(vectorString);
        ps.setObject(i, pGobject);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getString(columnIndex));
    }

    /**
     * 解析 PostgreSQL vector 字符串为 float[]
     * 格式: [1.0,2.0,3.0]
     */
    private float[] parseVector(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return null;
        }
        // 移除首尾的方括号
        String content = vectorString.substring(1, vectorString.length() - 1);
        if (content.isEmpty()) {
            return new float[0];
        }
        String[] parts = content.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
