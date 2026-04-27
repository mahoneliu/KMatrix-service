-- 对话界面 / 欢迎页等前端 UI 配置（JSON）
ALTER TABLE km_app ADD COLUMN IF NOT EXISTS ui_setting JSONB DEFAULT NULL;
COMMENT ON COLUMN km_app.ui_setting IS '对话界面/欢迎页等前端 UI 配置';
ALTER TABLE km_chat_session ALTER COLUMN is_resumable SET DEFAULT '0';

-- 为 LLM_CHAT 修正默认入参定义
UPDATE km_node_definition
SET input_params = '[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"传递给 LLM 的用户输入"},{"key": "chatContext", "type": "string", "label": "上下文", "required": false, "description": "比如可以传递知识库的检索结果", "defaultValue": ""}, {"key": "retrievedDocs", "type": "array", "label": "知识检索结果记录", "required": false, "description": "知识检索结果记录列表", "defaultValue": ""},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"图片或音频的 OSS ID 列表"}]'
WHERE node_type = 'LLM_CHAT';
