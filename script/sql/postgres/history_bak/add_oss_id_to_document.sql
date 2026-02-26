-- 为 km_document 表添加 oss_id 字段
-- 执行日期: 2026-01-29

ALTER TABLE km_document ADD COLUMN IF NOT EXISTS oss_id BIGINT;

COMMENT ON COLUMN km_document.oss_id IS 'OSS文件ID';

-- 如果之前 file_path 存储的是 OSS ID，需要迁移数据
-- UPDATE km_document SET oss_id = file_path::bigint WHERE file_path ~ '^[0-9]+$';
