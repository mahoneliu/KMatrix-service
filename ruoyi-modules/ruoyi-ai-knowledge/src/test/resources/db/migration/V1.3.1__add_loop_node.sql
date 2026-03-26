-- V1.3.2: 新增循环节点 LOOP 及其连接规则

-- ----------------------------
-- 新增加循环节点至工作流定义表
-- ----------------------------
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    14, 'LOOP', '循环节点', 'mdi-sync', '#8b5cf6', 
    'logic', '用于执行一个循环流程，直到满足条件就跳出或结束', '0', '1', 
    '0', '0', 
    '[]', 
    '[]', 
    1, NOW(), NOW()
) on conflict (node_def_id) do nothing;

-- ----------------------------
-- LOOP 节点的连接规则
-- ----------------------------
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
(140, 'START', 'LOOP', '0', 10, '1', NOW()),
(141, 'LLM_CHAT', 'LOOP', '0', 10, '1', NOW()),
(142, 'INTENT_CLASSIFIER', 'LOOP', '0', 10, '1', NOW()),
(143, 'CONDITION', 'LOOP', '0', 10, '1', NOW()),
(144, 'DB_QUERY', 'LOOP', '0', 10, '1', NOW()),
(145, 'SQL_GENERATE', 'LOOP', '0', 10, '1', NOW()),
(146, 'SQL_EXECUTE', 'LOOP', '0', 10, '1', NOW()),
(147, 'KNOWLEDGE_RETRIEVAL', 'LOOP', '0', 10, '1', NOW()),
(148, 'TOOL', 'LOOP', '0', 10, '1', NOW()),
(149, 'SKILL', 'LOOP', '0', 10, '1', NOW()),
(150, 'LOOP', 'LOOP', '0', 10, '1', NOW()),
(151, 'LOOP', 'TOOL', '0', 10, '1', NOW()),
(152, 'LOOP', 'LLM_CHAT', '0', 10, '1', NOW()),
(153, 'LOOP', 'INTENT_CLASSIFIER', '0', 10, '1', NOW()),
(154, 'LOOP', 'CONDITION', '0', 10, '1', NOW()),
(155, 'LOOP', 'DB_QUERY', '0', 10, '1', NOW()),
(156, 'LOOP', 'SQL_GENERATE', '0', 10, '1', NOW()),
(157, 'LOOP', 'SQL_EXECUTE', '0', 10, '1', NOW()),
(158, 'LOOP', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', NOW()),
(159, 'LOOP', 'FIXED_RESPONSE', '0', 10, '1', NOW()),
(160, 'LOOP', 'END', '0', 10, '1', NOW()),
(161, 'LOOP', 'SKILL', '0', 10, '1', NOW()),
(162, 'FIXED_RESPONSE', 'LOOP', '0', 10, '1', NOW()) on conflict (rule_id) do nothing;
