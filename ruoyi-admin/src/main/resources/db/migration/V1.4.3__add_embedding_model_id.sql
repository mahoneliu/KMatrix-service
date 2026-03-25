-- 增加 km_knowledge_base 表的 embedding_model_id 字段
ALTER TABLE km_knowledge_base ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT;
COMMENT ON COLUMN km_knowledge_base.embedding_model_id IS '绑定的向量化模型ID';
