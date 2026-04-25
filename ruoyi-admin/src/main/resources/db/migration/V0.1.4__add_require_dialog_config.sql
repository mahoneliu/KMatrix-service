-- ============================================
-- V0.1.4: 新增 require_dialog_config 字段
-- 用于标识 AI 节点是否需要对话配置面板（用户提示词/多模态/历史对话）
-- ============================================

-- DDL: 新增字段
ALTER TABLE km_node_definition ADD COLUMN IF NOT EXISTS require_dialog_config VARCHAR(1) DEFAULT '0';
COMMENT ON COLUMN km_node_definition.require_dialog_config IS '是否需要对话配置面板(0-否 1-是)';

-- DML: 更新需要对话配置的 AI 节点
-- LLM_CHAT 是主要的对话节点，INTENT_CLASSIFIER 也使用对话上下文
UPDATE km_node_definition
SET require_dialog_config = '1', update_time = NOW()
WHERE node_type IN ('LLM_CHAT', 'INTENT_CLASSIFIER');
