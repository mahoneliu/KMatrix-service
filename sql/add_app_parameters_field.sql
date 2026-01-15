-- 添加应用参数配置字段
-- 用于存储全局参数、接口参数、会话参数
-- @author Mahone
-- @date 2026-01-14

ALTER TABLE km_app 
ADD COLUMN parameters JSON COMMENT '应用参数配置(全局/接口/会话)' 
AFTER workflow_config;
