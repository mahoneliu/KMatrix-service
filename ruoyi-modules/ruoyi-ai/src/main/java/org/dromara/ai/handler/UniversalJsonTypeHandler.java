package org.dromara.ai.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 通用 JSON TypeHandler
 * 自动适配 PostgreSQL (使用 Types.OTHER) 和 MySQL (使用 setString)
 * 解决 PostgreSQL JSONB 类型字段插入报错的问题，同时保持对 MySQL 的兼容性
 */
@Slf4j
@MappedTypes({ Object.class })
public class UniversalJsonTypeHandler extends JacksonTypeHandler {

    public UniversalJsonTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.NULL);
            return;
        }

        String json = toJson(parameter);
        if (isPostgreSQL(ps)) {
            // PostgreSQL requires JSONB to be set via setObject with Types.OTHER
            ps.setObject(i, json, Types.OTHER);
        } else {
            // MySQL and others typically use setString for JSON/VARCHAR columns
            ps.setString(i, json);
        }
    }

    /**
     * 判断当前数据源是否为 PostgreSQL
     */
    private boolean isPostgreSQL(PreparedStatement ps) {
        try {
            String productName = ps.getConnection().getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        } catch (SQLException e) {
            log.warn("Failed to determine database product name", e);
            return false;
        }
    }
}
