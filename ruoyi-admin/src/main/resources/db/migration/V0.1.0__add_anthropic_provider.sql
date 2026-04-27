-- ============================================
-- V0.1.0: 新增 Anthropic (Claude) 模型供应商
-- ============================================

INSERT INTO km_model_provider (provider_id, provider_name, provider_key, provider_type, default_endpoint, site_url, icon_url, config_schema, status, sort, models, create_time)
VALUES (13, 'Anthropic', 'anthropic', '1', 'https://api.anthropic.com', 'https://anthropic.com', '/model-provider-icon/anthropic.svg', NULL, '0', 13,
'[{"modelKey": "claude-opus-4-5", "modelType": "1"}, {"modelKey": "claude-sonnet-4-5", "modelType": "1"}, {"modelKey": "claude-haiku-4-5", "modelType": "1"}, {"modelKey": "claude-3-5-sonnet-20241022", "modelType": "1"}, {"modelKey": "claude-3-5-haiku-20241022", "modelType": "1"}, {"modelKey": "claude-3-opus-20240229", "modelType": "1"}]',
CURRENT_TIMESTAMP)
ON CONFLICT (provider_id) DO NOTHING;
