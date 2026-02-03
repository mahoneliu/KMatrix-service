-- 文档管理状态追踪字段迁移脚本
-- 用于支持文档和分块的启用/禁用、向量化状态、问题生成状态追踪

-- km_document 表添加状态追踪字段
ALTER TABLE km_document ADD COLUMN IF NOT EXISTS enabled INTEGER DEFAULT 1;
ALTER TABLE km_document ADD COLUMN IF NOT EXISTS embedding_status INTEGER DEFAULT 0;
ALTER TABLE km_document ADD COLUMN IF NOT EXISTS question_status INTEGER DEFAULT 0;
ALTER TABLE km_document ADD COLUMN IF NOT EXISTS status_meta JSONB;

-- km_document_chunk 表添加状态追踪字段
ALTER TABLE km_document_chunk ADD COLUMN IF NOT EXISTS enabled INTEGER DEFAULT 1;
ALTER TABLE km_document_chunk ADD COLUMN IF NOT EXISTS embedding_status INTEGER DEFAULT 0;
ALTER TABLE km_document_chunk ADD COLUMN IF NOT EXISTS question_status INTEGER DEFAULT 0;
ALTER TABLE km_document_chunk ADD COLUMN IF NOT EXISTS status_meta JSONB;

-- 添加注释说明
COMMENT ON COLUMN km_document.enabled IS '启用状态: 0=禁用, 1=启用';
COMMENT ON COLUMN km_document.embedding_status IS '向量化状态: 0=未生成, 1=生成中, 2=已生成, 3=生成失败';
COMMENT ON COLUMN km_document.question_status IS '问题生成状态: 0=未生成, 1=生成中, 2=已生成, 3=生成失败';
COMMENT ON COLUMN km_document.status_meta IS '状态追踪元数据(JSON): aggs聚合统计, state_time状态时间记录';

COMMENT ON COLUMN km_document_chunk.enabled IS '启用状态: 0=禁用, 1=启用';
COMMENT ON COLUMN km_document_chunk.embedding_status IS '向量化状态: 0=未生成, 1=生成中, 2=已生成, 3=生成失败';
COMMENT ON COLUMN km_document_chunk.question_status IS '问题生成状态: 0=未生成, 1=生成中, 2=已生成, 3=生成失败';
COMMENT ON COLUMN km_document_chunk.status_meta IS '状态追踪元数据(JSON): state_time状态时间记录';

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_km_document_enabled ON km_document(enabled);
CREATE INDEX IF NOT EXISTS idx_km_document_embedding_status ON km_document(embedding_status);
CREATE INDEX IF NOT EXISTS idx_km_document_question_status ON km_document(question_status);

CREATE INDEX IF NOT EXISTS idx_km_document_chunk_enabled ON km_document_chunk(enabled);
CREATE INDEX IF NOT EXISTS idx_km_document_chunk_embedding_status ON km_document_chunk(embedding_status);
CREATE INDEX IF NOT EXISTS idx_km_document_chunk_question_status ON km_document_chunk(question_status);
