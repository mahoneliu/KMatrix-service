-- Add Anthropic Provider
INSERT INTO km_model_provider (provider_id, provider_name, provider_key, default_endpoint, icon_url, status, sort, models, remark, create_time, update_time)
VALUES (
    nextval('seq_km_model_provider'),
    'Anthropic',
    'anthropic',
    'https://api.anthropic.com/v1/',
    'https://upload.wikimedia.org/wikipedia/commons/7/78/Anthropic_logo.svg',
    '1',
    4,
    '[{"key": "claude-3-5-sonnet-20241022", "type": "1"}, {"key": "claude-3-5-haiku-20241022", "type": "1"}, {"key": "claude-3-opus-20240229", "type": "1"}]'::jsonb,
    'Anthropic Claude Models',
    NOW(),
    NOW()
) ON CONFLICT (provider_key) DO NOTHING;

-- Update OpenAI Models (GPT-4o)
UPDATE km_model_provider
SET models = models || '[{"key": "gpt-4o", "type": "1"}, {"key": "gpt-4o-mini", "type": "1"}]'::jsonb
WHERE provider_key = 'openai'
AND NOT models @> '[{"key": "gpt-4o", "type": "1"}]'::jsonb;

-- Update DeepSeek Models (DeepSeek-V3, R1)
UPDATE km_model_provider
SET models = models || '[{"key": "deepseek-chat", "type": "1"}, {"key": "deepseek-reasoner", "type": "1"}]'::jsonb,
    default_endpoint = 'https://api.deepseek.com'
WHERE provider_key = 'deepseek'
AND NOT models @> '[{"key": "deepseek-reasoner", "type": "1"}]'::jsonb;

-- Update Zhipu Models (GLM-4)
UPDATE km_model_provider
SET models = models || '[{"key": "glm-4", "type": "1"}, {"key": "glm-4-flash", "type": "1"}, {"key": "glm-4v", "type": "1"}]'::jsonb
WHERE provider_key = 'zhipu'
AND NOT models @> '[{"key": "glm-4", "type": "1"}]'::jsonb;

-- Add Moonshot (Kimi) if not exists (assuming it was generic before)
-- If Moonshot entry exists, update it.
UPDATE km_model_provider
SET models = models || '[{"key": "moonshot-v1-8k", "type": "1"}, {"key": "moonshot-v1-32k", "type": "1"}, {"key": "moonshot-v1-128k", "type": "1"}]'::jsonb
WHERE provider_key = 'moonshot'
AND NOT models @> '[{"key": "moonshot-v1-8k", "type": "1"}]'::jsonb;

-- Update Google Gemini Models (Gemini 1.5)
UPDATE km_model_provider
SET models = models || '[{"key": "gemini-1.5-pro", "type": "1"}, {"key": "gemini-1.5-flash", "type": "1"}]'::jsonb
WHERE provider_key = 'gemini'
AND NOT models @> '[{"key": "gemini-1.5-pro", "type": "1"}]'::jsonb;

-- Update Qwen (Alibaba)
UPDATE km_model_provider
SET models = models || '[{"key": "qwen-plus", "type": "1"}, {"key": "qwen-turbo", "type": "1"}, {"key": "qwen-max", "type": "1"}]'::jsonb
WHERE provider_key IN ('qwen', 'bailian')
AND NOT models @> '[{"key": "qwen-max", "type": "1"}]'::jsonb;
