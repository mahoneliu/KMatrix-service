-- ----------------------------
-- KMatrix 知识库模块数据库脚本 (PostgreSQL 版)
-- ----------------------------

-- 1. 扩展配置 (优先尝试 pg_jieba，失败可切换 pg_trgm)
CREATE EXTENSION IF NOT EXISTS pg_jieba; -- 如环境支持 jieba，请取消注释
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- 通用备选方案
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 知识库主表
DROP TABLE IF EXISTS km_knowledge_base;
CREATE TABLE km_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT,
    permission_level VARCHAR(50) DEFAULT 'PRIVATE', -- PRIVATE, TEAM, PUBLIC
    status VARCHAR(50) DEFAULT 'ACTIVE',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
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
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX idx_dataset_kb_id ON km_dataset(kb_id);
COMMENT ON TABLE km_dataset IS '数据集表';

-- 文档表
CREATE TABLE km_document (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    original_filename VARCHAR(512),
    file_path VARCHAR(1024), -- Storage path or URL
    oss_id BIGINT, -- OSS file ID
    file_type VARCHAR(50), -- PDF, TXT, DOCX
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, ERROR
    error_msg TEXT,
    token_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    hash_code VARCHAR(128), -- File hash for de-duplication
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX idx_document_dataset_id ON km_document(dataset_id);
COMMENT ON TABLE km_document IS '文档表';
COMMENT ON COLUMN km_document.oss_id IS 'OSS文件ID';

-- 向量切片表
CREATE TABLE km_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT,
    metadata JSONB, -- {page: 1, source: "xyz.pdf"}
    -- embedding vector(384), -- use all-minilm-l6-v2 dim
    -- Generated column for full-text search (pre-computed tsvector)
    content_search_vector tsvector GENERATED ALWAYS AS (to_tsvector('jiebacfg', content)) STORED,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunk_document_id ON km_document_chunk(document_id);
-- GIN Index for fast full-text search
CREATE INDEX idx_chunk_search_vector ON km_document_chunk USING GIN (content_search_vector);

-- HNSW Index for fast similarity search
-- Note: This might take time on large data. For init, it's fine.
-- 针对 384 维向量的余弦相似度索引（下面优化sql）
-- CREATE INDEX idx_chunk_embedding ON km_document_chunk USING hnsw (embedding vector_cosine_ops);
-- CREATE INDEX idx_chunk_embedding ON km_document_chunk USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

COMMENT ON TABLE km_document_chunk IS '文档向量切片表';

-- ----------------------------
-- 5. 初始化菜单数据
-- ----------------------------
-- 知识库管理 (2018)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2018, '知识库管理', 2000, 5, 'knowledge-manager', 'ai/knowledge-manager/index', 1, 0, 'C', '0', '0', 'ai:knowledge:list', 'mdi:database', 1, NOW())
ON CONFLICT (menu_id) DO NOTHING;

-- 知识库详情 (2019) - 隐藏路由
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2019, '知识库详情', 2000, 6, 'knowledge-detail', 'ai/knowledge-detail/index', 1, 0, 'C', '1', '0', 'ai:knowledge:view', 'mdi:database-search', 1, NOW())
ON CONFLICT (menu_id) DO NOTHING;

-- ----------------------------
-- 6. 初始化角色权限
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2018 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2018);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2019 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2019);
