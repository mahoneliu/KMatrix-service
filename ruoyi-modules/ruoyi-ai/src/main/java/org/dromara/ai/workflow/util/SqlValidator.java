package org.dromara.ai.workflow.util;

import lombok.extern.slf4j.Slf4j;

/**
 * SQL安全校验工具类
 * 
 * @author Mahone
 * @date 2026-01-24
 */
@Slf4j
public final class SqlValidator {

    private static final String[] FORBIDDEN_KEYWORDS = {
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE"
    };

    private SqlValidator() {
        // 工具类禁止实例化
    }

    /**
     * 校验 SQL 安全性
     * 
     * @param sql 待校验的 SQL 语句
     * @throws RuntimeException 如果 SQL 不安全
     */
    public static void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new RuntimeException("SQL语句不能为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 只允许 SELECT
        if (!upperSql.startsWith("SELECT")) {
            throw new RuntimeException("仅允许SELECT查询语句");
        }

        // 禁止危险操作
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                throw new RuntimeException("禁止使用 " + keyword + " 语句");
            }
        }
    }

    /**
     * 校验 SQL 安全性（静默模式，返回是否有效）
     * 
     * @param sql 待校验的 SQL 语句
     * @return true 如果 SQL 有效
     */
    public static boolean isValid(String sql) {
        try {
            validate(sql);
            return true;
        } catch (RuntimeException e) {
            log.warn("SQL校验失败: {}", e.getMessage());
            return false;
        }
    }
}
