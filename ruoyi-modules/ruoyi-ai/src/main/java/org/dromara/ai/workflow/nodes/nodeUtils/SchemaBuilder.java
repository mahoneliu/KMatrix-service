package org.dromara.ai.workflow.nodes.nodeUtils;

import cn.hutool.core.util.StrUtil;
import org.dromara.ai.domain.KmDatabaseMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库 Schema 描述构建工具类
 * 
 * @author Mahone
 * @date 2026-01-24
 */
public final class SchemaBuilder {

    private SchemaBuilder() {
        // 工具类禁止实例化
    }

    /**
     * 构建数据库 Schema 描述（用于 LLM Prompt）
     * 
     * @param metas     表元数据列表
     * @param whitelist 表白名单（逗号分隔）
     * @param blacklist 表黑名单（逗号分隔）
     * @return Schema 描述文本
     */
    public static String build(List<KmDatabaseMeta> metas, String whitelist, String blacklist) {
        Set<String> whitelistSet = parseListConfig(whitelist);
        Set<String> blacklistSet = parseListConfig(blacklist);

        StringBuilder sb = new StringBuilder();
        sb.append("数据库包含以下表结构：\n\n");

        for (KmDatabaseMeta meta : metas) {
            String tableName = meta.getTableName();

            // 白名单过滤
            if (!whitelistSet.isEmpty() && !whitelistSet.contains(tableName.toLowerCase())) {
                continue;
            }
            // 黑名单过滤
            if (blacklistSet.contains(tableName.toLowerCase())) {
                continue;
            }

            sb.append("表名: ").append(tableName);
            if (StrUtil.isNotBlank(meta.getTableComment())) {
                sb.append(" (").append(meta.getTableComment()).append(")");
            }
            sb.append("\n列:\n");

            if (meta.getColumns() != null) {
                for (KmDatabaseMeta.ColumnMeta col : meta.getColumns()) {
                    sb.append("  - ").append(col.getColumnName())
                            .append(" ").append(col.getColumnType());
                    if (Boolean.TRUE.equals(col.getIsPrimaryKey())) {
                        sb.append(" [主键]");
                    }
                    if (StrUtil.isNotBlank(col.getColumnComment())) {
                        sb.append(" -- ").append(col.getColumnComment());
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建数据库表列表（仅包含表名和注释，用于初步筛选）
     *
     * @param metas     表元数据列表
     * @param whitelist 表白名单（逗号分隔）
     * @param blacklist 表黑名单（逗号分隔）
     * @return 表列表描述文本
     */
    public static String buildTableList(List<KmDatabaseMeta> metas, String whitelist, String blacklist) {
        Set<String> whitelistSet = parseListConfig(whitelist);
        Set<String> blacklistSet = parseListConfig(blacklist);

        StringBuilder sb = new StringBuilder();
        sb.append("数据库包含以下表：\n\n");

        for (KmDatabaseMeta meta : metas) {
            String tableName = meta.getTableName();

            // 白名单过滤
            if (!whitelistSet.isEmpty() && !whitelistSet.contains(tableName.toLowerCase())) {
                continue;
            }
            // 黑名单过滤
            if (blacklistSet.contains(tableName.toLowerCase())) {
                continue;
            }

            sb.append("- ").append(tableName);
            if (StrUtil.isNotBlank(meta.getTableComment())) {
                sb.append(" (").append(meta.getTableComment()).append(")");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 解析逗号分隔的配置项为 Set
     */
    private static Set<String> parseListConfig(String config) {
        Set<String> set = new HashSet<>();
        if (StrUtil.isNotBlank(config)) {
            for (String item : config.split(",")) {
                set.add(item.trim().toLowerCase());
            }
        }
        return set;
    }
}
