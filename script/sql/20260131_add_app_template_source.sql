-- ============================================
-- km_app 表新增模版来源字段
-- 执行日期: 2026-01-31
-- ============================================

-- 新增来源模版ID字段
ALTER TABLE km_app ADD COLUMN IF NOT EXISTS source_template_id BIGINT DEFAULT NULL;

-- 新增来源模版类型字段 (0=系统模版, 1=自建模版)
ALTER TABLE km_app ADD COLUMN IF NOT EXISTS source_template_scope CHAR(1) DEFAULT NULL;

-- 添加字段注释
COMMENT ON COLUMN km_app.source_template_id IS '来源模版ID';
COMMENT ON COLUMN km_app.source_template_scope IS '来源模版类型(0系统/1自建)';

-- 创建索引 (可选，用于按模版查询应用)
CREATE INDEX IF NOT EXISTS idx_km_app_source_template ON km_app(source_template_id);
