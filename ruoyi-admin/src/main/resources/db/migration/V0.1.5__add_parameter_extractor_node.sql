-- ============================================
-- V0.1.5: 新增 PARAMETER_EXTRACTOR 参数提取器节点
-- 使用 LLM 将非结构化文本智能转换为结构化 JSON 参数
-- ============================================

-- 插入 PARAMETER_EXTRACTOR 节点定义
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color,
    category, description, is_system, is_enabled,
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, "version",
    require_ai_config, require_dialog_config, create_time, update_time
) VALUES (
    1002, 'PARAMETER_EXTRACTOR', '参数提取器', 'mdi-code-json', '#f97316',
    'ai', '使用大型语言模型将非结构化文本智能转换为结构化 JSON 参数，弥合自然语言与工具/API 所需结构化输入之间的差距。', '0', '1',
    '0', '1',
    '[{"key":"inputText","label":"输入文本","type":"string","required":true,"description":"待提取参数的非结构化文本内容"}]',
    '[{"key":"extractedJson","label":"提取结果 JSON","type":"string","required":true,"description":"完整的提取结果 JSON 字符串"}]',
    1,
    '1', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- 添加 PARAMETER_EXTRACTOR 的连接规则

-- 1. 允许其他常用节点连接到 PARAMETER_EXTRACTOR
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (175, 'START', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (176, 'LLM_CHAT', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (177, 'CONDITION', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (178, 'FILE_PARSE', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (179, 'KNOWLEDGE_RETRIEVAL', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (180, 'MCP_RESOURCE', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (181, 'LOOP', 'PARAMETER_EXTRACTOR', '0', 10, '1', CURRENT_TIMESTAMP);

-- 2. 允许 PARAMETER_EXTRACTOR 连接到其他节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (182, 'PARAMETER_EXTRACTOR', 'LLM_CHAT', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (183, 'PARAMETER_EXTRACTOR', 'CONDITION', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (184, 'PARAMETER_EXTRACTOR', 'END', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (185, 'PARAMETER_EXTRACTOR', 'TOOL', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (186, 'PARAMETER_EXTRACTOR', 'SKILL', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (187, 'PARAMETER_EXTRACTOR', 'FIXED_RESPONSE', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (188, 'PARAMETER_EXTRACTOR', 'LOOP', '0', 10, '1', CURRENT_TIMESTAMP);

-- 3. 黑名单规则：PARAMETER_EXTRACTOR 不能连接到 START
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (189, 'PARAMETER_EXTRACTOR', 'START', '1', 10, '1', CURRENT_TIMESTAMP);
-- END 不能连接到 PARAMETER_EXTRACTOR
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES (190, 'END', 'PARAMETER_EXTRACTOR', '1', 10, '1', CURRENT_TIMESTAMP);
