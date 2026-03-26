package org.dromara.ai.workflow.workflow.nodes;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataSource;
import org.dromara.ai.knowledge.domain.KmDatabaseMeta;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.knowledge.mapper.KmDataSourceMapper;
import org.dromara.ai.knowledge.mapper.KmDatabaseMetaMapper;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SchemaBuilder;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlExecutor;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlGenerator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlValidator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SseHelper;
import org.dromara.common.json.utils.JsonUtils;
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
@RequiredArgsConstructor
@Component("DB_QUERY")
public class DbQueryNode extends AbstractWorkflowNode {

    private final KmDataSourceMapper dataSourceMapper;
    private final KmDatabaseMetaMapper databaseMetaMapper;
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;
    private final SqlExecutor sqlExecutor;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行DB_QUERY节点");

        NodeOutput output = new NodeOutput();
        SseEmitter emitter = context.getSseEmitter();
        Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong("dataSourceId");
        Long modelId = context.getConfigAsLong("modelId");
        Integer maxRows = context.getConfigAsInteger("maxRows", 100);
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
                new LambdaQueryWrapper<KmDatabaseMeta>()
                        .eq(KmDatabaseMeta::getDataSourceId, dataSourceId));
        if (metas.isEmpty()) {
            throw new RuntimeException(MessageUtils.message("ai.msg.datasource.meta_missing"));
        }

        // 4. 选择相关表
        String tableListPrompt = SchemaBuilder.buildTableList(metas, tableWhitelist, tableBlacklist);

        // 加载 LLM 模型 (提前加载)
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());

        // 发送thinking事件：分析相关表
        SseHelper.sendThinking(emitter, streamOutput, "📊 正在分析数据库结构，筛选相关表...\n");

        List<String> relevantTables = SqlGenerator.selectRelevantTables(chatModel, tableListPrompt, userQuery);

        // 过滤元数据
        List<KmDatabaseMeta> filteredMetas;
        if (relevantTables.isEmpty()) {
            log.warn("LLM未选择任何相关表");
            output.addOutput("response", "没有相关的表");
            output.addOutput("generatedSql", "");
            output.addOutput("queryResult", "");
            output.addOutput("strResult", "");
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
            output.addOutput("response", "没有生成SQL");
            output.addOutput("generatedSql", "");
            output.addOutput("queryResult", "");
            output.addOutput("strResult", "");
            log.info("DB_QUERY节点执行完成");
            return output;
        }
        log.info("生成的SQL: {}", generatedSql);
        output.addOutput("generatedSql", generatedSql);

        // 添加 SQL 生成阶段的 token 统计到输出
        Map<String, Object> sqlGenTokenUsage = context.getTokenUsage();
        if (sqlGenTokenUsage != null) {
            output.addOutput("sqlGenTokenUsage", sqlGenTokenUsage);
        }

        SseHelper.sendThinking(emitter, streamOutput, "✅ SQL已生成: `" + generatedSql + "`");

        // 7. 校验 SQL（使用工具类）
        SqlValidator.validate(generatedSql);

        // 8. 执行 SQL（使用工具类）
        SseHelper.sendThinking(emitter, streamOutput, "⚡ 正在执行SQL查询...\n");
        List<Map<String, Object>> queryResult = sqlExecutor.executeQuery(dataSource, generatedSql, maxRows);
        output.addOutput("queryResult", queryResult);
        output.addOutput("strResult", JsonUtils.toJsonString(queryResult));
        log.info("查询结果行数: {}", queryResult.size());
        SseHelper.sendThinking(emitter, streamOutput, "✅ 查询完成，返回 " + queryResult.size() + " 条记录\n");

        // 9. 生成自然语言回答
        SseHelper.sendThinking(emitter, streamOutput, "💬 正在生成回答...\n");
        String response = generateAnswer(chatModel, userQuery, generatedSql, queryResult, context);
        output.addOutput("response", response);

        // 添加答案生成阶段的 token 统计到输出
        Map<String, Object> answerGenTokenUsage = context.getTokenUsage();
        if (answerGenTokenUsage != null) {
            output.addOutput("answerGenTokenUsage", answerGenTokenUsage);
        }

        log.info("DB_QUERY节点执行完成");
        return output;
    }

    /**
     * 调用 LLM 生成自然语言回答
     */
    private String generateAnswer(ChatLanguageModel chatModel, String userQuery, String sql,
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

        var response = chatModel.generate(messages);

        // 保存 token 使用信息
        if (response != null && response.tokenUsage() != null) {
            TokenUsage tokenUsage = response.tokenUsage();
            Map<String, Object> tokenUsageMap = new HashMap<>();
            tokenUsageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
            tokenUsageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
            tokenUsageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
            context.setTokenUsage(tokenUsageMap);
        }

        return response.content().text();
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
