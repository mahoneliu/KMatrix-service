-- V1.4.0: 增加聊天对话 Token 和请求次数频率控制
-- 1. 给 sys_user 表增加用户级限流配置字段（JSON 格式）
ALTER TABLE sys_user 
    ADD COLUMN IF NOT EXISTS rate_limit_config TEXT DEFAULT NULL;
COMMENT ON COLUMN sys_user.rate_limit_config IS '聊天限流配置(JSON)，结构：{"minute":{"requests":N,"tokens":N},"hour":{...},"day":{...}}，为空时使用系统默认配置';

-- 2. 在 sys_config 表插入系统级默认限流配置
-- config_key: chat.rate.limit.default
-- config_value: 默认限制：分钟10次/1万token, 小时100次/10万token, 天1000次/100万token
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
SELECT COALESCE((SELECT MAX(config_id) FROM sys_config), 0) + 1,
       '聊天对话默认限流配置',
       'chat.rate.limit.default',
       '{"minute":{"requests":10,"tokens":10000},"hour":{"requests":100,"tokens":100000},"day":{"requests":1000,"tokens":1000000}}',
       'Y',
       1,
       NOW(),
       1,
       NOW(),
       '嵌入第三方对话窗口的默认频率与Token限制，用户可在限流管理页面为特定用户配置覆盖值'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'chat.rate.limit.default'
);

-- 3. 增加菜单数据：限流配置 (AI 管理模块下)
-- Parent ID 2000 是 "AI 管理"
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2130, '限流配置', 2000, 8, 'rate-limit', 'ai/rate-limit/index', 1, 0, 'C', '0', '0', 'ai:rate-limit:list', 'mdi:timer-sand', 1, NOW(), 'AI对话频率与Token限制管理'),
(2131, '限流查询', 2130, 1, '', '', 1, 0, 'F', '0', '0', 'ai:rate-limit:query', '#', 1, NOW(), ''),
(2132, '限流修改', 2130, 2, '', '', 1, 0, 'F', '0', '0', 'ai:rate-limit:edit', '#', 1, NOW(), ''),
(2133, '限流删除', 2130, 3, '', '', 1, 0, 'F', '0', '0', 'ai:rate-limit:remove', '#', 1, NOW(), '') on conflict (menu_id) do nothing;

-- 4. 为超级管理员关联菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2130, 2131, 2132, 2133) on conflict (role_id, menu_id) do nothing;
