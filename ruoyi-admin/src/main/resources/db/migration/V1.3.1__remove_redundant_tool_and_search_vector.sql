-- V1.3.1: 移除冗余表 km_tool 和冗余字段 km_question.content_search_vector

-- 1. 移除冗余表 km_tool
DROP TABLE IF EXISTS km_tool;

-- 2. 移除 km_question 表中的冗余全文搜索向量字段
ALTER TABLE km_question DROP COLUMN IF EXISTS content_search_vector;
