package org.dromara.ai.workflow.workflow.nodes;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataSource;
import org.dromara.ai.knowledge.domain.KmDatabaseMeta;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.knowledge.mapper.KmDataSourceMapper;
import org.dromara.ai.knowledge.mapper.KmDatabaseMetaMapper;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractAiWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SchemaBuilder;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlExecutor;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlGenerator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlValidator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SseHelper;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * 数据库查询节点
 * 结合LLM分析用户问题，生成SQL，执行查询，并用自然语言回答
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Slf4j
@Component(NodeTypeConstants.DB_QUERY)
public class DbQueryNode extends AbstractAiWorkflowNode {

    @Autowired
    private KmDataSourceMapper dataSourceMapper;

    @Autowired
    private KmDatabaseMetaMapper databaseMetaMapper;

    @Autowired
    private SqlExecutor sqlExecutor;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行DB_QUERY节点");

        NodeOutput output = new NodeOutput();
        SseEmitter emitter = context.getSseEmitter();
        Boolean streamOutput = context.getConfigAsBoolean(NodeConfigConstants.CFG_AI_STREAM_OUTPUT, false);

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong(NodeConfigConstants.CFG_SQL_DATA_SOURCE_ID);
        Integer maxRows = context.getConfigAsInteger(NodeConfigConstants.CFG_SQL_MAX_ROWS, 100);
        String tableWhitelist = context.getConfigAsString(NodeConfigConstants.CFG_SQL_TABLE_WHITELIST);
        String tableBlacklist = context.getConfigAsString(NodeConfigConstants.CFG_SQL_TABLE_BLACKLIST);

        // 2. 获取输入参数
        String userQuery = (String) context.getInput(NodeIOConstants.INPUT_USER_QUERY);
        if (StrUtil.isBlank(userQuery)) {
            throw new RuntimeException("userQuery不能为空");
        }

        // 3. 加载数据源和元数据
        KmDataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在: " + dataSourceId);
        }

        List<KmDatabaseMeta> metas = databaseMetaMapper.selectList(
                new LambdaQueryWrapper<KmDatabaseMeta>()
                        .eq(KmDatabaseMeta::getDataSourceId, dataSourceId));
        if (metas.isEmpty()) {
            throw new RuntimeException(MessageUtils.message("ai.msg.datasource.meta_missing"));
        }

        // 4. 加载模型（基类统一处理）
        Object[] modelAndProvider = loadModelAndProvider(context);
        KmModel model = (KmModel) modelAndProvider[0];
        KmModelProvider provider = (KmModelProvider) modelAndProvider[1];
        ChatModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());

        // 5. 选择相关表
        String tableListPrompt = SchemaBuilder.buildTableList(metas, tableWhitelist, tableBlacklist);

        // 发送thinking事件：分析相关表
        SseHelper.sendThinking(emitter, streamOutput, "📊 正在分析数据库结构，筛选相关表...\n");

        List<String> relevantTables = SqlGenerator.selectRelevantTables(chatModel, tableListPrompt, userQuery);
        List<KmDatabaseMeta> filteredMetas;
        if (relevantTables.isEmpty()) {
            log.warn("LLM未选择任何相关表");
            output.addOutput(NodeIOConstants.OUTPUT_RESPONSE, "没有相关的表");
            output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, "");
            output.addOutput(NodeIOConstants.OUTPUT_QUERY_RESULT, "");
            output.addOutput(NodeIOConstants.OUTPUT_STR_RESULT, "");
            log.info("DB_QUERY节点执行完成");
            return output;
        } else {
            log.info("LLM选择的相关表: {}", relevantTables);
            SseHelper.sendThinking(emitter, streamOutput, "✅ 已选择相关表: " + String.join(", ", relevantTables) + "\n");
            Map<String, KmDatabaseMeta> metaMap = new HashMap<>();
            for (KmDatabaseMeta m : metas) {
                metaMap.put(m.getTableName().toLowerCase(), m);
            }

            filteredMetas = new ArrayList<>();
            for (String t : relevantTables) {
                KmDatabaseMeta m = metaMap.get(t.toLowerCase());
                if (m != null) {
                    filteredMetas.add(m);
                }
            }
        }

        // 5. 构建 Schema Prompt
        String schemaDescription = SchemaBuilder.build(filteredMetas, null, null);

        // 6. 生成 SQL（使用工具类）
        SseHelper.sendThinking(emitter, streamOutput, "📝 正在生成SQL查询语句...\n");

        String generatedSql = SqlGenerator.generateSql(chatModel, schemaDescription, userQuery, context);
        if (StrUtil.isBlank(generatedSql) || generatedSql.toUpperCase().contains("SELECT") == false) {
            log.warn("LLM未生成有效的SQL");
            output.addOutput(NodeIOConstants.OUTPUT_RESPONSE, "没有生成SQL");
            output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, "");
            output.addOutput(NodeIOConstants.OUTPUT_QUERY_RESULT, "");
            output.addOutput(NodeIOConstants.OUTPUT_STR_RESULT, "");
            log.info("DB_QUERY节点执行完成");
            return output;
        }
        log.info("生成的SQL: {}", generatedSql);
        output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, generatedSql);

        // 添加 SQL 生成阶段的 token 统计到输出
        Map<String, Object> sqlGenTokenUsage = context.getTokenUsage();
        if (sqlGenTokenUsage != null) {
            output.addOutput(NodeIOConstants.OUTPUT_SQL_GEN_TOKEN_USAGE, sqlGenTokenUsage);
        }

        SseHelper.sendThinking(emitter, streamOutput, "✅ SQL已生成: `" + generatedSql + "`");

        // 7. 校验 SQL（使用工具类）
        SqlValidator.validate(generatedSql);

        // 8. 执行 SQL（使用工具类）
        SseHelper.sendThinking(emitter, streamOutput, "⚡ 正在执行SQL查询...\n");
        List<Map<String, Object>> queryResult = sqlExecutor.executeQuery(dataSource, generatedSql, maxRows);
        output.addOutput(NodeIOConstants.OUTPUT_QUERY_RESULT, queryResult);
        output.addOutput(NodeIOConstants.OUTPUT_STR_RESULT, JsonUtils.toJsonString(queryResult));
        log.info("查询结果行数: {}", queryResult.size());
        SseHelper.sendThinking(emitter, streamOutput, "✅ 查询完成，返回 " + queryResult.size() + " 条记录\n");

        // 9. 生成自然语言回答
        SseHelper.sendThinking(emitter, streamOutput, "💬 正在生成回答...\n");
        String response = generateAnswer(chatModel, userQuery, generatedSql, queryResult, context);
        output.addOutput(NodeIOConstants.OUTPUT_RESPONSE, response);

        // 添加答案生成阶段的 token 统计到输出
        Map<String, Object> answerGenTokenUsage = context.getTokenUsage();
        if (answerGenTokenUsage != null) {
            output.addOutput(NodeIOConstants.OUTPUT_ANSWER_GEN_TOKEN_USAGE, answerGenTokenUsage);
        }

        log.info("DB_QUERY节点执行完成");
        return output;
    }

    /**
     * 调用 LLM 生成自然语言回答
     */
    private String generateAnswer(ChatModel chatModel, String userQuery, String sql,
            List<Map<String, Object>> result, NodeContext context) {
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

        var response = chatModel.chat(messages);

        // token 统计（基类统一处理）
        recordTokenUsage(response, context);

        return response.aiMessage().text();
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.DB_QUERY;
    }

    @Override
    public String getNodeName() {
        return "数据库查询";
    }
}
