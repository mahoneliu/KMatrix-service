-- ========================================
-- SQL生成节点和SQL执行节点初始化数据
-- 执行顺序: 1.节点定义 -> 2.连接规则
-- ========================================

-- ========================================
-- 1. 插入 SQL_GENERATE 节点定义
-- ========================================
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, version, create_time
) VALUES (
    9,
    'SQL_GENERATE', 
    'SQL生成', 
    'mdi:database-cog', 
    '#8b5cf6',
    'ai', 
    '使用LLM分析用户问题，结合数据库元数据生成SQL语句', 
    '0', 
    '1',
    '0',
    '0',
    '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"description":"用户提出的业务问题"}]',
    '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"description":"LLM生成的SQL语句"}]',
    1,
    NOW()
);

-- ========================================
-- 2. 插入 SQL_EXECUTE 节点定义
-- ========================================
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, version, create_time
) VALUES (
    10,
    'SQL_EXECUTE', 
    'SQL执行', 
    'mdi:database-arrow-right', 
    '#06b6d4',
    'database', 
    '执行SQL语句并返回查询结果', 
    '0', 
    '1',
    '0',
    '0',
    '[{"key":"sql","label":"SQL语句","type":"string","required":true,"description":"待执行的SQL语句"}]',
    '[{"key":"queryResult","label":"查询结果","type":"object","required":true,"description":"SQL执行结果(JSON)"},{"key":"rowCount","label":"返回行数","type":"number","required":true,"description":"查询返回的行数"}]',
    1,
    NOW()
);

-- ========================================
-- 3. 插入 SQL_GENERATE 节点连接规则
-- ========================================
-- SQL_GENERATE 可以从以下节点连入
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(26, 'START', 'SQL_GENERATE', '0', 10, NOW()),
(27, 'LLM_CHAT', 'SQL_GENERATE', '0', 10, NOW()),
(28, 'CONDITION', 'SQL_GENERATE', '0', 10, NOW()),
(29, 'INTENT_CLASSIFIER', 'SQL_GENERATE', '0', 10, NOW());

-- SQL_GENERATE 可以连接到以下节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(30, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, NOW()),
(31, 'SQL_GENERATE', 'END', '0', 10, NOW()),
(32, 'SQL_GENERATE', 'LLM_CHAT', '0', 10, NOW()),
(33, 'SQL_GENERATE', 'CONDITION', '0', 10, NOW()),
(34, 'SQL_GENERATE', 'FIXED_RESPONSE', '0', 10, NOW());

-- ========================================
-- 4. 插入 SQL_EXECUTE 节点连接规则
-- ========================================
-- SQL_EXECUTE 可以从以下节点连入
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(35, 'START', 'SQL_EXECUTE', '0', 10, NOW()),
(36, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, NOW()),
(37, 'LLM_CHAT', 'SQL_EXECUTE', '0', 10, NOW()),
(38, 'CONDITION', 'SQL_EXECUTE', '0', 10, NOW()),
(39, 'INTENT_CLASSIFIER', 'SQL_EXECUTE', '0', 10, NOW());

-- SQL_EXECUTE 可以连接到以下节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(40, 'SQL_EXECUTE', 'END', '0', 10, NOW()),
(41, 'SQL_EXECUTE', 'LLM_CHAT', '0', 10, NOW()),
(42, 'SQL_EXECUTE', 'CONDITION', '0', 10, NOW()),
(43, 'SQL_EXECUTE', 'FIXED_RESPONSE', '0', 10, NOW()),
(44, 'SQL_EXECUTE', 'INTENT_CLASSIFIER', '0', 10, NOW());
