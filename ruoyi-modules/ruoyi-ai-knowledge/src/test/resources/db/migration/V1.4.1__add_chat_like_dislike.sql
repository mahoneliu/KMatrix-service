-- 1.1 在 km_chat_message 表中新增 feedback_status 字段
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS feedback_status int2 DEFAULT 0;
COMMENT ON COLUMN km_chat_message.feedback_status IS '用户反馈状态：0=无评价，1=赞同(Like)，-1=踩(Dislike)';
