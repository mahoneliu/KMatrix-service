-- ============================================
-- V0.1.7: 新增 VARIABLE_AGGREGATOR 变量聚合器节点
-- 将互斥分支（IF/ELSE、意图分类器）的输出汇聚为单一输出变量
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
    '[{"key":"output","label":"聚合输出","type":"string","required":false,"description":"有值的分支变量，若所有分支均未执行则为 null"}]',
    1,
    '0', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (node_def_id) DO NOTHING;

-- 添加 VARIABLE_AGGREGATOR 的连接规则

-- 1. 允许条件/意图分类器分支连接到 VARIABLE_AGGREGATOR（核心使用场景）
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (191, 'CONDITION', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (192, 'INTENT_CLASSIFIER', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (193, 'LLM_CHAT', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (194, 'FIXED_RESPONSE', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (195, 'KNOWLEDGE_RETRIEVAL', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (196, 'PARAMETER_EXTRACTOR', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (197, 'TOOL', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (198, 'SKILL', 'VARIABLE_AGGREGATOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;

-- 2. 允许 VARIABLE_AGGREGATOR 连接到下游节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (199, 'VARIABLE_AGGREGATOR', 'LLM_CHAT', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (200, 'VARIABLE_AGGREGATOR', 'CONDITION', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (201, 'VARIABLE_AGGREGATOR', 'END', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (202, 'VARIABLE_AGGREGATOR', 'FIXED_RESPONSE', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (203, 'VARIABLE_AGGREGATOR', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (204, 'VARIABLE_AGGREGATOR', 'TOOL', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (205, 'VARIABLE_AGGREGATOR', 'SKILL', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (206, 'VARIABLE_AGGREGATOR', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (207, 'VARIABLE_AGGREGATOR', 'LOOP', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;

-- 3. 黑名单规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (208, 'VARIABLE_AGGREGATOR', 'START', '1', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (209, 'END', 'VARIABLE_AGGREGATOR', '1', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (210, 'START', 'VARIABLE_AGGREGATOR', '1', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
