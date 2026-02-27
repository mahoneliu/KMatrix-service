-- V1.1.0: 父子分块支持
-- 为 km_document_chunk 表新增父子分块相关字段
-- chunk_type: 0=PARENT(父块), 1=CHILD(子块), 2=STANDALONE(独立块/存量数据兼容)
-- parent_id: 子块指向其父块的 ID，父块和独立块为 NULL

ALTER TABLE km_document_chunk
    ADD COLUMN IF NOT EXISTS chunk_type SMALLINT NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS parent_id BIGINT NULL;

-- 为 parent_id 创建索引，用于检索时子块→父块的快速溯源
CREATE INDEX IF NOT EXISTS idx_km_document_chunk_parent_id ON km_document_chunk (parent_id);

-- 为 km_dataset 表新增子块分块参数字段
-- NULL 表示使用系统默认值（km.chunking.child-chunk-size）
ALTER TABLE km_dataset
    ADD COLUMN IF NOT EXISTS child_chunk_size INTEGER NULL,
    ADD COLUMN IF NOT EXISTS child_chunk_overlap INTEGER NULL;

COMMENT ON COLUMN km_document_chunk.chunk_type IS '块类型: 0=PARENT(父块), 1=CHILD(子块), 2=STANDALONE(独立块)';
COMMENT ON COLUMN km_document_chunk.parent_id IS '父块ID，子块指向其所属父块，父块和独立块为 NULL';
COMMENT ON COLUMN km_dataset.child_chunk_size IS '子块大小(字符数)，NULL 表示使用系统默认值';
COMMENT ON COLUMN km_dataset.child_chunk_overlap IS '子块重叠大小(字符数)，NULL 表示使用系统默认值';
