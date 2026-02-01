-- ----------------------------
-- KMatrix 知识库增量脚本 (PostgreSQL 版)
-- 日期: 2026-02-02
-- 功能: 为 km_question 表增加全文检索字段
-- ----------------------------

-- 1. 增加 content_search_vector 字段 (自动生成)
ALTER TABLE km_question 
ADD COLUMN content_search_vector tsvector GENERATED ALWAYS AS (to_tsvector('jiebacfg', content)) STORED;

-- 2. 创建 GIN 索引
CREATE INDEX idx_question_search_vector ON km_question USING GIN (content_search_vector);

COMMENT ON COLUMN km_question.content_search_vector IS '全文检索向量 (自动生成)';
