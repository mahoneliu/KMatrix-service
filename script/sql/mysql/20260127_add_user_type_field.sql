-- 匿名用户认证功能 - 数据库迁移脚本
-- 日期: 2026-01-27
-- 描述: 为 km_chat_session 表添加 user_type 字段，用于区分匿名用户、系统用户和第三方用户

-- 添加用户类型字段
ALTER TABLE km_chat_session 
ADD COLUMN user_type VARCHAR(20) DEFAULT 'system_user' COMMENT '用户类型 (anonymous_user/system_user/third_user)';

-- 更新现有数据的用户类型为系统用户
UPDATE km_chat_session SET user_type = 'system_user' WHERE user_type IS NULL;
