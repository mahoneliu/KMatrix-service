-- ----------------------------
-- 工作流节点定义初始化数据
-- ----------------------------

-- 清空表（如果需要重新初始化）
-- TRUNCATE TABLE km_node_definition;
-- TRUNCATE TABLE km_node_connection_rule;

-- ----------------------------
-- 初始化节点定义数据 (来自 node-definitions.json)
-- ----------------------------

-- 1. APP_INFO节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (1, 'APP_INFO', '基础信息', 'mdi:information', '#10b981', 'basic', '应用的基础信息配置', '1', '1', '[]', '[]', 1, NOW());

-- 2. START节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version,create_time)
VALUES (2, 'START', '开始', 'mdi:play-circle', '#10b981', 'basic', '工作流的入口节点', '1', '1', '[]', 
'[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"用户的输入内容"},{"key":"sessionId","label":"会话ID","type":"string","required":true,"description":"当前会话的唯一标识"},{"key":"userId","label":"用户ID","type":"string","required":false,"description":"当前用户的ID"}]',
1, NOW());

-- 3. END节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (3, 'END', '结束', 'mdi:stop-circle', '#ef4444', 'basic', '工作流的结束节点', '1', '1',
'[{"key":"response","label":"最终回复","type":"string","required":true,"description":"返回给用户的最终回复内容"}]',
'[]', 1, NOW());

-- 4. LLM_CHAT节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (4, 'LLM_CHAT', 'LLM 对话', 'mdi:robot', '#3b82f6', 'ai', '调用大语言模型进行对话', '0', '1',
'[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"传递给 LLM 的用户输入"},{"key":"systemPrompt","label":"系统提示词","type":"string","required":false,"description":"可选的系统提示词,用于覆盖节点配置"}]',
'[{"key":"response","label":"AI 回复","type":"string","required":true,"description":"LLM 生成的回复内容"}]',
1, NOW());

-- 5. INTENT_CLASSIFIER节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (5, 'INTENT_CLASSIFIER', '意图分类', 'mdi:sitemap', '#8b5cf6', 'ai', '识别用户输入的意图并分类', '0', '1', 
'[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"需要分类的用户输入"}]',
'[{"key":"matchedIntent","label":"匹配的意图","type":"string","required":true,"description":"识别出的意图名称"},{"key":"confidence","label":"置信度","type":"number","required":true,"description":"意图识别的置信度(0-1)"}]',
1, NOW());

-- 6. CONDITION节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (6, 'CONDITION', '条件判断', 'mdi:source-branch', '#f59e0b', 'logic', '根据条件表达式进行分支判断', '0', '1',
'[{"key":"matchedBranch","label":"匹配的分支","type":"string","required":false,"description":"用于条件判断的值"}]',
'[{"key":"matchedBranch","label":"匹配的分支","type":"string","required":true,"description":"满足条件的分支名称"}]',
1, NOW());

-- 7. FIXED_RESPONSE节点
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, input_params, output_params, version, create_time)
VALUES (7, 'FIXED_RESPONSE', '固定回复', 'mdi:message-text', '#6b7280', 'action', '返回预设的固定文本内容', '0', '1',
'[]',
'[{"key":"response","label":"回复内容","type":"string","required":true,"description":"固定的回复文本"}]',
1, NOW());

-- ----------------------------
-- 初始化节点连接规则数据 (来自 connection-rules.ts)
-- ----------------------------

-- START 节点允许连接的目标节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(1, 'START', 'LLM_CHAT', '0', 10, NOW()),
(2, 'START', 'INTENT_CLASSIFIER', '0', 10, NOW()),
(3, 'START', 'CONDITION', '0', 10, NOW()),
(4, 'START', 'FIXED_RESPONSE', '0', 10, NOW());

-- LLM_CHAT 节点允许连接的目标节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(5, 'LLM_CHAT', 'END', '0', 10, NOW()),
(6, 'LLM_CHAT', 'LLM_CHAT', '0', 10, NOW()),
(7, 'LLM_CHAT', 'CONDITION', '0', 10, NOW()),
(8, 'LLM_CHAT', 'FIXED_RESPONSE', '0', 10, NOW());

-- INTENT_CLASSIFIER 节点允许连接的目标节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(9, 'INTENT_CLASSIFIER', 'LLM_CHAT', '0', 10, NOW()),
(10, 'INTENT_CLASSIFIER', 'CONDITION', '0', 10, NOW()),
(11, 'INTENT_CLASSIFIER', 'FIXED_RESPONSE', '0', 10, NOW()),
(12, 'INTENT_CLASSIFIER', 'END', '0', 10, NOW());

-- CONDITION 节点允许连接的目标节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(13, 'CONDITION', 'LLM_CHAT', '0', 10, NOW()),
(14, 'CONDITION', 'FIXED_RESPONSE', '0', 10, NOW()),
(15, 'CONDITION', 'END', '0', 10, NOW());

-- FIXED_RESPONSE 节点只能连接到 END
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(16, 'FIXED_RESPONSE', 'END', '0', 10, NOW());

-- END 和 APP_INFO 节点不允许连接到任何节点（不需要插入记录）
