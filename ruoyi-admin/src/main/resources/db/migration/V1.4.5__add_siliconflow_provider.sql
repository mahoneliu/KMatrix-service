-- 1. 增加硅基流动供应商 (id=12)
INSERT INTO km_model_provider(provider_id, provider_name, provider_key, provider_type, default_endpoint, site_url, icon_url, status, sort, models, create_time)
VALUES (12, 'SiliconFlow (硅基流动)', 'siliconflow', '1', 'https://api.siliconflow.cn/v1/', 'https://siliconflow.cn/', '/model-provider-icon/logo-siliconflow.svg', '0', 12, 
'[{"modelKey": "deepseek-ai/DeepSeek-V3", "modelType": "1"},{"modelKey": "deepseek-ai/DeepSeek-R1", "modelType": "1"},{"modelKey": "Qwen/Qwen2.5-72B-Instruct", "modelType": "1"},{"modelKey": "Qwen/Qwen3-Reranker-0.6B", "modelType": "3"},{"modelKey": "BAAI/bge-reranker-v2-m3", "modelType": "3"},{"modelKey": "BAAI/bge-m3", "modelType": "2"}]', 
CURRENT_TIMESTAMP)
ON CONFLICT (provider_id) DO UPDATE SET provider_name = EXCLUDED.provider_name, models = EXCLUDED.models, default_endpoint = EXCLUDED.default_endpoint;

-- 2. 增加系统内置硅基流动 rerank 模型 (BAAI/bge-reranker-v2-m3)
-- 注意：这是个公用云模型，这里只是预设配置，实际使用需在界面配置 API Key
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES (32, 12, 'BAAI/bge-reranker-v2-m3', '3', 'BAAI/bge-reranker-v2-m3', 'N', '1', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;
