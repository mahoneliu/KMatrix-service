package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
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
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlGenerator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlValidator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SseHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL生成节点
 * 使用LLM分析用户问题并生成SQL语句
 *
 * @author Mahone
 * @date 2026-01-24
 */
@Slf4j
@Component(NodeTypeConstants.SQL_GENERATE)
public class SqlGenerateNode extends AbstractAiWorkflowNode {

    @Autowired
    private KmDataSourceMapper dataSourceMapper;

    @Autowired
    private KmDatabaseMetaMapper databaseMetaMapper;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行SQL_GENERATE节点");

        NodeOutput output = new NodeOutput();
        SseEmitter emitter = context.getSseEmitter();

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong(NodeConfigConstants.CFG_SQL_DATA_SOURCE_ID);
        String tableWhitelist = context.getConfigAsString(NodeConfigConstants.CFG_SQL_TABLE_WHITELIST);
        String tableBlacklist = context.getConfigAsString(NodeConfigConstants.CFG_SQL_TABLE_BLACKLIST);

        Double temperature = context.getConfigAsDouble(NodeConfigConstants.CFG_AI_TEMPERATURE, null);
        Integer maxTokens = context.getConfigAsInteger(NodeConfigConstants.CFG_AI_MAX_TOKENS, null);
        Boolean streamOutput = context.getConfigAsBoolean(NodeConfigConstants.CFG_AI_STREAM_OUTPUT, false);

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
            throw new RuntimeException("数据源元数据为空，请先同步元数据");
        }

        // 4. 加载模型（基类统一处理）
        Object[] modelAndProvider = loadModelAndProvider(context);
        KmModel model = (KmModel) modelAndProvider[0];
        KmModelProvider provider = (KmModelProvider) modelAndProvider[1];

        // 5. 选择相关表（使用阻塞模型，不需要流式）
        String tableListPrompt = SchemaBuilder.buildTableList(metas, tableWhitelist, tableBlacklist);
        SseHelper.sendThinking(emitter, streamOutput, "📊 正在分析数据库结构，筛选相关表...\n");

        ChatModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());
        List<String> relevantTables = SqlGenerator.selectRelevantTables(chatModel, tableListPrompt, userQuery);
        log.info("LLM选择的相关表: {}", relevantTables);

        if (relevantTables.isEmpty()) {
            log.warn("LLM未选择任何相关表");
            output.addOutput(NodeIOConstants.OUTPUT_RESPONSE, "没有相关的表");
            output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, "");
            output.addOutput(NodeIOConstants.OUTPUT_QUERY_RESULT, "");
            output.addOutput(NodeIOConstants.OUTPUT_STR_RESULT, "");
            return output;
        }

        Map<String, KmDatabaseMeta> metaMap = new HashMap<>();
        for (KmDatabaseMeta m : metas) {
            metaMap.put(m.getTableName().toLowerCase(), m);
        }
        List<KmDatabaseMeta> filteredMetas = new ArrayList<>();
        for (String t : relevantTables) {
            KmDatabaseMeta m = metaMap.get(t.toLowerCase());
            if (m != null) filteredMetas.add(m);
        }
        SseHelper.sendThinking(emitter, streamOutput,
                "📊 已经生成相关表: "
                        + filteredMetas.stream().map(KmDatabaseMeta::getTableName).collect(Collectors.joining(", "))
                        + "\n");

        SseHelper.sendThinking(emitter, streamOutput, "📊 正在生成SQL语句...\n");

        // 6. 构建 Schema Prompt 并生成 SQL
        String schemaDescription = SchemaBuilder.build(filteredMetas, null, null);
        String generatedSql;
        if (Boolean.TRUE.equals(streamOutput)) {
            StreamingChatModel streamingModel = modelBuilder
                    .buildStreamingChatModel(model, provider.getProviderKey(), temperature, maxTokens);
            generatedSql = SqlGenerator.generateSql(streamingModel, schemaDescription, userQuery, context);
        } else {
            generatedSql = SqlGenerator.generateSql(chatModel, schemaDescription, userQuery, context);
        }

        if (StrUtil.isBlank(generatedSql) || !generatedSql.toUpperCase().contains("SELECT")) {
            log.warn("LLM未生成有效的SQL");
            output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, "");
            return output;
        }
        log.info("生成的SQL: {}", generatedSql);

        SqlValidator.validate(generatedSql);
        output.addOutput(NodeIOConstants.OUTPUT_GENERATED_SQL, generatedSql);

        // token 统计（基类已写入 context）
        Map<String, Object> tokenUsage = context.getTokenUsage();
        if (tokenUsage != null) {
            output.addOutput(NodeIOConstants.OUTPUT_TOKEN_USAGE, tokenUsage);
        }

        log.info("SQL_GENERATE节点执行完成");
        return output;
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.SQL_GENERATE;
    }

    @Override
    public String getNodeName() {
        return "SQL生成";
    }
}
