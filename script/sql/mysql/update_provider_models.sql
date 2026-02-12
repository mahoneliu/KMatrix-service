-- ----------------------------
-- 更新供应商 models 字段为新格式
-- 将简单字符串数组改为包含 modelKey 和 modelType 的对象数组
-- 执行日期: 2025-12-25
-- ----------------------------

-- 更新 OpenAI
UPDATE km_model_provider 
SET models = '[{"modelKey":"gpt-4o","modelType":"1"},{"modelKey":"gpt-4o-mini","modelType":"1"},{"modelKey":"gpt-4","modelType":"1"},{"modelKey":"gpt-3.5-turbo","modelType":"1"},{"modelKey":"text-embedding-3-small","modelType":"2"},{"modelKey":"text-embedding-3-large","modelType":"2"},{"modelKey":"text-embedding-ada-002","modelType":"2"}]',
    site_url = 'https://openai.com'
WHERE provider_key = 'openai';

-- 更新 Gemini
UPDATE km_model_provider 
SET models = '[{"modelKey":"gemini-1.5-pro","modelType":"1"},{"modelKey":"gemini-1.5-flash","modelType":"1"},{"modelKey":"gemini-pro","modelType":"1"},{"modelKey":"text-embedding-004","modelType":"2"}]',
    site_url = 'https://ai.google.dev'
WHERE provider_key = 'gemini';

-- 更新 Ollama
UPDATE km_model_provider 
SET models = '[{"modelKey":"llama3","modelType":"1"},{"modelKey":"llama2","modelType":"1"},{"modelKey":"mistral","modelType":"1"},{"modelKey":"mixtral","modelType":"1"},{"modelKey":"phi3","modelType":"1"},{"modelKey":"qwen2","modelType":"1"},{"modelKey":"gemma2","modelType":"1"},{"modelKey":"nomic-embed-text","modelType":"2"},{"modelKey":"mxbai-embed-large","modelType":"2"}]',
    site_url = 'https://ollama.com'
WHERE provider_key = 'ollama';

-- 更新 DeepSeek
UPDATE km_model_provider 
SET models = '[{"modelKey":"deepseek-chat","modelType":"1"},{"modelKey":"deepseek-coder","modelType":"1"}]',
    site_url = 'https://www.deepseek.com'
WHERE provider_key = 'deepseek';

-- 更新 vLLM
UPDATE km_model_provider 
SET models = '[]',
    site_url = 'https://docs.vllm.ai'
WHERE provider_key = 'vllm';

-- 更新 Azure OpenAI
UPDATE km_model_provider 
SET models = '[{"modelKey":"gpt-4","modelType":"1"},{"modelKey":"gpt-4-turbo","modelType":"1"},{"modelKey":"gpt-35-turbo","modelType":"1"}]',
    site_url = 'https://azure.microsoft.com/products/ai-services/openai-service'
WHERE provider_key = 'azure';

-- 更新 通义千问
UPDATE km_model_provider 
SET models = '[{"modelKey":"qwen-max","modelType":"1"},{"modelKey":"qwen-plus","modelType":"1"},{"modelKey":"qwen-turbo","modelType":"1"},{"modelKey":"text-embedding-v1","modelType":"2"},{"modelKey":"text-embedding-v2","modelType":"2"}]',
    site_url = 'https://tongyi.aliyun.com'
WHERE provider_key = 'qwen';

-- 更新 智谱AI
UPDATE km_model_provider 
SET models = '[{"modelKey":"glm-4","modelType":"1"},{"modelKey":"glm-4-flash","modelType":"1"},{"modelKey":"glm-3-turbo","modelType":"1"}]',
    site_url = 'https://open.bigmodel.cn'
WHERE provider_key = 'zhipu';

-- 更新 豆包
UPDATE km_model_provider 
SET models = '[{"modelKey":"doubao-pro-32k","modelType":"1"},{"modelKey":"doubao-lite-32k","modelType":"1"}]',
    site_url = 'https://www.volcengine.com/product/doubao'
WHERE provider_key = 'doubao';

-- 更新 Moonshot
UPDATE km_model_provider 
SET models = '[{"modelKey":"moonshot-v1-8k","modelType":"1"},{"modelKey":"moonshot-v1-32k","modelType":"1"},{"modelKey":"moonshot-v1-128k","modelType":"1"}]',
    site_url = 'https://www.moonshot.cn'
WHERE provider_key = 'moonshot';
