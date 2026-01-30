package org.dromara.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.ai.mapper.KmDataSourceMapper;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowNode;
import org.dromara.ai.workflow.nodes.nodeUtils.SqlExecutor;
import org.dromara.ai.workflow.nodes.nodeUtils.SqlValidator;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * SQL执行节点
 * 执行SQL语句并返回查询结果
 *
 * @author Mahone
 * @date 2026-01-24
 */
@Slf4j
@RequiredArgsConstructor
@Component("SQL_EXECUTE")
public class SqlExecuteNode implements WorkflowNode {

    private final KmDataSourceMapper dataSourceMapper;
    private final SqlExecutor sqlExecutor;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行SQL_EXECUTE节点");

        NodeOutput output = new NodeOutput();

        // 1. 获取配置参数
        Long dataSourceId = context.getConfigAsLong("dataSourceId");
        Integer maxRows = context.getConfigAsInteger("maxRows", 100);

        // 2. 获取输入参数
        String sql = (String) context.getInput("sql");
        if (StrUtil.isBlank(sql)) {
            throw new RuntimeException("sql不能为空");
        }

        // 3. 校验 SQL 安全性（使用工具类）
        SqlValidator.validate(sql);

        // 4. 加载数据源
        KmDataSource dataSource = dataSourceMapper.selectById(dataSourceId);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在: " + dataSourceId);
        }

        // 5. 执行 SQL（使用工具类）
        List<Map<String, Object>> queryResult = sqlExecutor.executeQuery(dataSource, sql, maxRows);
        output.addOutput("queryResult", queryResult);
        output.addOutput("strResult", JsonUtils.toJsonString(queryResult));
        output.addOutput("rowCount", queryResult.size());
        log.info("查询结果行数: {}", queryResult.size());

        log.info("SQL_EXECUTE节点执行完成");
        return output;
    }

    @Override
    public String getNodeType() {
        return "SQL_EXECUTE";
    }

    @Override
    public String getNodeName() {
        return "SQL执行";
    }
}
