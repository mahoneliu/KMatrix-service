-- 添加模型默认标识字段
-- 日期: 2026-02-04
-- 描述: 为 km_model 表添加 is_default 字段,用于标识系统默认模型

ALTER TABLE km_model ADD COLUMN is_default TINYINT(1) DEFAULT 0 COMMENT '是否为系统默认模型(0-否 1-是)';

-- 创建索引以提高查询性能
CREATE INDEX idx_is_default ON km_model(is_default);

-- 注释: 系统中只能有一个默认的语言模型(model_type='1')
