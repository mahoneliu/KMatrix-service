-- =====================================
-- 数据库创建验证SQL
-- =====================================

-- 1. 检查表是否存在
SHOW TABLES LIKE 'km_node%';
SHOW TABLES LIKE 'km_workflow_template';

-- 2. 检查节点定义数据
SELECT 
    node_type, 
    node_label, 
    category,
    is_system,
    is_enabled,
    version
FROM km_node_definition
ORDER BY category, node_type;

-- 预期结果：7条记录
-- APP_INFO (basic), START (basic), END (basic)
-- LLM_CHAT (ai), INTENT_CLASSIFIER (ai)
-- CONDITION (logic)
-- FIXED_RESPONSE (action)

-- 3. 检查连接规则数据
SELECT 
    source_node_type, 
    target_node_type,
    rule_type,
    priority,
    is_enabled
FROM km_node_connection_rule
ORDER BY rule_id;

-- 预期结果：16条记录

-- 4. 统计信息
SELECT 
    '节点定义总数' AS item, 
    COUNT(*) AS count 
FROM km_node_definition
UNION ALL
SELECT 
    '连接规则总数' AS item, 
    COUNT(*) AS count 
FROM km_node_connection_rule
UNION ALL
SELECT 
    '工作流模板总数' AS item, 
    COUNT(*) AS count 
FROM km_workflow_template;

-- 预期结果：
-- 节点定义总数: 7
-- 连接规则总数: 16
-- 工作流模板总数: 0

-- 5. 检查节点参数定义（JSON字段）
SELECT 
    node_type,
    node_label,
    JSON_LENGTH(input_params) AS input_param_count,
    JSON_LENGTH(output_params) AS output_param_count
FROM km_node_definition
ORDER BY category;

-- 6. 检查特定节点的详细信息（以START节点为例）
SELECT 
    node_type,
    node_label,
    input_params,
    output_params
FROM km_node_definition
WHERE node_type = 'START';

-- 7. 检查连接规则 - START节点可以连接到哪些节点
SELECT 
    source_node_type,
    target_node_type,
    CASE rule_type 
        WHEN '0' THEN '允许' 
        WHEN '1' THEN '禁止' 
    END AS rule_desc
FROM km_node_connection_rule
WHERE source_node_type = 'START'
ORDER BY target_node_type;

-- 预期：START可以连接到 LLM_CHAT, INTENT_CLASSIFIER, CONDITION, FIXED_RESPONSE

-- =====================================
-- 如果数据有问题，可以使用以下SQL清理重建
-- =====================================
-- TRUNCATE TABLE km_node_definition;
-- TRUNCATE TABLE km_node_connection_rule;
-- TRUNCATE TABLE km_workflow_template;
-- 然后重新执行 km_workflow_node_complete.sql
