package org.dromara.common.mybatis.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库兼容的 JSON TypeHandler
 * 支持 MySQL (JSON) 和 PostgreSQL (JSONB)
 * 
 * @author Mahone
 * @date 2026-01-27
 */
@MappedTypes({ Object.class })
public class JsonTypeHandler extends BaseTypeHandler<Object> {

    private final Class<?> type;

    public JsonTypeHandler(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        // 如果原本就是 String，且已经是 JSON 格式，不应再进行序列化，否则会带双引号
        String json = parameter instanceof String ? (String) parameter : JSONUtil.toJsonStr(parameter);

        // 清理不合法的 UTF-8 字符和 Null 字符，防止 PostgreSQL 报错 invalid byte sequence for encoding
        // "UTF8"
        if (json != null) {
            json = json.replace("\u0000", "");
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }

        // 如果是 PostgreSQL 环境，需要特别处理 jsonb 类型
        // 通过反射判断是否有 PGobject 类，避免对非 PG 环境产生强依赖
        if (isPostgreSQL(ps)) {
            try {
                Object pgObject = Class.forName("org.postgresql.util.PGobject").getDeclaredConstructor().newInstance();
                pgObject.getClass().getMethod("setType", String.class).invoke(pgObject, "jsonb");
                pgObject.getClass().getMethod("setValue", String.class).invoke(pgObject, json);
                ps.setObject(i, pgObject);
                return;
            } catch (Exception e) {
                // 如果反射失败（理论上不应该，因为检测到了是 PG），则回退到普通 setObject
                ps.setObject(i, json, java.sql.Types.OTHER);
            }
        } else {
            // MySQL 等数据库直接使用 setString，避免 Types.OTHER 导致被识别为 binary 字符集
            ps.setString(i, json);
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
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    private Object parseJson(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 如果目标类型就是 String，直接返回原始 JSON 字符串
        if (type == String.class) {
            return json;
        }
        try {
            return JSONUtil.toBean(json, type);
        } catch (Exception e) {
            return null;
        }
    }
}
