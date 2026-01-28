-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
-- Enable zhparser for Chinese full text search (optional, if available in image)
-- CREATE EXTENSION IF NOT EXISTS zhparser;

-- 知识库表
CREATE TABLE km_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT,
    permission_level VARCHAR(50) DEFAULT 'PRIVATE', -- PRIVATE, TEAM, PUBLIC
    status VARCHAR(50) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    update_by VARCHAR(64)
);

COMMENT ON TABLE km_knowledge_base IS '知识库主表';
COMMENT ON COLUMN km_knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN km_knowledge_base.permission_level IS '权限等级';

-- 数据集表 (Logical grouping within KB)
CREATE TABLE km_dataset (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) DEFAULT 'FILE', -- FILE, WEB, MANUL
    config JSONB, -- ETL config specific to this dataset
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64)
);

CREATE INDEX idx_dataset_kb_id ON km_dataset(kb_id);
COMMENT ON TABLE km_dataset IS '数据集表';

-- 文档表
CREATE TABLE km_document (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    original_filename VARCHAR(512),
    file_path VARCHAR(1024), -- Storage path or URL
    file_type VARCHAR(50), -- PDF, TXT, DOCX
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, ERROR
    error_msg TEXT,
    token_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    hash_code VARCHAR(128), -- File hash for de-duplication
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_dataset_id ON km_document(dataset_id);
COMMENT ON TABLE km_document IS '文档表';

-- 向量切片表
CREATE TABLE km_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT,
    metadata JSONB, -- {page: 1, source: "xyz.pdf"}
    embedding vector(1536), -- Default OpenAI embedding size, adjustable
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunk_document_id ON km_document_chunk(document_id);

-- HNSW Index for fast similarity search
-- Note: This might take time on large data. For init, it's fine.
CREATE INDEX idx_chunk_embedding ON km_document_chunk USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE km_document_chunk IS '文档向量切片表';
