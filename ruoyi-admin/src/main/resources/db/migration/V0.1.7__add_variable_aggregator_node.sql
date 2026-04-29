-- ============================================
-- 第一部分：DML 语句（节点定义）
-- ============================================

-- 插入 VARIABLE_AGGREGATOR 节点定义
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color,
    category, description, is_system, is_enabled,
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, "version",
    require_ai_config, require_dialog_config, create_time, update_time
) VALUES (
    1004, 'VARIABLE_AGGREGATOR', '变量聚合器', 'mdi-merge', '#8b5cf6',
    'logic', '将互斥工作流分支（如 IF/ELSE、意图分类器）的输出汇聚为单一输出变量，消除在每条分支上重复配置下游节点的需要。', '0', '1',
    '0', '0',
    '[]',
    '[]',
    1,
    '0', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (node_def_id) DO NOTHING;

-- ============================================
-- 第二部分：DML 语句（连线规则）
-- ============================================

-- 添加 VARIABLE_AGGREGATOR 的连接规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
-- 1. 允许上游节点连接到 VARIABLE_AGGREGATOR
(191, 'CONDITION',           'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(192, 'INTENT_CLASSIFIER',   'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(193, 'LLM_CHAT',            'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(194, 'FIXED_RESPONSE',      'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(195, 'KNOWLEDGE_RETRIEVAL', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(196, 'PARAMETER_EXTRACTOR', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(197, 'TOOL',                'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
(198, 'SKILL',               'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP),
-- 2. 允许 VARIABLE_AGGREGATOR 连接到下游节点
(199, 'VARIABLE_AGGREGATOR', 'LLM_CHAT',            '0', 10, '1', CURRENT_TIMESTAMP),
(200, 'VARIABLE_AGGREGATOR', 'CONDITION',           '0', 10, '1', CURRENT_TIMESTAMP),
(201, 'VARIABLE_AGGREGATOR', 'END',                 '0', 10, '1', CURRENT_TIMESTAMP),
(202, 'VARIABLE_AGGREGATOR', 'FIXED_RESPONSE',      '0', 10, '1', CURRENT_TIMESTAMP),
(203, 'VARIABLE_AGGREGATOR', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP),
(204, 'VARIABLE_AGGREGATOR', 'TOOL',                '0', 10, '1', CURRENT_TIMESTAMP),
(205, 'VARIABLE_AGGREGATOR', 'SKILL',               '0', 10, '1', CURRENT_TIMESTAMP),
(206, 'VARIABLE_AGGREGATOR', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP),
(207, 'VARIABLE_AGGREGATOR', 'LOOP',                '0', 10, '1', CURRENT_TIMESTAMP),
-- 3. 黑名单规则
(208, 'VARIABLE_AGGREGATOR', 'START',               '1', 10, '1', CURRENT_TIMESTAMP),
(209, 'END',                 'VARIABLE_AGGREGATOR', '1', 10, '1', CURRENT_TIMESTAMP),
(210, 'START',               'VARIABLE_AGGREGATOR', '1', 10, '1', CURRENT_TIMESTAMP)
ON CONFLICT (rule_id) DO NOTHING;

