-- ----------------------------
-- KMatrix 知识库增量脚本 (PostgreSQL 版)
-- 日期: 2026-02-01
-- 功能: 引入 QA 对表、问题表、统一向量存储表
-- ----------------------------

-- ============================================================
-- 1. 问题表 (km_question)
-- 用于存储从 QA 对导入或由 LLM 自动生成的问题
-- ============================================================
DROP TABLE IF EXISTS km_question CASCADE;
CREATE TABLE km_question (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,              -- 所属知识库ID
    content VARCHAR(500) NOT NULL,      -- 问题内容 (限制255字符)
    hit_num INT DEFAULT 0,              -- 命中次数
    source_type VARCHAR(20) DEFAULT 'IMPORT', -- 来源类型: IMPORT (导入), LLM (LLM生成)
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX idx_question_kb_id ON km_question(kb_id);
COMMENT ON TABLE km_question IS '问题表';
COMMENT ON COLUMN km_question.content IS '问题内容，限制255字符';
COMMENT ON COLUMN km_question.hit_num IS '检索命中次数';
COMMENT ON COLUMN km_question.source_type IS '来源类型: IMPORT (导入), LLM (LLM生成)';

-- ============================================================
-- 2. 问题与分块关联表 (km_question_chunk_map)
-- 一个 Chunk 可以关联多个问题 (1:N)
-- ============================================================
DROP TABLE IF EXISTS km_question_chunk_map CASCADE;
CREATE TABLE km_question_chunk_map (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    CONSTRAINT uk_question_chunk UNIQUE (question_id, chunk_id)
);

CREATE INDEX idx_qcm_question_id ON km_question_chunk_map(question_id);
CREATE INDEX idx_qcm_chunk_id ON km_question_chunk_map(chunk_id);
COMMENT ON TABLE km_question_chunk_map IS '问题与分块关联表';

-- ============================================================
-- 3. 统一向量存储池 (km_embedding)
-- 解耦 chunk 的文本和向量化存储
-- source_type 区分向量来源: QUESTION(0), CONTENT(1), TITLE(2)
-- ============================================================
DROP TABLE IF EXISTS km_embedding CASCADE;
CREATE TABLE km_embedding (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,              -- 所属知识库ID (方便按知识库过滤)
    source_id BIGINT NOT NULL,          -- 关联的源 ID (question_id 或 chunk_id)
    source_type SMALLINT NOT NULL,      -- 来源类型: 0=QUESTION, 1=CONTENT, 2=TITLE
    embedding vector(384),              -- 向量 (使用 all-minilm-l6-v2: 384维)
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_embedding_kb_id ON km_embedding(kb_id);
CREATE INDEX idx_embedding_source ON km_embedding(source_id, source_type);
-- HNSW 向量索引 (余弦相似度)
CREATE INDEX idx_embedding_vector ON km_embedding USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

COMMENT ON TABLE km_embedding IS '统一向量存储池';
COMMENT ON COLUMN km_embedding.source_id IS '关联的源 ID (question_id 或 chunk_id)';
COMMENT ON COLUMN km_embedding.source_type IS '来源类型: 0=QUESTION, 1=CONTENT, 2=TITLE';

-- ============================================================
-- 4. 修改 km_dataset 表，增加处理类型字段
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='km_dataset' AND column_name='process_type') THEN
        ALTER TABLE km_dataset ADD COLUMN process_type VARCHAR(50) DEFAULT 'GENERIC_FILE';
    END IF;
END $$;

COMMENT ON COLUMN km_dataset.process_type IS '处理类型: GENERIC_FILE, QA_PAIR, ONLINE_DOC, WEB_LINK';

-- ============================================================
-- 5. 修改 km_document 表，增加 kb_id 字段 (便于查询)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='km_document' AND column_name='kb_id') THEN
        ALTER TABLE km_document ADD COLUMN kb_id BIGINT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_document_kb_id ON km_document(kb_id);
COMMENT ON COLUMN km_document.kb_id IS '所属知识库ID (冗余字段，便于查询)';

-- ============================================================
-- 6. 修改 km_document_chunk 表: 增加 kb_id, title 字段
-- (embedding 字段保留以兼容旧数据，后续迁移到 km_embedding)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='km_document_chunk' AND column_name='kb_id') THEN
        ALTER TABLE km_document_chunk ADD COLUMN kb_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='km_document_chunk' AND column_name='title') THEN
        ALTER TABLE km_document_chunk ADD COLUMN title VARCHAR(500);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='km_document_chunk' AND column_name='parent_chain') THEN
        ALTER TABLE km_document_chunk ADD COLUMN parent_chain TEXT; -- JSON array for tree structure
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_chunk_kb_id ON km_document_chunk(kb_id);
COMMENT ON COLUMN km_document_chunk.kb_id IS '所属知识库ID (冗余字段)';
COMMENT ON COLUMN km_document_chunk.title IS '分块标题 (用于标题向量化)';
COMMENT ON COLUMN km_document_chunk.parent_chain IS '父级标题链路 (JSON数组)';

-- ============================================================
-- End of Migration Script
-- ============================================================
