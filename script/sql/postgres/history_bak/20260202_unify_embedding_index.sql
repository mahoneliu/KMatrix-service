-- ----------------------------
-- KMatrix 知识库增量脚本 (PostgreSQL 版)
-- 日期: 2026-02-02
-- 功能: 统一检索索引 - 将全文检索能力迁移至 km_embedding 表
-- ----------------------------

-- 1. 修改 km_embedding 表结构
-- 增加 text_content 用于存储原始文本 (用于生成向量和高亮)
ALTER TABLE km_embedding ADD COLUMN text_content TEXT;

-- 增加 search_vector 用于全文检索 (自动生成)
ALTER TABLE km_embedding ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('jiebacfg', coalesce(text_content, ''))) STORED;

-- 2. 创建 GIN 索引
CREATE INDEX idx_embedding_search_vector ON km_embedding USING GIN (search_vector);

COMMENT ON COLUMN km_embedding.text_content IS '原始文本内容 (用于全文检索)';
COMMENT ON COLUMN km_embedding.search_vector IS '全文检索向量 (自动生成)';

-- 3. 数据迁移: 同步现有的 Document Chunk 数据
-- source_type: 1 = CONTENT
INSERT INTO km_embedding (kb_id, source_id, source_type, embedding, text_content, create_time)
SELECT kb_id, id, 1, embedding, content, create_time 
FROM km_document_chunk
WHERE kb_id IS NOT NULL
ON CONFLICT DO NOTHING; -- 假设没有唯一约束冲突，或者是新表

-- 4. 数据迁移: 同步现有的 Question 数据
-- source_type: 0 = QUESTION
INSERT INTO km_embedding (kb_id, source_id, source_type, embedding, text_content, create_time)
SELECT kb_id, id, 0, NULL, content, create_time 
FROM km_question
WHERE kb_id IS NOT NULL AND del_flag = '0';
-- 注意: Question 表原本没有向量(embedding列)，这里暂时置为 NULL 或者后续需要补全向量
-- 如果 Question 以前是通过 ETL 生成的 embedding，可能在 km_embedding 里已经有记录了？
-- 根据之前的 SQL，km_question 只是 metadata 表，此时迁移主要是为了全文检索。
-- 如果 km_embedding 里已经有了 Question 的向量(source_type=0)，则应该 UPDATE text_content。

-- 尝试更新已存在的 Question 向量记录 (如果有)
UPDATE km_embedding e
SET text_content = q.content
FROM km_question q
WHERE e.source_id = q.id AND e.source_type = 0 AND e.text_content IS NULL;

-- 插入那些只有问题但还没有向量记录的数据 (纯关键词检索用)
INSERT INTO km_embedding (kb_id, source_id, source_type, embedding, text_content, create_time)
SELECT q.kb_id, q.id, 0, NULL, q.content, q.create_time
FROM km_question q
WHERE NOT EXISTS (
    SELECT 1 FROM km_embedding e WHERE e.source_id = q.id AND e.source_type = 0
);
