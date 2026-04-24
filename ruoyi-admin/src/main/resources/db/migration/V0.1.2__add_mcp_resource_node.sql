-- 插入 MCP_RESOURCE 节点定义
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, "version", 
    require_ai_config, create_time, update_time
) VALUES (
    1001, 'MCP_RESOURCE', 'MCP 资源读取', 'mdi-server-network', '#4ade80', 
    'tool', '从指定的 MCP Server 动态读取资源并注入工作流上下文中。', '0', '1', 
    '1', '1', 
    '[{"key":"uri","label":"资源 URI","type":"string","required":true,"description":"MCP 资源的唯一标识符"}]', 
    '[{"key":"content","label":"资源原始内容","type":"object","required":true,"description":"读取的资源内容对象"},{"key":"textContent","label":"提取的纯文本","type":"string","required":false,"description":"如果资源包含纯文本内容，将提取为字符串"}]', 
    1, 
    '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- 添加 MCP_RESOURCE 的连接规则 (白名单模式下允许的连接，rule_type='0')

-- 1. 允许其他常用节点连接到 MCP_RESOURCE
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('START', 'MCP_RESOURCE', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('LLM_CHAT', 'MCP_RESOURCE', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('CONDITION', 'MCP_RESOURCE', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('INTENT_CLASSIFIER', 'MCP_RESOURCE', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('LOOP', 'MCP_RESOURCE', '0', 10, '1', CURRENT_TIMESTAMP);

-- 2. 允许 MCP_RESOURCE 连接到其他节点
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'LLM_CHAT', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'CONDITION', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'END', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'INTENT_CLASSIFIER', '0', 10, '1', CURRENT_TIMESTAMP);
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'LOOP', '0', 10, '1', CURRENT_TIMESTAMP);

-- 3. 添加黑名单模式的默认规则 (rule_type='1')
-- MCP_RESOURCE 不能连接到 START
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('MCP_RESOURCE', 'START', '1', 10, '1', CURRENT_TIMESTAMP);
-- END 不能连接到 MCP_RESOURCE
INSERT INTO km_node_connection_rule (source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES ('END', 'MCP_RESOURCE', '1', 10, '1', CURRENT_TIMESTAMP);
