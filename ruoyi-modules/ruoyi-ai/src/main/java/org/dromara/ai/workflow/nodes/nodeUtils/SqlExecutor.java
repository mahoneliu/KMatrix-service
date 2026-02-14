package org.dromara.ai.workflow.nodes.nodeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * SQL执行工具类
 * 
 * @author Mahone
 * @date 2026-01-24
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SqlExecutor {

    private final Map<String, DataSource> dataSourceMap;

    /**
     * 执行 SQL 查询
     * 
     * @param ds      数据源配置
     * @param sql     SQL 语句
     * @param maxRows 最大返回行数
     * @return 查询结果列表
     * @throws Exception 执行异常
     */
    public List<Map<String, Object>> executeQuery(KmDataSource ds, String sql, int maxRows) throws Exception {
        Connection conn = null;
        try {
            conn = getConnection(ds);

            // 添加 LIMIT 限制（如果没有的话）
            String limitedSql = sql.trim();
            if (!limitedSql.toUpperCase().contains("LIMIT")) {
                limitedSql = limitedSql.replaceAll(";$", "") + " LIMIT " + maxRows;
            }

            Statement stmt = conn.createStatement();
            stmt.setMaxRows(maxRows);
            ResultSet rs = stmt.executeQuery(limitedSql);

            List<Map<String, Object>> result = convertResultSet(rs);

            rs.close();
            stmt.close();
            return result;

        } finally {
            closeConnection(conn);
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection(KmDataSource ds) throws Exception {
        if ("DYNAMIC".equals(ds.getSourceType())) {
            DataSource dataSource = dataSourceMap.get(ds.getDsKey());
            if (dataSource == null) {
                throw new RuntimeException("动态数据源不存在: " + ds.getDsKey());
            }
            return dataSource.getConnection();
        } else {
            Class.forName(ds.getDriverClassName());
            return DriverManager.getConnection(ds.getJdbcUrl(), ds.getUsername(), ds.getPassword());
        }
    }

    /**
     * 转换 ResultSet 为 List<Map>
     */
    private List<Map<String, Object>> convertResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<Map<String, Object>> result = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 安全关闭连接
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("关闭连接失败", e);
            }
        }
    }
}
