-- 添加公开访问字段
ALTER TABLE km_app ADD COLUMN public_access CHAR(1) DEFAULT '1' COMMENT '公开访问（0关闭 1开启）';

-- 更新现有数据，默认开启公开访问
UPDATE km_app SET public_access = '1' WHERE public_access IS NULL;
