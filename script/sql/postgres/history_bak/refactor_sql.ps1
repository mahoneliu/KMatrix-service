$ErrorActionPreference = "Stop"
$filePath = "f:\code\KMatrix\kmatrix-service\script\sql\postgres\kmatrix_complete.sql"
$tempPath = $filePath + ".new"

Write-Host "Reading $filePath..."
$lines = Get-Content -Path $filePath -Encoding UTF8

# Define New KB Schema
$kbSchema = @"
-- ----------------------------
-- 2. 知识库主表
-- ----------------------------
DROP TABLE IF EXISTS km_knowledge_base CASCADE;
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

-- ----------------------------
-- 3. 数据集表
-- ----------------------------
DROP TABLE IF EXISTS km_dataset CASCADE;
CREATE TABLE km_dataset (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) DEFAULT 'FILE', -- FILE, WEB, MANUAL
    config JSONB,
    allowed_file_types VARCHAR(255),
    process_type VARCHAR(50) DEFAULT 'GENERIC_FILE',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);
CREATE INDEX idx_dataset_kb_id ON km_dataset(kb_id);
COMMENT ON TABLE km_dataset IS '数据集表';
COMMENT ON COLUMN km_dataset.process_type IS '处理类型';

-- ----------------------------
-- 4. 文档表
-- ----------------------------
DROP TABLE IF EXISTS km_document CASCADE;
CREATE TABLE km_document (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    kb_id BIGINT,              -- 冗余字段，便于查询
    original_filename VARCHAR(512),
    file_path VARCHAR(1024),
    oss_id BIGINT,             -- OSS文件ID
    file_type VARCHAR(50),
    file_size BIGINT,
    error_msg TEXT,
    token_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    hash_code VARCHAR(128),
    store_type INTEGER DEFAULT 1, -- 1-OSS, 2-本地文件
    enabled INTEGER DEFAULT 1,    -- 0=禁用, 1=启用
    embedding_status INTEGER DEFAULT 0, -- 0=未生成, 1=生成中, 2=已生成, 3=生成失败
    question_status INTEGER DEFAULT 0,  -- 0=未生成, 1=生成中, 2=已生成, 3=生成失败
    status_meta JSONB,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);
CREATE INDEX idx_document_dataset_id ON km_document(dataset_id);
CREATE INDEX idx_document_kb_id ON km_document(kb_id);
COMMENT ON TABLE km_document IS '文档表';

-- ----------------------------
-- 5. 文档分块表
-- ----------------------------
DROP TABLE IF EXISTS km_document_chunk CASCADE;
CREATE TABLE km_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    kb_id BIGINT,              -- 冗余字段
    title VARCHAR(500),        -- 分块标题
    content TEXT,
    metadata JSONB,
    parent_chain TEXT,         -- 父级标题链路 JSON array
    enabled INTEGER DEFAULT 1,
    embedding_status INTEGER DEFAULT 0,
    question_status INTEGER DEFAULT 0,
    status_meta JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chunk_document_id ON km_document_chunk(document_id);
CREATE INDEX idx_chunk_kb_id ON km_document_chunk(kb_id);
COMMENT ON TABLE km_document_chunk IS '文档分块表';

-- ----------------------------
-- 6. 问题表
-- ----------------------------
DROP TABLE IF EXISTS km_question CASCADE;
CREATE TABLE km_question (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    hit_num INT DEFAULT 0,
    source_type VARCHAR(20) DEFAULT 'IMPORT',
    content_search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);
CREATE INDEX idx_question_kb_id ON km_question(kb_id);
CREATE INDEX idx_question_search_vector ON km_question USING GIN (content_search_vector);
COMMENT ON TABLE km_question IS '问题表';

-- ----------------------------
-- 7. 问题分块关联表
-- ----------------------------
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

-- ----------------------------
-- 8. 统一向量存储表
-- ----------------------------
DROP TABLE IF EXISTS km_embedding CASCADE;
CREATE TABLE km_embedding (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_type SMALLINT NOT NULL,  -- 0=QUESTION, 1=CONTENT, 2=TITLE
    embedding vector(512),
    text_content TEXT,
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(text_content, ''))) STORED,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_embedding_kb_id ON km_embedding(kb_id);
CREATE INDEX idx_embedding_source ON km_embedding(source_id, source_type);
CREATE INDEX idx_embedding_vector ON km_embedding USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX idx_embedding_search_vector ON km_embedding USING GIN (search_vector);
COMMENT ON TABLE km_embedding IS '统一向量存储表';

-- ----------------------------
-- 9. 临时文件表
-- ----------------------------
DROP TABLE IF EXISTS km_temp_file CASCADE;
CREATE TABLE km_temp_file (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_extension VARCHAR(50),
    file_size BIGINT,
    temp_path VARCHAR(1000) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_temp_file_dataset FOREIGN KEY (dataset_id) REFERENCES km_dataset (id) ON DELETE CASCADE
);
CREATE INDEX idx_temp_file_dataset ON km_temp_file(dataset_id);
CREATE INDEX idx_temp_file_expire ON km_temp_file(expire_time);
COMMENT ON TABLE km_temp_file IS '临时文件表';
"@

$extraSysMenu = @"
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2018, '知识库管理', 2000, 5, 'knowledge-manager', 'ai/knowledge-manager/index', 1, 0, 'C', '0', '0', 'ai:knowledge:list', 'mdi:database', 1, NOW())
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES (2019, '知识库详情', 2000, 6, 'knowledge-detail', 'ai/knowledge-detail/index', 1, 0, 'C', '1', '0', 'ai:knowledge:view', 'mdi:database-search', 1, NOW())
ON CONFLICT (menu_id) DO NOTHING;

-- 更新角色权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2018 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2018);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2019 WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2019);
"@

# Helper to write to file
function Add-Content-UTF8 {
    param([string]$Path, [string]$Value)
    [System.IO.File]::AppendAllText($Path, $Value + [Environment]::NewLine, [System.Text.Encoding]::UTF8)
}

function Add-Lines-UTF8 {
    param([string]$Path, [string[]]$Lines)
    [System.IO.File]::AppendAllLines($Path, $Lines, [System.Text.Encoding]::UTF8)
}

# Clear temp file
if (Test-Path $tempPath) { Remove-Item $tempPath }

# Part 1: Header + Sys Tables + Model Table (Lines 0 to 1854 inclusive)
$part1 = $lines[0..1854]
Write-Host "Part 1 Length: $($part1.Count)"
Add-Lines-UTF8 -Path $tempPath -Lines $part1
Add-Content-UTF8 -Path $tempPath -Value "`n`n$kbSchema`n`n"

# Part 3: App Tables + Tool Table (Lines 1958 to 2291 inclusive)
# Indices 1957..2291
$part3 = $lines[1957..2291]
Write-Host "Part 3 Length: $($part3.Count)"
Add-Lines-UTF8 -Path $tempPath -Lines $part3

Add-Content-UTF8 -Path $tempPath -Value "`n`n-- ============================================================================`n-- 初始化数据`n-- ============================================================================`n`n"

# Part 4: Golden Data (Lines 3520 to 3765 inclusive)
# Indices 3519..3764
$part4 = $lines[3519..3764]
Write-Host "Part 4 Length: $($part4.Count)"
Add-Lines-UTF8 -Path $tempPath -Lines $part4

Add-Content-UTF8 -Path $tempPath -Value "`n`n$extraSysMenu`n"

Write-Host "Replacing original file..."
Remove-Item $filePath
Rename-Item $tempPath $filePath
Write-Host "Done."
