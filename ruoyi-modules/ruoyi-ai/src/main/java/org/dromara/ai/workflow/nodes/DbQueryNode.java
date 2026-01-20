package org.dromara.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.ai.domain.KmDatabaseMeta;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.mapper.KmDataSourceMapper;
import org.dromara.ai.mapper.KmDatabaseMetaMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库查询节点
 * 结合LLM分析用户问题，生成SQL，执行查询，并用自然语言回答
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Slf4j
@RequiredArgsConstructor
@Component("DB_QUERY")
public class DbQueryNode implements WorkflowNode {

    private final KmDataSourceMapper dataSourceMapper;
    private final KmDatabaseMetaMapper databaseMetaMapper;
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;
    private final Map<String, DataSource> dataSourceMap;

    // SQL 提取正则
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "```sql\\s*([\\s\\S]*?)\\s*```|```\\s*([\\s\\S]*?)\\s*```|SELECT[\\s\\S]*?(?:;|$)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行DB_QUERY节点");

        NodeOutput output = new NodeOutput();

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong("dataSourceId");
        Long modelId = context.getConfigAsLong("modelId");
        Integer maxRows = getConfigAsInteger(context, "maxRows", 100);
        String tableWhitelist = context.getConfigAsString("tableWhitelist");
        String tableBlacklist = context.getConfigAsString("tableBlacklist");

        // 2. 获取输入参数
        String userQuery = (String) context.getInput("userQuery");
        if (StrUtil.isBlank(userQuery)) {
            throw new RuntimeException("userQuery不能为空");
        }

        // 3. 加载数据源和元数据
        KmDataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在: " + dataSourceId);
        }

        List<KmDatabaseMeta> metas = databaseMetaMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KmDatabaseMeta>()
                        .eq(KmDatabaseMeta::getDataSourceId, dataSourceId));
        if (metas.isEmpty()) {
            throw new RuntimeException("数据源没有配置元数据，请先添加表结构信息");
        }

        // 4. 构建 Schema Prompt
        String schemaDescription = buildSchemaDescription(metas, tableWhitelist, tableBlacklist);

        // 5. 加载 LLM 模型
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());

        // 6. 生成 SQL
        String generatedSql = generateSql(chatModel, schemaDescription, userQuery);
        log.info("生成的SQL: {}", generatedSql);
        output.addOutput("generatedSql", generatedSql);

        // 7. 校验 SQL
        validateSql(generatedSql, tableWhitelist, tableBlacklist);

        // 8. 执行 SQL
        List<Map<String, Object>> queryResult = executeQuery(dataSource, generatedSql, maxRows);
        output.addOutput("queryResult", queryResult);
        log.info("查询结果行数: {}", queryResult.size());

        // 9. 生成自然语言回答
        String response = generateAnswer(chatModel, userQuery, generatedSql, queryResult);
        output.addOutput("response", response);

        log.info("DB_QUERY节点执行完成");
        return output;
    }

    /**
     * 构建数据库 Schema 描述
     */
    private String buildSchemaDescription(List<KmDatabaseMeta> metas, String whitelist, String blacklist) {
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
     * 调用 LLM 生成 SQL
     */
    private String generateSql(ChatLanguageModel chatModel, String schema, String userQuery) {
        String systemPrompt = """
                你是一个专业的数据库助手。根据用户的问题和提供的数据库表结构，生成正确的SQL查询语句。

                要求：
                1. 只生成 SELECT 语句，不允许任何修改数据的操作
                2. SQL 语句用 ```sql ``` 包裹
                3. 确保SQL语法正确
                4. 如果用户问的问题无法通过给定的表结构查询，请说明原因

                数据库表结构：
                """ + schema;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userQuery));

        String response = chatModel.generate(messages).content().text();

        // 提取 SQL
        Matcher matcher = SQL_PATTERN.matcher(response);
        if (matcher.find()) {
            String sql = matcher.group(1);
            if (sql == null)
                sql = matcher.group(2);
            if (sql == null)
                sql = matcher.group(0);
            return sql.trim();
        }

        throw new RuntimeException("无法从LLM响应中提取SQL语句: " + response);
    }

    /**
     * 校验 SQL 安全性
     */
    private void validateSql(String sql, String whitelist, String blacklist) {
        String upperSql = sql.toUpperCase().trim();

        // 只允许 SELECT
        if (!upperSql.startsWith("SELECT")) {
            throw new RuntimeException("仅允许SELECT查询语句");
        }

        // 禁止危险操作
        String[] forbidden = { "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE" };
        for (String keyword : forbidden) {
            if (upperSql.contains(keyword)) {
                throw new RuntimeException("禁止使用 " + keyword + " 语句");
            }
        }

        // TODO: 可添加更多白名单/黑名单表检查
    }

    /**
     * 执行 SQL 查询
     */
    private List<Map<String, Object>> executeQuery(KmDataSource ds, String sql, int maxRows) throws Exception {
        Connection conn = null;
        try {
            // 获取连接
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

            // 添加 LIMIT 限制
            String limitedSql = sql.trim();
            if (!limitedSql.toUpperCase().contains("LIMIT")) {
                limitedSql = limitedSql.replaceAll(";$", "") + " LIMIT " + maxRows;
            }

            Statement stmt = conn.createStatement();
            stmt.setMaxRows(maxRows);
            ResultSet rs = stmt.executeQuery(limitedSql);

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

            rs.close();
            stmt.close();
            return result;

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

    /**
     * 调用 LLM 生成自然语言回答
     */
    private String generateAnswer(ChatLanguageModel chatModel, String userQuery, String sql,
            List<Map<String, Object>> result) {
        String systemPrompt = """
                你是一个数据分析助手。根据用户的问题和SQL查询结果，用简洁清晰的自然语言回答用户的问题。

                要求：
                1. 直接回答用户的问题，不要解释SQL
                2. 如果结果为空，说明没有找到相关数据
                3. 数字结果要准确
                """;

        String resultStr = JsonUtils.toJsonString(result);
        if (resultStr.length() > 2000) {
            resultStr = resultStr.substring(0, 2000) + "...(结果过长已截断)";
        }

        String userPrompt = String.format("""
                用户问题: %s

                执行的SQL: %s

                查询结果: %s

                请根据查询结果回答用户的问题。
                """, userQuery, sql, resultStr);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userPrompt));

        return chatModel.generate(messages).content().text();
    }

    private Set<String> parseListConfig(String config) {
        Set<String> set = new HashSet<>();
        if (StrUtil.isNotBlank(config)) {
            for (String item : config.split(",")) {
                set.add(item.trim().toLowerCase());
            }
        }
        return set;
    }

    private Integer getConfigAsInteger(NodeContext context, String key, Integer defaultValue) {
        Object value = context.getConfig(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public String getNodeType() {
        return "DB_QUERY";
    }

    @Override
    public String getNodeName() {
        return "数据库查询";
    }

}
