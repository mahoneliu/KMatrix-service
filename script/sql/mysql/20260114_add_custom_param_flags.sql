-- 添加节点定义自定义参数开关字段
ALTER TABLE `km_node_definition` ADD COLUMN `allow_custom_input_params` CHAR(1) DEFAULT '0' COMMENT '是否允许自定义输入参数 (0否/1是)' AFTER `is_enabled`;
ALTER TABLE `km_node_definition` ADD COLUMN `allow_custom_output_params` CHAR(1) DEFAULT '0' COMMENT '是否允许自定义输出参数 (0否/1是)' AFTER `allow_custom_input_params`;
