-- ----------------------------
-- KMatrix AI 模块全量数据库脚本 (MySQL 版 - 整合版)
-- 整合日期: 2026-01-27
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 0. 模型供应商表
-- ----------------------------
DROP TABLE IF EXISTS `km_model_provider`;
CREATE TABLE `km_model_provider` (
  `provider_id` bigint(20) NOT NULL COMMENT '供应商ID',
  `provider_name` varchar(64) NOT NULL COMMENT '供应商名称',
  `provider_key` varchar(64) NOT NULL COMMENT '供应商标识(openai/ollama)',
  `provider_type` char(1) DEFAULT '1' COMMENT '供应商类型（1公用 2本地）',
  `default_endpoint` varchar(255) DEFAULT '' COMMENT '默认API地址',
  `site_url` varchar(255) DEFAULT '' COMMENT '官网URL',
  `icon_url` varchar(500) DEFAULT '' COMMENT '图标URL',
  `config_schema` json DEFAULT NULL COMMENT '配置参数定义',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `sort` int(4) DEFAULT '0' COMMENT '排序',
  `models` json DEFAULT NULL COMMENT '支持的模型标识(JSON)',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`provider_id`),
  UNIQUE KEY `uk_provider_key` (`provider_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型供应商表';

-- ----------------------------
-- 1. 模型管理表
-- ----------------------------
DROP TABLE IF EXISTS `km_model`;
CREATE TABLE `km_model` (
  `model_id` bigint(20) NOT NULL COMMENT '模型ID',
  `provider_id` bigint(20) DEFAULT NULL COMMENT '关联供应商ID',
  `model_name` varchar(64) NOT NULL COMMENT '模型名称',
  `model_type` char(1) NOT NULL COMMENT '模型类型（1语言模型 2向量模型）',
  `model_key` varchar(100) NOT NULL COMMENT '基础模型 (e.g. gpt-4)',
  `api_key` varchar(255) DEFAULT '' COMMENT 'API Key',
  `api_base` varchar(255) DEFAULT '' COMMENT 'API Base URL',
  `config` json DEFAULT NULL COMMENT '其它配置参数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `is_builtin` char(1) DEFAULT 'N' COMMENT '是否内置（Y是 N否）',
  `model_source` char(1) DEFAULT '1' COMMENT '模型来源（1公有 2本地）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- ----------------------------
-- 2. 知识库系统表
-- ----------------------------
DROP TABLE IF EXISTS `km_knowledge`;
CREATE TABLE `km_knowledge` (
  `knowledge_id` bigint(20) NOT NULL COMMENT '知识库ID',
  `knowledge_name` varchar(64) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT '' COMMENT '描述',
  `embed_model_id` bigint(20) DEFAULT NULL COMMENT '关联的向量模型ID',
  `index_name` varchar(64) DEFAULT '' COMMENT 'ES索引名称',
  `permission` char(1) DEFAULT '1' COMMENT '权限范围（1私有 2公开）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

DROP TABLE IF EXISTS `km_document`;
CREATE TABLE `km_document` (
  `doc_id` bigint(20) NOT NULL COMMENT '文档ID',
  `knowledge_id` bigint(20) NOT NULL COMMENT '所属知识库ID',
  `file_name` varchar(128) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件路径',
  `file_type` varchar(10) DEFAULT '' COMMENT '文件类型',
  `file_size` bigint(20) DEFAULT '0' COMMENT '文件大小',
  `char_count` int(11) DEFAULT '0' COMMENT '字符数',
  `status` char(1) DEFAULT '0' COMMENT '状态（0待解析 1解析中 2完成 3失败）',
  `error_msg` varchar(1000) DEFAULT '' COMMENT '错误信息',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`doc_id`),
  KEY `idx_kb_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

DROP TABLE IF EXISTS `km_paragraph`;
CREATE TABLE `km_paragraph` (
  `paragraph_id` bigint(20) NOT NULL COMMENT '分段ID',
  `doc_id` bigint(20) NOT NULL COMMENT '所属文档ID',
  `knowledge_id` bigint(20) NOT NULL COMMENT '所属知识库ID',
  `content` longtext NOT NULL COMMENT '分段内容',
  `title` varchar(255) DEFAULT '' COMMENT '分段标题',
  `status` char(1) DEFAULT '1' COMMENT '状态（1启用 0禁用）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`paragraph_id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_kb_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分段表';

DROP TABLE IF EXISTS `km_problem`;
CREATE TABLE `km_problem` (
  `problem_id` bigint(20) NOT NULL COMMENT '问题ID',
  `knowledge_id` bigint(20) NOT NULL COMMENT '所属知识库ID',
  `content` varchar(500) NOT NULL COMMENT '问题内容',
  `hit_count` int(11) DEFAULT '0' COMMENT '命中次数',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`problem_id`),
  KEY `idx_kb_id` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库相关问题表';

DROP TABLE IF EXISTS `km_problem_paragraph`;
CREATE TABLE `km_problem_paragraph` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `problem_id` bigint(20) NOT NULL COMMENT '问题ID',
  `paragraph_id` bigint(20) NOT NULL COMMENT '分段ID',
  `doc_id` bigint(20) NOT NULL COMMENT '文档ID',
  `knowledge_id` bigint(20) NOT NULL COMMENT '知识库ID',
  PRIMARY KEY (`id`),
  KEY `idx_problem` (`problem_id`),
  KEY `idx_paragraph` (`paragraph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题与分段关联表';

-- ----------------------------
-- 4. AI应用与访问控制 (含增量字段)
-- ----------------------------
DROP TABLE IF EXISTS `km_app`;
CREATE TABLE `km_app` (
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `app_name` varchar(64) NOT NULL COMMENT '应用名称',
  `description` varchar(500) DEFAULT '' COMMENT '应用描述',
  `icon` varchar(255) DEFAULT '' COMMENT '应用图标',
  `app_type` char(1) DEFAULT '1' COMMENT '应用类型（1基础对话 2工作流）',
  `status` char(1) DEFAULT '0' COMMENT '状态（0草稿 1发布）',
  `prologue` varchar(1000) DEFAULT '' COMMENT '开场白',
  `model_setting` json DEFAULT NULL COMMENT '模型配置(JSON)',
  `knowledge_setting` json DEFAULT NULL COMMENT '知识库配置(JSON)',
  `workflow_config` json DEFAULT NULL COMMENT '工作流配置(JSON)',
  `graph_data` json DEFAULT NULL COMMENT '前端画布数据(JSON)',
  `dsl_data` json DEFAULT NULL COMMENT '工作流DSL配置(JSON)',
  `parameters` json DEFAULT NULL COMMENT '应用参数配置(全局/接口/会话)',
  `model_id` bigint(20) DEFAULT NULL COMMENT '关联模型ID',
  `enable_execution_detail` char(1) DEFAULT '0' COMMENT '是否启用执行详情（0禁用 1启用）',
  `public_access` char(1) DEFAULT '1' COMMENT '公开访问（0关闭 1开启）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI应用表';

DROP TABLE IF EXISTS `km_app_knowledge`;
CREATE TABLE `km_app_knowledge` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `knowledge_id` bigint(20) NOT NULL COMMENT '知识库ID',
  `sort` int(4) DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_kb` (`app_id`,`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用-知识库关联表';

DROP TABLE IF EXISTS `km_app_token`;
CREATE TABLE `km_app_token` (
  `token_id` bigint(20) NOT NULL COMMENT 'Token ID',
  `app_id` bigint(20) NOT NULL COMMENT '关联应用ID',
  `token` varchar(64) NOT NULL COMMENT 'Token值',
  `token_name` varchar(100) NOT NULL COMMENT 'Token名称',
  `allowed_origins` varchar(500) DEFAULT '*' COMMENT '允许的来源域名(逗号分隔,*表示全部)',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `status` char(1) DEFAULT '1' COMMENT '状态(0停用 1启用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`token_id`),
  UNIQUE KEY `idx_token` (`token`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App嵌入Token表';

-- ----------------------------
-- 5. 数据源与元数据
-- ----------------------------
DROP TABLE IF EXISTS `km_data_source`;
CREATE TABLE `km_data_source` (
  `data_source_id` bigint(20) NOT NULL COMMENT '数据源ID',
  `data_source_name` varchar(200) NOT NULL COMMENT '数据源名称',
  `source_type` varchar(20) NOT NULL COMMENT '数据源类型 (DYNAMIC/MANUAL)',
  `ds_key` varchar(100) DEFAULT NULL COMMENT '数据源标识',
  `driver_class_name` varchar(200) DEFAULT NULL COMMENT 'JDBC驱动类',
  `jdbc_url` varchar(500) DEFAULT NULL COMMENT 'JDBC连接URL',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(500) DEFAULT NULL COMMENT '密码',
  `db_type` varchar(50) DEFAULT NULL COMMENT '数据库类型',
  `is_enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`data_source_id`),
  UNIQUE KEY `uk_data_source_name` (`data_source_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

DROP TABLE IF EXISTS `km_database_meta`;
CREATE TABLE `km_database_meta` (
  `meta_id` bigint(20) NOT NULL COMMENT '元数据ID',
  `data_source_id` bigint(20) NOT NULL COMMENT '关联数据源ID',
  `meta_source_type` varchar(20) NOT NULL COMMENT '来源类型',
  `ddl_content` text COMMENT '建表SQL',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `table_comment` varchar(500) DEFAULT NULL COMMENT '表注释',
  `columns` json DEFAULT NULL COMMENT '列信息',
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`meta_id`),
  UNIQUE KEY `uk_ds_table` (`data_source_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库元数据表';

-- ----------------------------
-- 6. 工作流执行状态 (含消耗字段)
-- ----------------------------
DROP TABLE IF EXISTS `km_workflow_instance`;
CREATE TABLE `km_workflow_instance` (
  `instance_id` bigint(20) NOT NULL COMMENT '实例ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `session_id` bigint(20) NOT NULL COMMENT '会话ID',
  `workflow_config` json NOT NULL COMMENT '配置快照',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `current_node` varchar(64) DEFAULT NULL,
  `global_state` json DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例表';

DROP TABLE IF EXISTS `km_node_execution`;
CREATE TABLE `km_node_execution` (
  `execution_id` bigint(20) NOT NULL COMMENT '执行ID',
  `instance_id` bigint(20) NOT NULL COMMENT '实例ID',
  `node_id` varchar(64) NOT NULL COMMENT '节点ID',
  `node_type` varchar(64) NOT NULL COMMENT '节点类型',
  `node_name` varchar(200) DEFAULT NULL COMMENT '节点名称',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `input_params` json DEFAULT NULL,
  `output_params` json DEFAULT NULL,
  `input_tokens` int(11) DEFAULT '0',
  `output_tokens` int(11) DEFAULT '0',
  `total_tokens` int(11) DEFAULT '0',
  `duration_ms` bigint(20) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `retry_count` int(4) DEFAULT '0',
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点执行记录表';

-- ----------------------------
-- 7. 聊天记录 (含 user_type)
-- ----------------------------
DROP TABLE IF EXISTS `km_chat_session`;
CREATE TABLE `km_chat_session` (
  `session_id` bigint(20) NOT NULL COMMENT '会话ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `title` varchar(128) DEFAULT '新会话' COMMENT '标题',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) DEFAULT 'system_user' COMMENT '用户类型',
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

DROP TABLE IF EXISTS `km_chat_message`;
CREATE TABLE `km_chat_message` (
  `message_id` bigint(20) NOT NULL COMMENT '消息ID',
  `instance_id` bigint(20) NOT NULL COMMENT '实例ID',
  `session_id` bigint(20) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色',
  `content` longtext COMMENT '内容',
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ----------------------------
-- 8. 工作流定义 (含自定义参数开关)
-- ----------------------------
DROP TABLE IF EXISTS `km_node_definition`;
CREATE TABLE `km_node_definition` (
  `node_def_id` bigint(20) NOT NULL COMMENT '节点定义ID',
  `node_type` varchar(100) NOT NULL COMMENT '节点类型',
  `node_label` varchar(200) NOT NULL COMMENT '显示名称',
  `node_icon` varchar(200) DEFAULT NULL,
  `node_color` varchar(50) DEFAULT NULL,
  `category` varchar(50) NOT NULL COMMENT '分类',
  `description` varchar(500) DEFAULT NULL,
  `is_system` char(1) DEFAULT '0',
  `is_enabled` char(1) DEFAULT '1',
  `allow_custom_input_params` char(1) DEFAULT '0' COMMENT '自定义输入',
  `allow_custom_output_params` char(1) DEFAULT '0' COMMENT '自定义输出',
  `input_params` text COMMENT '输入参数',
  `output_params` text COMMENT '输出参数',
  `version` int(4) DEFAULT '1',
  `parent_version_id` bigint(20) DEFAULT NULL,
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`node_def_id`),
  UNIQUE KEY `uk_node_type_version` (`node_type`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点定义表';

DROP TABLE IF EXISTS `km_node_connection_rule`;
CREATE TABLE `km_node_connection_rule` (
  `rule_id` bigint(20) NOT NULL COMMENT '规则ID',
  `source_node_type` varchar(100) NOT NULL,
  `target_node_type` varchar(100) NOT NULL,
  `rule_type` char(1) NOT NULL,
  `priority` int(4) DEFAULT '0',
  `is_enabled` char(1) DEFAULT '1',
  `create_dept` bigint(20) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_connection` (`source_node_type`,`target_node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点连接规则表';

-- ============================================================================
-- 初始化数据
-- ============================================================================

-- 模型供应商
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_time`) VALUES 
(1, 'OpenAI', 'openai', '1', 'https://api.openai.com/v1', 'https://openai.com', '/model-provider-icon/openai.png', NULL, '0', 1, '[{"modelKey": "gpt-4o", "modelType": "1"}, {"modelKey": "gpt-4o-mini", "modelType": "1"}, {"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-3.5-turbo", "modelType": "1"}]', NOW()),
(2, 'Gemini', 'gemini', '1', 'https://generativelanguage.googleapis.com', 'https://ai.google.dev', '/model-provider-icon/gemini.svg', NULL, '0', 2, '[{"modelKey": "gemini-3-flash-preview", "modelType": "1"}, {"modelKey": "gemini-3-pro-preview", "modelType": "1"}, {"modelKey": "gemini-2.5-flash", "modelType": "1"}, {"modelKey": "text-embedding-004", "modelType": "2"}]', NOW()),
(3, 'Ollama', 'ollama', '2', 'http://localhost:11434', 'https://ollama.com', '/model-provider-icon/ollama.png', NULL, '0', 3, '[{"modelKey": "llama3", "modelType": "1"}]', NOW()),
(4, 'DeepSeek', 'deepseek', '1', 'https://api.deepseek.com', 'https://www.deepseek.com', '/model-provider-icon/deepseek.png', NULL, '0', 4, '[{"modelKey": "deepseek-chat", "modelType": "1"}, {"modelKey": "deepseek-coder", "modelType": "1"}]', NOW()),
(5, 'vLLM', 'vllm', '2', 'http://localhost:8000/v1', 'https://docs.vllm.ai', '/model-provider-icon/vllm.ico', NULL, '0', 5, '[]', NOW()),
(6, 'Azure OpenAI', 'azure', '1', 'https://{resource}.openai.azure.com', 'https://azure.microsoft.com/products/ai-services/openai-service', '/model-provider-icon/azure.png', NULL, '0', 6, '[{"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-4-turbo", "modelType": "1"}, {"modelKey": "gpt-35-turbo", "modelType": "1"}]', NOW()),
(7, '阿里云百炼', 'bailian', '1', 'https://dashscope.aliyuncs.com/api/v1', 'https://www.aliyun.com/product/bailian', '/model-provider-icon/bailian.jpeg', NULL, '0', 7, '[{"modelKey": "qwen-max", "modelType": "1"}, {"modelKey": "qwen-plus", "modelType": "1"}, {"modelKey": "qwen-turbo", "modelType": "1"}, {"modelKey": "text-embedding-v1", "modelType": "2"}, {"modelKey": "text-embedding-v2", "modelType": "2"}]', NOW()),
(8, '智谱AI', 'zhipu', '1', 'https://open.bigmodel.cn/api/paas/v4', 'https://open.bigmodel.cn', '/model-provider-icon/zhipu.png', NULL, '0', 8, '[{"modelKey": "glm-4", "modelType": "1"}, {"modelKey": "glm-4-flash", "modelType": "1"}, {"modelKey": "glm-3-turbo", "modelType": "1"}]', NOW()),
(9, '豆包', 'doubao', '1', 'https://ark.cn-beijing.volces.com/api/v3', 'https://www.volcengine.com/product/doubao', '/model-provider-icon/doubao.png', NULL, '0', 9, '[{"modelKey": "doubao-pro-32k", "modelType": "1"}, {"modelKey": "doubao-lite-32k", "modelType": "1"}]', NOW()),
(10, 'Moonshot', 'moonshot', '1', 'https://api.moonshot.cn/v1', 'https://www.moonshot.cn', '/model-provider-icon/moonshot.ico', NULL, '0', 10, '[{"modelKey": "moonshot-v1-8k", "modelType": "1"}, {"modelKey": "moonshot-v1-32k", "modelType": "1"}, {"modelKey": "moonshot-v1-128k", "modelType": "1"}]', NOW());

-- 节点定义 (1-10)
INSERT INTO `kmatrix`.`km_node_definition`(`node_def_id`, `node_type`, `node_label`, `node_icon`, `node_color`, `category`, `description`, `is_system`, `is_enabled`, `allow_custom_input_params`, `allow_custom_output_params`, `input_params`, `output_params`, `version`, `parent_version_id`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES 
(1, 'APP_INFO', '基础信息', 'mdi:information', '#486191F0', 'basic', '应用的基础信息配置', '1', '1', '0', '0', '[]', '[]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-14 16:33:01', NULL),
(2, 'START', '开始', 'mdi:play-circle', '#1E6021FF', 'basic', '工作流的入口节点', '1', '1', '0', '0', '[]', '[{"key":"userInput","label":"用户输入","type":"string","required":true,"defaultValue":null,"description":"用户的输入内容"}]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-18 20:37:29', NULL),
(3, 'END', '结束', 'mdi:stop-circle', '#9875BFFF', 'basic', '工作流的结束节点，可以把各节点的输出参数引用进来，组合成最终回复消息作为工作流最终输出', '1', '1', '1', '0', '[{"key":"finalResponse","label":"最终回复","type":"string","required":true,"description":"返回给用户的最终回复内容"}]', '[]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-16 23:31:07', NULL),
(4, 'LLM_CHAT', 'LLM 对话', 'mdi:robot', '#3b82f6', 'ai', '调用大语言模型进行对话', '0', '1', '1', '1', '[{"key":"inputMessage","label":"输入消息","type":"string","required":true,"defaultValue":null,"description":"传递给 LLM 的输入消息"}]', '[{"key":"response","label":"AI 回复","type":"string","required":true,"defaultValue":null,"description":"LLM 生成的回复内容"}]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-18 15:23:23', NULL),
(5, 'INTENT_CLASSIFIER', '意图分类', 'mdi:sitemap', '#F9AA7FFF', 'ai', '识别用户输入的意图并分类', '0', '1', '0', '0', '[{"key":"instruction","label":"文本指令","type":"string","required":true,"description":"需要分类的指令"}]', '[{"key":"intent","label":"匹配的意图","type":"string","required":true,"description":"识别出的意图名称"}]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-16 23:33:03', NULL),
(6, 'CONDITION', '条件判断', 'mdi:source-branch', '#958365FF', 'logic', '根据条件表达式进行分支判断', '0', '1', '0', '0', '[{"key":"matchedBranch","label":"匹配的分支","type":"string","required":false,"description":"用于条件判断的值"}]', '[{"key":"matchedBranch","label":"匹配的分支","type":"string","required":true,"description":"满足条件的分支名称"}]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-15 16:59:54', NULL),
(7, 'FIXED_RESPONSE', '指定回复', 'mdi:message-text', '#7A7170FF', 'action', '返回预设的固定文本内容', '0', '1', '1', '0', '[]', '[{"key":"response","label":"回复内容","type":"string","required":true,"defaultValue":null,"description":"固定的回复文本"}]', 1, NULL, NULL, NULL, '2026-01-07 15:24:03', 1, '2026-01-24 02:47:10', NULL),
(8, 'DB_QUERY', '数据库查询', 'mdi:database-search', '#06b6d4', 'ai', '结合LLM智能分析用户问题，生成SQL查询并返回自然语言回答', '0', '1', '0', '0', '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"defaultValue":null,"description":"用户提出的业务问题"}]', '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"defaultValue":null,"description":"LLM生成的SQL语句"},{"key":"queryResult","label":"查询结果","type":"object","required":true,"defaultValue":null,"description":"SQL执行结果(JSON)"},{"key":"response","label":"AI回复","type":"string","required":true,"defaultValue":null,"description":"基于查询结果生成的自然语言回答"}]', 1, NULL, NULL, NULL, '2026-01-20 02:50:40', 1, '2026-01-20 03:07:34', NULL),
(9, 'SQL_GENERATE', 'SQL生成', 'mdi:database-cog', '#8b5cf6', 'ai', '使用LLM分析用户问题，结合数据库元数据生成SQL语句', '0', '1', '0', '0', '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"description":"用户提出的业务问题"}]', '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"description":"LLM生成的SQL语句"}]', 1, NULL, NULL, NULL, '2026-01-24 02:16:29', NULL, NULL, NULL),
(10, 'SQL_EXECUTE', 'SQL执行', 'mdi:database-arrow-right', '#06b6d4', 'database', '执行SQL语句并返回查询结果', '0', '1', '0', '0', '[{"key":"sql","label":"SQL语句","type":"string","required":true,"defaultValue":null,"description":"待执行的SQL语句"}]', '[{"key":"queryResult","label":"查询结果","type":"object","required":true,"defaultValue":null,"description":"SQL执行结果(JSON)"},{"key":"rowCount","label":"返回行数","type":"number","required":true,"defaultValue":null,"description":"查询返回的行数"},{"key":"strResult","label":"查询结果","type":"string","required":true,"defaultValue":"","description":""}]', 1, NULL, NULL, NULL, '2026-01-24 02:16:29', 1, '2026-01-24 04:32:48', NULL);

-- 连接规则数据 (1-44 号规则，已去除 Rule 36 重复)
INSERT INTO `km_node_connection_rule` (`rule_id`, `source_node_type`, `target_node_type`, `rule_type`, `priority`, `create_time`) VALUES 
(1, 'START', 'LLM_CHAT', '0', 10, NOW()), (2, 'START', 'INTENT_CLASSIFIER', '0', 10, NOW()), (3, 'START', 'CONDITION', '0', 10, NOW()), (4, 'START', 'FIXED_RESPONSE', '0', 10, NOW()),
(5, 'LLM_CHAT', 'END', '0', 10, NOW()), (6, 'LLM_CHAT', 'LLM_CHAT', '0', 10, NOW()), (7, 'LLM_CHAT', 'CONDITION', '0', 10, NOW()), (8, 'LLM_CHAT', 'FIXED_RESPONSE', '0', 10, NOW()),
(9, 'INTENT_CLASSIFIER', 'LLM_CHAT', '0', 10, NOW()), (10, 'INTENT_CLASSIFIER', 'CONDITION', '0', 10, NOW()), (11, 'INTENT_CLASSIFIER', 'FIXED_RESPONSE', '0', 10, NOW()), (12, 'INTENT_CLASSIFIER', 'END', '0', 10, NOW()),
(13, 'CONDITION', 'LLM_CHAT', '0', 10, NOW()), (14, 'CONDITION', 'FIXED_RESPONSE', '0', 10, NOW()), (15, 'CONDITION', 'END', '0', 10, NOW()), (16, 'FIXED_RESPONSE', 'END', '0', 10, NOW()),
(17, 'START', 'DB_QUERY', '0', 10, NOW()), (18, 'LLM_CHAT', 'DB_QUERY', '0', 10, NOW()), (19, 'CONDITION', 'DB_QUERY', '0', 10, NOW()), (20, 'INTENT_CLASSIFIER', 'DB_QUERY', '0', 10, NOW()),
(21, 'DB_QUERY', 'END', '0', 10, NOW()), (22, 'DB_QUERY', 'LLM_CHAT', '0', 10, NOW()), (23, 'DB_QUERY', 'CONDITION', '0', 10, NOW()), (24, 'DB_QUERY', 'INTENT_CLASSIFIER', '0', 10, NOW()), (25, 'DB_QUERY', 'FIXED_RESPONSE', '0', 10, NOW()),
(26, 'START', 'SQL_GENERATE', '0', 10, NOW()), (27, 'LLM_CHAT', 'SQL_GENERATE', '0', 10, NOW()), (28, 'CONDITION', 'SQL_GENERATE', '0', 10, NOW()), (29, 'INTENT_CLASSIFIER', 'SQL_GENERATE', '0', 10, NOW()),
(30, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, NOW()), (31, 'SQL_GENERATE', 'END', '0', 10, NOW()), (32, 'SQL_GENERATE', 'LLM_CHAT', '0', 10, NOW()), (33, 'SQL_GENERATE', 'CONDITION', '0', 10, NOW()), (34, 'SQL_GENERATE', 'FIXED_RESPONSE', '0', 10, NOW()),
(35, 'START', 'SQL_EXECUTE', '0', 10, NOW()), (37, 'LLM_CHAT', 'SQL_EXECUTE', '0', 10, NOW()), (38, 'CONDITION', 'SQL_EXECUTE', '0', 10, NOW()), (39, 'INTENT_CLASSIFIER', 'SQL_EXECUTE', '0', 10, NOW()),
(40, 'SQL_EXECUTE', 'END', '0', 10, NOW()), (41, 'SQL_EXECUTE', 'LLM_CHAT', '0', 10, NOW()), (42, 'SQL_EXECUTE', 'CONDITION', '0', 10, NOW()), (43, 'SQL_EXECUTE', 'FIXED_RESPONSE', '0', 10, NOW()), (44, 'SQL_EXECUTE', 'INTENT_CLASSIFIER', '0', 10, NOW());

-- 菜单数据
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES 
(2000, 'AI 管理', 0, 10, 'ai', NULL, 1, 0, 'M', '0', '0', '', 'robot', 1, NOW()),
(2001, '模型管理', 2000, 1, 'model-manager', 'ai/model-manager/index', 1, 0, 'C', '0', '0', 'ai:model:list', 'model-alt', 1, NOW()),
(2002, '应用管理', 2000, 2, 'app-manager', 'ai/app-manager/index', 1, 0, 'C', '0', '0', 'ai:app:list', 'app-store', 1, NOW()),
(2010, '工作流编排', 2000, 10, 'workflow', 'ai/workflow/index', 1, 0, 'C', '1', '0', 'ai:app:workflow', '#', 1, NOW()),
(2011, 'AI对话', 2000, 3, 'chat', 'ai/chat/index', 1, 0, 'C', '1', '0', 'ai:chat:view', 'chat', 1, NOW()),
(2013, '节点定义', 2000, 3, 'node-definition', 'ai/node-definition/index', 1, 1, 'C', '0', '0', 'ai:nodeDefinition:list', 'mdi:menu', 1, NOW()),
(2014, '数据源管理', 2000, 4, 'datasource-manager', 'ai/datasource-manager/index', 1, 1, 'C', '0', '0', 'ai:datasourceManager:list', 'mdi:menu', 1, NOW());

-- 角色权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id <= 2014;
