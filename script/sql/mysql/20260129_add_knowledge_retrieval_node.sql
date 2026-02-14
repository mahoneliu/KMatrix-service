-- ========================================
-- 知识检索节点(KNOWLEDGE_RETRIEVAL)初始化数据
-- 执行顺序: 1.节点定义 -> 2.连接规则
-- ========================================

-- ========================================
-- 1. 插入 KNOWLEDGE_RETRIEVAL 节点定义
-- ========================================
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, version, create_time
) VALUES (
    11,
    'KNOWLEDGE_RETRIEVAL', 
    '知识检索', 
    'mdi:book-search', 
    '#f59e0b',
    'ai', 
    '从知识库中检索相关文档片段，用于RAG对话', 
    '0', 
    '1',
    '0',
    '0',
    '[{"key":"query","label":"查询文本","type":"string","required":true,"description":"用于检索的查询文本"}]',
    '[{"key":"context","label":"检索上下文","type":"string","required":true,"description":"拼接后的上下文文本"},{"key":"docCount","label":"文档数量","type":"number","required":true,"description":"检索到的文档数量"},{"key":"retrievedDocs","label":"检索结果","type":"array","required":true,"description":"检索到的文档片段列表"}]',
    1,
    NOW()
);

-- ========================================
-- 2. 插入 KNOWLEDGE_RETRIEVAL 节点连接规则
-- ========================================
-- KNOWLEDGE_RETRIEVAL 可以从以下节点连入
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(50, 'START', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(51, 'LLM_CHAT', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(52, 'CONDITION', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(53, 'INTENT_CLASSIFIER', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW());

-- KNOWLEDGE_RETRIEVAL 可以连接到以下节点
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(54, 'KNOWLEDGE_RETRIEVAL', 'LLM_CHAT', '0', 10, NOW()),
(55, 'KNOWLEDGE_RETRIEVAL', 'CONDITION', '0', 10, NOW()),
(56, 'KNOWLEDGE_RETRIEVAL', 'END', '0', 10, NOW());
