-- 对话界面 / 欢迎页等前端 UI 配置（JSON）
ALTER TABLE km_app ADD COLUMN IF NOT EXISTS ui_setting JSONB DEFAULT NULL;
COMMENT ON COLUMN km_app.ui_setting IS '对话界面/欢迎页等前端 UI 配置';
ALTER TABLE km_chat_session ALTER COLUMN is_resumable SET DEFAULT '0';
