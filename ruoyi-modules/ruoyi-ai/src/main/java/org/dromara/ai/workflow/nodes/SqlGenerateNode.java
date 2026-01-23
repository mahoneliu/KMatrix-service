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
import org.dromara.ai.workflow.util.SchemaBuilder;
import org.dromara.ai.workflow.util.SqlGenerator;
import org.dromara.ai.workflow.util.SqlValidator;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.*;

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
public class SqlGenerateNode implements WorkflowNode {

    private final KmDataSourceMapper dataSourceMapper;
    private final KmDatabaseMetaMapper databaseMetaMapper;
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行SQL_GENERATE节点");

        NodeOutput output = new NodeOutput();

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong("dataSourceId");
        Long modelId = context.getConfigAsLong("modelId");
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

        // 4. 构建 Schema Prompt（使用工具类）
        String schemaDescription = SchemaBuilder.build(metas, tableWhitelist, tableBlacklist);

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

        // 6. 生成 SQL（使用工具类）
        String generatedSql = SqlGenerator.generateSql(chatModel, schemaDescription, userQuery);
        log.info("生成的SQL: {}", generatedSql);

        // 7. 校验 SQL（使用工具类）
        SqlValidator.validate(generatedSql);

        output.addOutput("generatedSql", generatedSql);

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
