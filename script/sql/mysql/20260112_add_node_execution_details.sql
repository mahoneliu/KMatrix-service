-- 扩展节点执行记录表,增加 token 消耗和执行详情字段
-- 作者: Mahone
-- 日期: 2026-01-12

ALTER TABLE km_node_execution
ADD COLUMN node_name VARCHAR(200) COMMENT '节点名称',
ADD COLUMN input_tokens INT DEFAULT 0 COMMENT '输入token数',
ADD COLUMN output_tokens INT DEFAULT 0 COMMENT '输出token数',
ADD COLUMN total_tokens INT DEFAULT 0 COMMENT '总token数',
ADD COLUMN duration_ms BIGINT COMMENT '执行耗时(毫秒)';
