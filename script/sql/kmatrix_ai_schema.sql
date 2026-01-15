-- ----------------------------
-- KMatrix AI 模块数据库脚本 (RuoYi 规范版 - 完整版)
-- ----------------------------

-- ----------------------------
-- 0. 模型供应商表 (新增)
-- ----------------------------
DROP TABLE IF EXISTS km_model_provider;
CREATE TABLE km_model_provider (
    provider_id     BIGINT(20)      NOT NULL COMMENT '供应商ID',
    provider_name   VARCHAR(64)     NOT NULL COMMENT '供应商名称',
    provider_key    VARCHAR(64)     NOT NULL COMMENT '供应商标识(openai/ollama)',
    provider_type   CHAR(1)         DEFAULT '1' COMMENT '供应商类型（1公用 2本地）',
    default_endpoint VARCHAR(255)   DEFAULT '' COMMENT '默认API地址',
    site_url        VARCHAR(255)    DEFAULT '' COMMENT '官网URL',
    icon_url        VARCHAR(500)    DEFAULT '' COMMENT '图标URL',
    config_schema   JSON            DEFAULT NULL COMMENT '配置参数定义',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
    sort            INT(4)          DEFAULT 0 COMMENT '排序',
    models          JSON            DEFAULT NULL COMMENT '支持的模型标识(JSON)',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (provider_id),
    UNIQUE KEY uk_provider_key (provider_key)
) ENGINE=InnoDB COMMENT='模型供应商表';

-- 初始化供应商数据
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (1, 'OpenAI', 'openai', '1', 'https://api.openai.com/v1', 'https://openai.com', '/model-provider-icon/openai.png', NULL, '0', 1, '[{\"modelKey\": \"gpt-4o\", \"modelType\": \"1\"}, {\"modelKey\": \"gpt-4o-mini\", \"modelType\": \"1\"}, {\"modelKey\": \"gpt-4\", \"modelType\": \"1\"}, {\"modelKey\": \"gpt-3.5-turbo\", \"modelType\": \"1\"}, {\"modelKey\": \"text-embedding-3-small\", \"modelType\": \"2\"}, {\"modelKey\": \"text-embedding-3-large\", \"modelType\": \"2\"}, {\"modelKey\": \"text-embedding-ada-002\", \"modelType\": \"2\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (2, 'Gemini', 'gemini', '1', 'https://generativelanguage.googleapis.com', 'https://ai.google.dev', '/model-provider-icon/gemini.svg', NULL, '0', 2, '[{\"modelKey\": \"gemini-1.5-pro\", \"modelType\": \"1\"}, {\"modelKey\": \"gemini-1.5-flash\", \"modelType\": \"1\"}, {\"modelKey\": \"gemini-pro\", \"modelType\": \"1\"}, {\"modelKey\": \"text-embedding-004\", \"modelType\": \"2\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (3, 'Ollama', 'ollama', '2', 'http://localhost:11434', 'https://ollama.com', '/model-provider-icon/ollama.png', NULL, '0', 3, '[{\"modelKey\": \"llama3\", \"modelType\": \"1\"}, {\"modelKey\": \"llama2\", \"modelType\": \"1\"}, {\"modelKey\": \"mistral\", \"modelType\": \"1\"}, {\"modelKey\": \"mixtral\", \"modelType\": \"1\"}, {\"modelKey\": \"phi3\", \"modelType\": \"1\"}, {\"modelKey\": \"qwen2\", \"modelType\": \"1\"}, {\"modelKey\": \"gemma2\", \"modelType\": \"1\"}, {\"modelKey\": \"nomic-embed-text\", \"modelType\": \"2\"}, {\"modelKey\": \"mxbai-embed-large\", \"modelType\": \"2\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (4, 'DeepSeek', 'deepseek', '1', 'https://api.deepseek.com', 'https://www.deepseek.com', '/model-provider-icon/deepseek.png', NULL, '0', 4, '[{\"modelKey\": \"deepseek-chat\", \"modelType\": \"1\"}, {\"modelKey\": \"deepseek-coder\", \"modelType\": \"1\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (5, 'vLLM', 'vllm', '2', 'http://localhost:8000/v1', 'https://docs.vllm.ai', '/model-provider-icon/vllm.ico', NULL, '0', 5, '[]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (6, 'Azure OpenAI', 'azure', '1', 'https://{resource}.openai.azure.com', 'https://azure.microsoft.com/products/ai-services/openai-service', '/model-provider-icon/azure.png', NULL, '0', 6, '[{\"modelKey\": \"gpt-4\", \"modelType\": \"1\"}, {\"modelKey\": \"gpt-4-turbo\", \"modelType\": \"1\"}, {\"modelKey\": \"gpt-35-turbo\", \"modelType\": \"1\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (7, '阿里云百炼', 'bailian', '1', 'https://dashscope.aliyuncs.com/api/v1', 'https://www.aliyun.com/product/bailian', '/model-provider-icon/bailian.jpeg', NULL, '0', 7, '[{\"modelKey\": \"qwen-max\", \"modelType\": \"1\"}, {\"modelKey\": \"qwen-plus\", \"modelType\": \"1\"}, {\"modelKey\": \"qwen-turbo\", \"modelType\": \"1\"}, {\"modelKey\": \"text-embedding-v1\", \"modelType\": \"2\"}, {\"modelKey\": \"text-embedding-v2\", \"modelType\": \"2\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (8, '智谱AI', 'zhipu', '1', 'https://open.bigmodel.cn/api/paas/v4', 'https://open.bigmodel.cn', '/model-provider-icon/zhipu.png', NULL, '0', 8, '[{\"modelKey\": \"glm-4\", \"modelType\": \"1\"}, {\"modelKey\": \"glm-4-flash\", \"modelType\": \"1\"}, {\"modelKey\": \"glm-3-turbo\", \"modelType\": \"1\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (9, '豆包', 'doubao', '1', 'https://ark.cn-beijing.volces.com/api/v3', 'https://www.volcengine.com/product/doubao', '/model-provider-icon/doubao.png', NULL, '0', 9, '[{\"modelKey\": \"doubao-pro-32k\", \"modelType\": \"1\"}, {\"modelKey\": \"doubao-lite-32k\", \"modelType\": \"1\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);
INSERT INTO `km_model_provider`(`provider_id`, `provider_name`, `provider_key`, `provider_type`, `default_endpoint`, `site_url`, `icon_url`, `config_schema`, `status`, `sort`, `models`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`) VALUES (10, 'Moonshot', 'moonshot', '1', 'https://api.moonshot.cn/v1', 'https://www.moonshot.cn', '/model-provider-icon/moonshot.ico', NULL, '0', 10, '[{\"modelKey\": \"moonshot-v1-8k\", \"modelType\": \"1\"}, {\"modelKey\": \"moonshot-v1-32k\", \"modelType\": \"1\"}, {\"modelKey\": \"moonshot-v1-128k\", \"modelType\": \"1\"}]', NULL, NULL, '2025-12-25 22:41:24', NULL, NULL, '0', NULL);

-- ----------------------------
-- 1. 模型管理表
-- ----------------------------
DROP TABLE IF EXISTS km_model;
CREATE TABLE km_model (
    model_id        BIGINT(20)      NOT NULL COMMENT '模型ID',
    provider_id     BIGINT(20)      DEFAULT NULL COMMENT '关联供应商ID',
    model_name      VARCHAR(64)     NOT NULL COMMENT '模型名称',
    model_type      CHAR(1)         NOT NULL COMMENT '模型类型（1语言模型 2向量模型）',
    `model_key` varchar(100) NOT NULL COMMENT '基础模型 (e.g. gpt-4)',
    api_key         VARCHAR(255)    DEFAULT '' COMMENT 'API Key',
    api_base        VARCHAR(255)    DEFAULT '' COMMENT 'API Base URL',
    config          JSON            DEFAULT NULL COMMENT '其它配置参数',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0正常 1停用）',
    is_builtin      CHAR(1)         DEFAULT 'N' COMMENT '是否内置（Y是 N否）',
    model_source    CHAR(1)         DEFAULT '1' COMMENT '模型来源（1公有 2本地）',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (model_id)
) ENGINE=InnoDB COMMENT='AI模型配置表';

-- ----------------------------
-- 2. 知识库表
-- ----------------------------
DROP TABLE IF EXISTS km_knowledge;
CREATE TABLE km_knowledge (
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '知识库ID',
    knowledge_name  VARCHAR(64)     NOT NULL COMMENT '知识库名称',
    description     VARCHAR(500)    DEFAULT '' COMMENT '描述',
    embed_model_id  BIGINT(20)      DEFAULT NULL COMMENT '关联的向量模型ID',
    index_name      VARCHAR(64)     DEFAULT '' COMMENT 'ES索引名称',
    permission      CHAR(1)         DEFAULT '1' COMMENT '权限范围（1私有 2公开）',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (knowledge_id)
) ENGINE=InnoDB COMMENT='知识库表';

-- ----------------------------
-- 3. 知识库文档表
-- ----------------------------
DROP TABLE IF EXISTS km_document;
CREATE TABLE km_document (
    doc_id          BIGINT(20)      NOT NULL COMMENT '文档ID',
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '所属知识库ID',
    file_name       VARCHAR(128)    NOT NULL COMMENT '文件名',
    file_url        VARCHAR(500)    NOT NULL COMMENT '文件路径',
    file_type       VARCHAR(10)     DEFAULT '' COMMENT '文件类型',
    file_size       BIGINT(20)      DEFAULT 0 COMMENT '文件大小',
    char_count      INT(11)         DEFAULT 0 COMMENT '字符数',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0待解析 1解析中 2完成 3失败）',
    error_msg       VARCHAR(1000)   DEFAULT '' COMMENT '错误信息',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (doc_id),
    KEY idx_kb_id (knowledge_id)
) ENGINE=InnoDB COMMENT='知识库文档表';

-- ----------------------------
-- 3.1 文档分段表 (Paragraph)
-- ----------------------------
DROP TABLE IF EXISTS km_paragraph;
CREATE TABLE km_paragraph (
    paragraph_id    BIGINT(20)      NOT NULL COMMENT '分段ID',
    doc_id          BIGINT(20)      NOT NULL COMMENT '所属文档ID',
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '所属知识库ID',
    content         LONGTEXT        NOT NULL COMMENT '分段内容',
    title           VARCHAR(255)    DEFAULT '' COMMENT '分段标题',
    status          CHAR(1)         DEFAULT '1' COMMENT '状态（1启用 0禁用）',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (paragraph_id),
    KEY idx_doc_id (doc_id),
    KEY idx_kb_id (knowledge_id)
) ENGINE=InnoDB COMMENT='文档分段表';

-- ----------------------------
-- 3.2 相关问题表 (Problem)
-- ----------------------------
DROP TABLE IF EXISTS km_problem;
CREATE TABLE km_problem (
    problem_id      BIGINT(20)      NOT NULL COMMENT '问题ID',
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '所属知识库ID',
    content         VARCHAR(500)    NOT NULL COMMENT '问题内容',
    hit_count       INT(11)         DEFAULT 0 COMMENT '命中次数',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (problem_id),
    KEY idx_kb_id (knowledge_id)
) ENGINE=InnoDB COMMENT='知识库相关问题表';

-- ----------------------------
-- 3.3 问题与分段关联表 (Mapping)
-- ----------------------------
DROP TABLE IF EXISTS km_problem_paragraph;
CREATE TABLE km_problem_paragraph (
    id              BIGINT(20)      NOT NULL COMMENT '主键',
    problem_id      BIGINT(20)      NOT NULL COMMENT '问题ID',
    paragraph_id    BIGINT(20)      NOT NULL COMMENT '分段ID',
    doc_id          BIGINT(20)      NOT NULL COMMENT '文档ID',
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '知识库ID',
    PRIMARY KEY (id),
    KEY idx_problem (problem_id),
    KEY idx_paragraph (paragraph_id)
) ENGINE=InnoDB COMMENT='问题与分段关联表';

-- ----------------------------
-- 4. AI应用表
-- ----------------------------
DROP TABLE IF EXISTS km_app;
CREATE TABLE km_app (
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    app_name        VARCHAR(64)     NOT NULL COMMENT '应用名称',
    description     VARCHAR(500)    DEFAULT '' COMMENT '应用描述',
    icon            VARCHAR(255)    DEFAULT '' COMMENT '应用图标',
    app_type        CHAR(1)         DEFAULT '1' COMMENT '应用类型（1基础对话 2工作流）',
    status          CHAR(1)         DEFAULT '0' COMMENT '状态（0草稿 1发布）',
    prologue        VARCHAR(1000)   DEFAULT '' COMMENT '开场白',

    -- 配置 (JSONB in Postgres, JSON in MySQL)
    model_setting   JSON            DEFAULT NULL COMMENT '模型配置(JSON)',
    knowledge_setting JSON          DEFAULT NULL COMMENT '知识库配置(JSON)',
    workflow_config JSON            DEFAULT NULL COMMENT '工作流配置(JSON,已废弃,使用dsl_data)',
    graph_data      JSON            DEFAULT NULL COMMENT '前端画布数据(JSON)',
    dsl_data        JSON            DEFAULT NULL COMMENT '工作流DSL配置(JSON)',
    parameters      JSON            DEFAULT NULL COMMENT '应用参数配置(全局/接口/会话)',
    -- 基础字段
    model_id        BIGINT(20)      DEFAULT NULL COMMENT '关联模型ID',

    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (app_id)
) ENGINE=InnoDB COMMENT='AI应用表';

-- ----------------------------
-- 4.1 应用-知识库关联表
-- ----------------------------
DROP TABLE IF EXISTS km_app_knowledge;
CREATE TABLE km_app_knowledge (
    id              BIGINT(20)      NOT NULL COMMENT 'ID',
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    knowledge_id    BIGINT(20)      NOT NULL COMMENT '知识库ID',
    sort            INT(4)          DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_kb (app_id, knowledge_id)
) ENGINE=InnoDB COMMENT='应用-知识库关联表';

-- ----------------------------
-- 4.2 应用版本表
-- ----------------------------
DROP TABLE IF EXISTS km_app_version;
CREATE TABLE km_app_version (
    version_id      BIGINT(20)      NOT NULL COMMENT '版本ID',
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    version         INT(11)         NOT NULL COMMENT '版本号',
    app_snapshot    JSON            NOT NULL COMMENT '应用配置快照',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '版本说明',
    PRIMARY KEY (version_id),
    KEY idx_app_version (app_id, version)
) ENGINE=InnoDB COMMENT='应用历史版本表';

-- ----------------------------
-- 4.3 应用访问统计表
-- ----------------------------
DROP TABLE IF EXISTS km_app_access_stat;
CREATE TABLE km_app_access_stat (
    id              BIGINT(20)      NOT NULL COMMENT 'ID',
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    user_id         BIGINT(20)      NOT NULL COMMENT '用户ID',
    access_count    BIGINT(20)      DEFAULT 0 COMMENT '总访问次数',
    last_access_time DATETIME       DEFAULT NULL COMMENT '最后访问时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user (app_id, user_id)
) ENGINE=InnoDB COMMENT='应用访问统计表';

-- ----------------------------
-- 5. 工作流定义表 (已废弃,工作流数据存储在 km_app 表中)
-- ----------------------------
-- DROP TABLE IF EXISTS km_workflow;

-- ----------------------------
-- 5.1 工作流实例表
-- ----------------------------
DROP TABLE IF EXISTS km_workflow_instance;
CREATE TABLE km_workflow_instance (
    instance_id     BIGINT(20)      NOT NULL COMMENT '实例ID',
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    session_id      BIGINT(20)      NOT NULL COMMENT '会话ID',
    workflow_config JSON            NOT NULL COMMENT '工作流配置快照(JSON)',
    status          VARCHAR(20)     NOT NULL COMMENT '实例状态(RUNNING/PAUSED/COMPLETED/FAILED)',
    current_node    VARCHAR(64)     DEFAULT NULL COMMENT '当前执行节点ID',
    global_state    JSON            DEFAULT NULL COMMENT '全局状态数据',
    start_time      DATETIME        DEFAULT NULL COMMENT '开始时间',
    end_time        DATETIME        DEFAULT NULL COMMENT '结束时间',
    error_message   VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (instance_id),
    KEY idx_app_session (app_id, session_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='工作流实例表';

-- ----------------------------
-- 5.2 节点执行记录表
-- ----------------------------
DROP TABLE IF EXISTS km_node_execution;
CREATE TABLE km_node_execution (
    execution_id    BIGINT(20)      NOT NULL COMMENT '执行ID',
    instance_id     BIGINT(20)      NOT NULL COMMENT '实例ID',
    node_id         VARCHAR(64)     NOT NULL COMMENT '节点ID',
    node_type       VARCHAR(64)     NOT NULL COMMENT '节点类型',
    status          VARCHAR(20)     NOT NULL COMMENT '执行状态(PENDING/RUNNING/COMPLETED/FAILED/SKIPPED)',
    input_params    JSON            DEFAULT NULL COMMENT '输入参数',
    output_params   JSON            DEFAULT NULL COMMENT '输出参数',
    start_time      DATETIME        DEFAULT NULL COMMENT '开始时间',
    end_time        DATETIME        DEFAULT NULL COMMENT '结束时间',
    error_message   VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    retry_count     INT(4)          DEFAULT 0 COMMENT '重试次数',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (execution_id),
    KEY idx_instance (instance_id),
    KEY idx_node (instance_id, node_id)
) ENGINE=InnoDB COMMENT='节点执行记录表';

-- ----------------------------
-- 6. 聊天会话表
-- ----------------------------
DROP TABLE IF EXISTS km_chat_session;
CREATE TABLE km_chat_session (
    session_id      BIGINT(20)      NOT NULL COMMENT '会话ID',
    app_id          BIGINT(20)      NOT NULL COMMENT '应用ID',
    title           VARCHAR(128)    DEFAULT '新会话' COMMENT '会话标题',
    user_id         BIGINT(20)      NOT NULL COMMENT '用户ID',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (session_id),
    KEY idx_user_app (user_id, app_id)
) ENGINE=InnoDB COMMENT='聊天会话表';

-- ----------------------------
-- 7. 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS km_chat_message;
CREATE TABLE km_chat_message (
    message_id      BIGINT(20)      NOT NULL COMMENT '消息ID',
    instance_id     BIGINT(20)      NOT NULL COMMENT '实例ID',
    session_id      BIGINT(20)      NOT NULL COMMENT '会话ID',
    role            VARCHAR(20)     NOT NULL COMMENT '角色(user/assistant)',
    content         LONGTEXT        COMMENT '消息内容',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (message_id),
    KEY idx_session (session_id)
) ENGINE=InnoDB COMMENT='聊天消息表';

-- ----------------------------
-- 8. 工具管理表
-- ----------------------------
DROP TABLE IF EXISTS km_tool;
CREATE TABLE km_tool (
    tool_id         BIGINT(20)      NOT NULL COMMENT '工具ID',
    tool_name       VARCHAR(64)     NOT NULL COMMENT '工具名称(唯一标识)',
    tool_label      VARCHAR(64)     NOT NULL COMMENT '显示名称',
    description     VARCHAR(500)    DEFAULT '' COMMENT '描述',
    tool_type       CHAR(1)         NOT NULL COMMENT '类型（1内置 2API 3MCP）',
    icon            VARCHAR(255)    DEFAULT '' COMMENT '图标',
    input_params_schema   JSON      DEFAULT NULL COMMENT '输入参数定义(JSON Schema)',
    init_params_schema   JSON      DEFAULT NULL COMMENT '初始化参数定义(JSON Schema)',
    api_spec        LONGTEXT        DEFAULT NULL COMMENT 'OpenAPI定义',
    mcp_config      JSON            DEFAULT NULL COMMENT 'MCP连接配置',
    create_dept     BIGINT(20)      DEFAULT NULL COMMENT '创建部门',
    create_by       BIGINT(20)      DEFAULT NULL COMMENT '创建者',
    create_time     DATETIME        DEFAULT NULL COMMENT '创建时间',
    update_by       BIGINT(20)      DEFAULT NULL COMMENT '更新者',
    update_time     DATETIME        DEFAULT NULL COMMENT '更新时间',
    del_flag        CHAR(1)         DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (tool_id)
) ENGINE=InnoDB COMMENT='AI工具表';

-- ----------------------------
-- 工作流节点定义相关表 (Workflow Node Definition)
-- ----------------------------

-- ----------------------------
-- 10. 节点定义表
-- ----------------------------
DROP TABLE IF EXISTS km_node_definition;
CREATE TABLE km_node_definition (
    -- 主键
    node_def_id         BIGINT(20)      NOT NULL    COMMENT '节点定义ID',    
    -- 基本信息
    node_type           VARCHAR(100)    NOT NULL    COMMENT '节点类型标识 (如 LLM_CHAT)',
    node_label          VARCHAR(200)    NOT NULL    COMMENT '节点显示名称',
    node_icon           VARCHAR(200)                COMMENT '节点图标 (Iconify)',
    node_color          VARCHAR(50)                 COMMENT '节点颜色 (HEX)',
    category            VARCHAR(50)     NOT NULL    COMMENT '节点分类 (basic/ai/logic/action)',
    description         VARCHAR(500)                COMMENT '节点描述',    
    -- 系统标识
    is_system           CHAR(1)         DEFAULT '0' COMMENT '是否系统节点 (0否/1是)',
    is_enabled          CHAR(1)         DEFAULT '1' COMMENT '是否启用 (0停用/1启用)',    
    -- 参数定义 (JSON Array)
    input_params        TEXT                        COMMENT '输入参数定义 (JSON Array)',
    output_params       TEXT                        COMMENT '输出参数定义 (JSON Array)',    
    allow_custom_input_params CHAR(1) DEFAULT '0' COMMENT '是否允许自定义输入参数 (0否/1是)',
    allow_custom_output_params CHAR(1) DEFAULT '0' COMMENT '是否允许自定义输出参数 (0否/1是)',
    -- 版本管理
    version             INT(4)          DEFAULT 1   COMMENT '版本号',
    parent_version_id   BIGINT(20)                  COMMENT '父版本ID (用于追溯历史)',    
    -- BaseEntity审计字段
    create_dept         BIGINT(20)                  COMMENT '创建部门',
    create_by           BIGINT(20)                  COMMENT '创建者',
    create_time         DATETIME                    COMMENT '创建时间',
    update_by           BIGINT(20)                  COMMENT '更新者',
    update_time         DATETIME                    COMMENT '更新时间',
    remark              VARCHAR(500)                COMMENT '备注',    
    PRIMARY KEY (node_def_id),
    UNIQUE KEY uk_node_type_version (node_type, version),
    KEY idx_node_type (node_type),
    KEY idx_category (category)
) ENGINE=InnoDB COMMENT='工作流节点定义表';

-- ----------------------------
-- 11. 工作流模板表
-- ----------------------------
DROP TABLE IF EXISTS km_workflow_template;
CREATE TABLE km_workflow_template (
    -- 主键
    template_id         BIGINT(20)      NOT NULL    COMMENT '模板ID',    
    -- 基本信息
    template_name       VARCHAR(200)    NOT NULL    COMMENT '模板名称',
    template_code       VARCHAR(100)    NOT NULL    COMMENT '模板编码 (唯一标识)',
    description         VARCHAR(500)                COMMENT '模板描述',
    icon                VARCHAR(200)                COMMENT '模板图标',
    category            VARCHAR(50)                 COMMENT '模板分类 (客服/营销/知识问答等)',    
    -- 作用域控制
    scope_type          CHAR(1)         NOT NULL    COMMENT '作用域类型 (0系统级)',    
    -- 工作流配置 (完整JSON)
    workflow_config     TEXT            NOT NULL    COMMENT '工作流DSL配置 (WorkflowConfig JSON)',
    graph_data          TEXT                        COMMENT '前端画布数据 (Vue Flow JSON)',    
    -- 版本管理
    version             INT(4)          DEFAULT 1   COMMENT '版本号',
    parent_version_id   BIGINT(20)                  COMMENT '父版本ID',
    is_published        CHAR(1)         DEFAULT '0' COMMENT '是否已发布 (0否/1是)',
    publish_time        DATETIME                    COMMENT '发布时间',    
    -- 状态
    is_enabled          CHAR(1)         DEFAULT '1' COMMENT '是否启用 (0停用/1启用)',    
    -- 统计信息
    use_count           INT(4)          DEFAULT 0   COMMENT '使用次数',    
    -- BaseEntity审计字段
    create_dept         BIGINT(20)                  COMMENT '创建部门',
    create_by           BIGINT(20)                  COMMENT '创建者',
    create_time         DATETIME                    COMMENT '创建时间',
    update_by           BIGINT(20)                  COMMENT '更新者',
    update_time         DATETIME                    COMMENT '更新时间',
    remark              VARCHAR(500)                COMMENT '备注',    
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_scope_code (scope_type, template_code, version),
    KEY idx_category (category)
) ENGINE=InnoDB COMMENT='工作流模板表';

-- ----------------------------
-- 12. 节点连接规则表
-- ----------------------------
DROP TABLE IF EXISTS km_node_connection_rule;
CREATE TABLE km_node_connection_rule (
    -- 主键
    rule_id             BIGINT(20)      NOT NULL    COMMENT '规则ID',
    -- 节点连接规则
    source_node_type    VARCHAR(100)    NOT NULL    COMMENT '源节点类型',
    target_node_type    VARCHAR(100)    NOT NULL    COMMENT '目标节点类型',
    -- 规则类型
    rule_type           CHAR(1)         NOT NULL    COMMENT '规则类型 (0允许连接/1禁止连接)',
    -- 优先级
    priority            INT(4)          DEFAULT 0   COMMENT '优先级 (数值越大优先级越高)',
    -- 状态
    is_enabled          CHAR(1)         DEFAULT '1' COMMENT '是否启用 (0停用/1启用)',
    -- BaseEntity审计字段
    create_dept         BIGINT(20)                  COMMENT '创建部门',
    create_by           BIGINT(20)                  COMMENT '创建者',
    create_time         DATETIME                   COMMENT '创建时间',
    update_by           BIGINT(20)                  COMMENT '更新者',
    update_time         DATETIME                    COMMENT '更新时间',
    remark              VARCHAR(500)                COMMENT '备注',
    
    PRIMARY KEY (rule_id),
    UNIQUE KEY uk_connection (source_node_type, target_node_type),
    KEY idx_source_type (source_node_type)
) ENGINE=InnoDB COMMENT='节点连接规则表';

-- ----------------------------
-- 9. 菜单初始化 (Menu Init)
-- ----------------------------
-- 一级菜单: AI 管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2000, 'AI 管理', 0, 10, 'ai', NULL, 1, 0, 'M', '0', '0', '', 'robot', 1, NOW(), NULL, NULL, 'AI模块根菜单');

-- 二级菜单: 模型管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2001, '模型管理', 2000, 1, 'model-manager', 'ai/model-manager/index', 1, 0, 'C', '0', '0', 'ai:model:list', 'model-alt', 1, NOW(), NULL, NULL, '模型管理菜单');

-- 角色菜单关联 (给超级管理员授权)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2000);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2001);
