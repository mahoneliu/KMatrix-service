-- ----------------------------
-- 初始化内置向量模型数据
-- 1-语言模型 2-向量模型
-- ----------------------------

-- 1. 增加内置本地供应商 (id=11)
INSERT INTO km_model_provider(provider_id, provider_name,icon_url, provider_key, provider_type, status, sort, models, create_time)
VALUES (11, 'Local (内置)', '/model-provider-icon/logo.png','local', '2', '0', 0, '[{"modelKey": "bge-reranker-v2-m3", "modelType": "3"},{"modelKey": "bge-small-zh", "modelType": "2"}]', CURRENT_TIMESTAMP)
ON CONFLICT (provider_id) DO UPDATE SET provider_name = EXCLUDED.provider_name, models = EXCLUDED.models;

-- 2. 增加嵌入式 ONNX rerank 模型 (bge-reranker-v2-m3)
-- 注意：这个模型是 第三方 如modescope 提供的，直接在 JVM 中运行
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (30, 11, 'bge-reranker-v2-m3 (内置)', '3', 'bge-reranker-v2-m3', 'Y', '2', '0', 1, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO UPDATE SET is_default = 1, provider_id = 11, model_key = 'bge-reranker-v2-m3';

-- 3. 增加嵌入式 ONNX 向量 模型 (bge-small-zh)
-- 注意：这个模型是 第三方 如modescope 提供的，直接在 JVM 中运行
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (31, 11, 'bge-small-zh (内置)', '2', 'bge-small-zh', 'Y', '2', '0', 1, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO UPDATE SET is_default = 1, provider_id = 11, model_key = 'bge-small-zh';

-- 4. 其他公有云供应商向量模型记录 (可选，设为非默认)
-- OpenAI
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (31, 1, 'text-embedding-3-small', '2', 'text-embedding-3-small', 'N', '1', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;

-- Ollama
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (40, 3, 'nomic-embed-text', '2', 'nomic-embed-text', 'N', '2', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;

-- 阿里云百炼
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (50, 7, 'text-embedding-v2', '2', 'text-embedding-v2', 'N', '1', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;

-- 智谱AI
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (60, 8, 'embedding-2', '2', 'embedding-2', 'N', '1', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;

-- 官网菜单调整
update sys_menu set menu_name = '对话测试（官网）',menu_type = 'C', icon = 'mdi-home', remark = 'KMatrix官网地址，用于测试嵌入对话框' where menu_id = '4';
