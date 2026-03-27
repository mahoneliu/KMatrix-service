-- ======================================================================
-- Flyway Migration: V0.0.2
-- Description: Add session resume fields to km_chat_session table
-- Author: KMatrix AI Assistant
-- Date: 2026-03-28
-- ======================================================================

-- Add session resume-related columns to km_chat_session table
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS abort_reason VARCHAR(50);
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS abort_exception_type VARCHAR(255);
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS abort_exception_message TEXT;
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS abort_exception_stacktrace TEXT;
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS abort_timestamp TIMESTAMP;
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS last_message_id BIGINT;
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS is_resumable CHAR(1) DEFAULT '1';
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS resume_token VARCHAR(100);
ALTER TABLE km_chat_session ADD COLUMN IF NOT EXISTS resumed_at TIMESTAMP;

-- Add comments for new columns
COMMENT ON COLUMN km_chat_session.abort_reason IS '中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误';
COMMENT ON COLUMN km_chat_session.abort_exception_type IS '异常类型（仅当中断原因为exception时有值）';
COMMENT ON COLUMN km_chat_session.abort_exception_message IS '异常消息（仅当中断原因为exception时有值）';
COMMENT ON COLUMN km_chat_session.abort_exception_stacktrace IS '异常堆栈信息（仅当中断原因为exception时有值）';
COMMENT ON COLUMN km_chat_session.abort_timestamp IS '会话被中断的时间戳';
COMMENT ON COLUMN km_chat_session.last_message_id IS '中断时最后一条消息的ID';
COMMENT ON COLUMN km_chat_session.is_resumable IS '会话是否可恢复（0=不可恢复, 1=可恢复）';
COMMENT ON COLUMN km_chat_session.resume_token IS '恢复令牌，用于防止重复恢复';
COMMENT ON COLUMN km_chat_session.resumed_at IS '会话被恢复的时间戳';

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_km_chat_session_abort_reason ON km_chat_session(abort_reason);
CREATE INDEX IF NOT EXISTS idx_km_chat_session_is_resumable ON km_chat_session(is_resumable);
CREATE INDEX IF NOT EXISTS idx_km_chat_session_resume_token ON km_chat_session(resume_token);
CREATE INDEX IF NOT EXISTS idx_km_chat_session_abort_timestamp ON km_chat_session(abort_timestamp);
CREATE INDEX IF NOT EXISTS idx_km_chat_session_user_id ON km_chat_session(user_id);
