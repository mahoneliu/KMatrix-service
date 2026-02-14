package org.dromara.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDatabaseMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DDL (CREATE TABLE) 语句解析器
 * 支持解析 MySQL 语法的建表语句
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Slf4j
public class DdlParser {

    // 匹配 CREATE TABLE 语句
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`'\"]?([\\w_]+)[`'\"]?\\s*\\(([^;]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // 匹配列定义
    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "^\\s*[`'\"]?([\\w_]+)[`'\"]?\\s+([\\w()]+(?:\\s*\\([^)]+\\))?)(?:\\s+(.*))?$",
            Pattern.CASE_INSENSITIVE);

    // 匹配 COMMENT
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "COMMENT\\s+['\"]([^'\"]*)['\"]",
            Pattern.CASE_INSENSITIVE);

    // 匹配表注释
    private static final Pattern TABLE_COMMENT_PATTERN = Pattern.compile(
            "\\)\\s*(?:ENGINE\\s*=[^;]*)?\\s*COMMENT\\s*=?\\s*['\"]([^'\"]*)['\"]",
            Pattern.CASE_INSENSITIVE);

    // 匹配 PRIMARY KEY
    private static final Pattern PRIMARY_KEY_PATTERN = Pattern.compile(
            "PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE);

    // 匹配 NOT NULL
    private static final Pattern NOT_NULL_PATTERN = Pattern.compile(
            "NOT\\s+NULL",
            Pattern.CASE_INSENSITIVE);

    /**
     * 解析 DDL 内容，提取表结构信息
     *
     * @param ddlContent DDL 内容
     * @return 解析后的表元数据列表
     */
    public static List<KmDatabaseMeta> parse(String ddlContent) {
        List<KmDatabaseMeta> result = new ArrayList<>();

        if (ddlContent == null || ddlContent.trim().isEmpty()) {
            return result;
        }

        Matcher tableMatcher = TABLE_PATTERN.matcher(ddlContent);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String tableBody = tableMatcher.group(2);

            log.debug("解析表: {}", tableName);

            // 提取表注释
            String tableComment = null;
            Matcher tableCommentMatcher = TABLE_COMMENT_PATTERN.matcher(ddlContent);
            if (tableCommentMatcher.find()) {
                tableComment = tableCommentMatcher.group(1);
            }

            // 提取主键列
            List<String> primaryKeys = new ArrayList<>();
            Matcher pkMatcher = PRIMARY_KEY_PATTERN.matcher(tableBody);
            if (pkMatcher.find()) {
                String pkCols = pkMatcher.group(1);
                for (String pk : pkCols.split(",")) {
                    primaryKeys.add(pk.trim().replaceAll("[`'\"]", ""));
                }
            }

            // 解析列定义
            List<KmDatabaseMeta.ColumnMeta> columns = new ArrayList<>();
            String[] lines = tableBody.split(",(?![^()]*\\))");

            for (String line : lines) {
                line = line.trim();

                // 跳过约束定义
                if (line.toUpperCase().startsWith("PRIMARY") ||
                        line.toUpperCase().startsWith("KEY") ||
                        line.toUpperCase().startsWith("INDEX") ||
                        line.toUpperCase().startsWith("UNIQUE") ||
                        line.toUpperCase().startsWith("FOREIGN") ||
                        line.toUpperCase().startsWith("CONSTRAINT")) {
                    continue;
                }

                Matcher colMatcher = COLUMN_PATTERN.matcher(line);
                if (colMatcher.find()) {
                    String colName = colMatcher.group(1);
                    String colType = colMatcher.group(2);
                    String rest = colMatcher.group(3);

                    KmDatabaseMeta.ColumnMeta column = new KmDatabaseMeta.ColumnMeta();
                    column.setColumnName(colName);
                    column.setColumnType(colType);
                    column.setIsPrimaryKey(primaryKeys.contains(colName));

                    // 解析 NOT NULL
                    if (rest != null) {
                        column.setIsNullable(!NOT_NULL_PATTERN.matcher(rest).find());

                        // 解析 COMMENT
                        Matcher commentMatcher = COMMENT_PATTERN.matcher(rest);
                        if (commentMatcher.find()) {
                            column.setColumnComment(commentMatcher.group(1));
                        }
                    } else {
                        column.setIsNullable(true);
                    }

                    columns.add(column);
                    log.debug("  列: {} {}", colName, colType);
                }
            }

            // 构建结果
            KmDatabaseMeta meta = new KmDatabaseMeta();
            meta.setTableName(tableName);
            meta.setTableComment(tableComment);
            meta.setColumns(columns);
            result.add(meta);
        }

        return result;
    }

}
