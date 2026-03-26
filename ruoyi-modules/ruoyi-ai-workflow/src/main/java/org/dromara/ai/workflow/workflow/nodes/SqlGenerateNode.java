package org.dromara.ai.workflow.workflow.nodes;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlGenerator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SqlValidator;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SseHelper;
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
@RequiredArgsConstructor
@Component("SQL_GENERATE")
public class SqlGenerateNode extends AbstractWorkflowNode {

    private final KmDataSourceMapper dataSourceMapper;
    private final KmDatabaseMetaMapper databaseMetaMapper;
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行SQL_GENERATE节点");

        NodeOutput output = new NodeOutput();
        SseEmitter emitter = context.getSseEmitter();

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong("dataSourceId");
        Long modelId = context.getConfigAsLong("modelId");
        String tableWhitelist = context.getConfigAsString("tableWhitelist");
        String tableBlacklist = context.getConfigAsString("tableBlacklist");

        // 获取大模型参数配置
        Double temperature = context.getConfigAsDouble("temperature", null);
        Integer maxTokens = context.getConfigAsInteger("maxTokens", null);
        Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);

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

        // 加载 LLM 模型 (提前加载，因为选择表也需要)
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }

        SseHelper.sendThinking(emitter, streamOutput, "📊 正在分析数据库结构，筛选相关表...\n");

        // 这一步通常不需要流式，也不太需要用户感知的"thinking"，使用默认阻塞模型
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());

        List<String> relevantTables = SqlGenerator.selectRelevantTables(chatModel, tableListPrompt, userQuery);
        log.info("LLM选择的相关表: {}", relevantTables);

        // 过滤元数据
        List<KmDatabaseMeta> filteredMetas;
        if (relevantTables.isEmpty()) {
            // 如果没有选出表，为了避免错误，可以使用所有过滤后的表，或者抛出异常。
            log.warn("LLM未选择任何相关表");
            output.addOutput("response", "没有相关的表");
            output.addOutput("generatedSql", "");
            output.addOutput("queryResult", "");
            output.addOutput("strResult", "");
            log.info("DB_QUERY节点执行完成");
            return output;
        } else {
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
            SseHelper.sendThinking(emitter, streamOutput,
                    "📊 已经生成相关表: "
                            + filteredMetas.stream().map(KmDatabaseMeta::getTableName).collect(Collectors.joining(", "))
                            + "\n");
        }

        SseHelper.sendThinking(emitter, streamOutput, "📊 正在生成SQL语句...\n");
        // 5. 构建 Schema Prompt
        String schemaDescription = SchemaBuilder.build(filteredMetas, null, null); // 已经过滤过了，这里不再传黑白名单

        // 6. 生成 SQL
        String generatedSql;
        if (Boolean.TRUE.equals(streamOutput)) {
            StreamingChatLanguageModel streamingModel = modelBuilder
                    .buildStreamingChatModel(model, provider.getProviderKey(), temperature, maxTokens);
            generatedSql = SqlGenerator.generateSql(streamingModel, schemaDescription, userQuery, context);
        } else {
            generatedSql = SqlGenerator.generateSql(chatModel, schemaDescription, userQuery, context);
        }

        if (StrUtil.isBlank(generatedSql) || generatedSql.toUpperCase().contains("SELECT") == false) {
            log.warn("LLM未生成有效的SQL");
            output.addOutput("generatedSql", "");
            log.info("SQL_GENERATE节点执行完成");
            return output;
        }
        log.info("生成的SQL: {}", generatedSql);

        // 7. 校验 SQL（使用工具类）
        SqlValidator.validate(generatedSql);

        output.addOutput("generatedSql", generatedSql);

        // 添加 token 使用统计到输出
        Map<String, Object> tokenUsage = context.getTokenUsage();
        if (tokenUsage != null) {
            output.addOutput("tokenUsage", tokenUsage);
        }

        log.info("SQL_GENERATE节点执行完成");
        return output;
    }

    @Override
    public String getNodeType() {
        return "SQL_GENERATE";
    }

    @Override
    public String getNodeName() {
        return "SQL生成";
    }
}
