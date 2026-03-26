-- 1. 为 km_app_access_stat 增加统计字段
ALTER TABLE km_app_access_stat ADD COLUMN IF NOT EXISTS token_count int8 DEFAULT 0;
ALTER TABLE km_app_access_stat ADD COLUMN IF NOT EXISTS like_count int8 DEFAULT 0;
ALTER TABLE km_app_access_stat ADD COLUMN IF NOT EXISTS dislike_count int8 DEFAULT 0;
ALTER TABLE km_app_access_stat ADD COLUMN IF NOT EXISTS question_count int8 DEFAULT 0;

-- 2. 添加注释
COMMENT ON COLUMN km_app_access_stat.token_count IS '消耗的 Token 总数';
COMMENT ON COLUMN km_app_access_stat.like_count IS '点赞次数';
COMMENT ON COLUMN km_app_access_stat.dislike_count IS '点踩次数';
COMMENT ON COLUMN km_app_access_stat.question_count IS '提问次数';

-- 3. 创建索引优化查询和支持 Upsert
CREATE INDEX IF NOT EXISTS idx_km_app_access_stat_app_id ON km_app_access_stat (app_id);
-- 如果已存在重复数据，需要先清理（正常业务下 appId+userId 应唯一）
CREATE UNIQUE INDEX IF NOT EXISTS uk_km_app_access_stat_app_user ON km_app_access_stat (app_id, user_id);

-- 4. 在 km_chat_message 表中新增 total_tokens 字段
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS total_tokens int DEFAULT 0;
COMMENT ON COLUMN km_chat_message.total_tokens IS '该条消息或会话周期内消耗的 Token 总数';
