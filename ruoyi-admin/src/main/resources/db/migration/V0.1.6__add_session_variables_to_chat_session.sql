-- ============================================
-- V0.1.5: 新增 session_variables 字段到 km_chat_session 表
-- 用于存储会话变量，支持跨对话轮次的状态持久化
-- ============================================

-- DDL: 新增 session_variables JSONB 字段
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS session_variables JSONB DEFAULT '{}';
COMMENT ON COLUMN km_chat_session.session_variables IS '会话变量（JSONB），用于在同一会话的多轮对话间持久化状态';

-- DML: 新增 SESSION_VARIABLE_ASSIGN 节点定义
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color,
    category, description, is_system, is_enabled,
    allow_custom_input_params, allow_custom_output_params,
    require_ai_config, require_dialog_config,
    input_params, output_params, "version",
    create_time, update_time
) VALUES (
    1003, 'SESSION_VARIABLE_ASSIGN', '会话变量赋值', 'mdi-variable', '#f59e0b',
    'action', '对会话变量进行赋值操作，支持覆写、清除、设置三种模式', '0', '1',
    '0', '0',
    '0', '0',
    '[]', '[]', 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (node_def_id) DO NOTHING;

-- 添加 SESSION_VARIABLE_ASSIGN 的连接规则（白名单模式）
-- 允许常用节点连接到 SESSION_VARIABLE_ASSIGN
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (275, 'START', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (276, 'LLM_CHAT', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (277, 'CONDITION', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (278, 'INTENT_CLASSIFIER', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (279, 'KNOWLEDGE_RETRIEVAL', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (280, 'LOOP', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;

-- 允许 SESSION_VARIABLE_ASSIGN 连接到其他节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (281, 'SESSION_VARIABLE_ASSIGN', 'LLM_CHAT', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (282, 'SESSION_VARIABLE_ASSIGN', 'CONDITION', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (283, 'SESSION_VARIABLE_ASSIGN', 'END', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (284, 'SESSION_VARIABLE_ASSIGN', 'INTENT_CLASSIFIER', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (285, 'SESSION_VARIABLE_ASSIGN', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (286, 'SESSION_VARIABLE_ASSIGN', 'FIXED_RESPONSE', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (287, 'SESSION_VARIABLE_ASSIGN', 'SESSION_VARIABLE_ASSIGN', '0', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;

-- 禁止规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (288, 'SESSION_VARIABLE_ASSIGN', 'START', '1', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (289, 'END', 'SESSION_VARIABLE_ASSIGN', '1', 10, '1', CURRENT_TIMESTAMP) ON CONFLICT (rule_id) DO NOTHING;
