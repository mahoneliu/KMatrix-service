-- Phase 1: 知识库功能增强 - 数据库Schema变更
-- 执行日期: 2026-02-01

-- 1. km_dataset 表: 新增 allowed_file_types 字段
ALTER TABLE km_dataset ADD COLUMN allowed_file_types VARCHAR(255);
COMMENT ON COLUMN km_dataset.allowed_file_types IS '支持的文件格式(逗号分隔,*表示全部)';

-- 2. km_document 表: 新增 title, content, url 字段
ALTER TABLE km_document 
    ADD COLUMN title VARCHAR(500),
    ADD COLUMN content TEXT,
    ADD COLUMN url VARCHAR(1000);

COMMENT ON COLUMN km_document.title IS '文档标题(用于向量化)';
COMMENT ON COLUMN km_document.content IS '在线文档内容(富文本HTML)';
COMMENT ON COLUMN km_document.url IS '网页链接URL';

-- 3. 更新现有系统预设数据集的 allowed_file_types
UPDATE km_dataset 
SET allowed_file_types = '*' 
WHERE process_type = 'GENERIC_FILE' AND is_system = true;

UPDATE km_dataset 
SET allowed_file_types = 'xlsx,xls,csv' 
WHERE process_type = 'QA_PAIR' AND is_system = true;