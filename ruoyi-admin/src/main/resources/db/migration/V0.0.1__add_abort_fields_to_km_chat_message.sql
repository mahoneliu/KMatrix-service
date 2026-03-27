-- ======================================================================
-- Flyway Migration: V0.0.1
-- Description: Add abort/interrupt fields to km_chat_message table
-- Author: KMatrix AI Assistant
-- Date: 2026-03-28
-- ======================================================================

-- Add abort-related columns to km_chat_message table
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS abort_status VARCHAR(20) DEFAULT 'none';
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS partial_content TEXT;
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS abort_time TIMESTAMP;
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);
ALTER TABLE km_chat_message ADD COLUMN IF NOT EXISTS abort_reason VARCHAR(50);

-- Add comments for new columns
COMMENT ON COLUMN km_chat_message.abort_status IS '中断状态: none=未中断, aborted=已中断';
COMMENT ON COLUMN km_chat_message.partial_content IS '请求被中断时已生成的部分内容';
COMMENT ON COLUMN km_chat_message.abort_time IS '请求被中断的时间';
COMMENT ON COLUMN km_chat_message.request_id IS '请求的唯一标识符';
COMMENT ON COLUMN km_chat_message.abort_reason IS '中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误';

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_km_chat_message_request_id ON km_chat_message(request_id);
CREATE INDEX IF NOT EXISTS idx_km_chat_message_abort_status ON km_chat_message(abort_status);
CREATE INDEX IF NOT EXISTS idx_km_chat_message_session_id ON km_chat_message(session_id);
