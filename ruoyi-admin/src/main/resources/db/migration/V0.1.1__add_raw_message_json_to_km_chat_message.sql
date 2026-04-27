-- V0.1.1: 为 km_chat_message 表增加 raw_message_json 字段
-- 用于存储 LangChain4j ChatMessage 的完整序列化 JSON，支持多模态和工具调用上下文还原

ALTER TABLE km_chat_message
    ADD COLUMN raw_message_json TEXT;

COMMENT ON COLUMN km_chat_message.raw_message_json IS 'LangChain4j ChatMessage 完整序列化JSON，用于精确还原多模态/工具调用历史';
