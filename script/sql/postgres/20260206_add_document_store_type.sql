-- 为 km_document 表添加存储类型字段
-- 用于区分文件存储在 OSS 还是本地文件系统
-- 作者: AI Assistant
-- 日期: 2026-02-06

-- 添加存储类型字段
ALTER TABLE km_document ADD COLUMN store_type INTEGER DEFAULT 1;

-- 添加字段注释
COMMENT ON COLUMN km_document.store_type IS '存储类型: 1-OSS, 2-本地文件';

-- 为现有数据设置默认值(已有数据都是 OSS 存储)
UPDATE km_document SET store_type = 1 WHERE store_type IS NULL;
