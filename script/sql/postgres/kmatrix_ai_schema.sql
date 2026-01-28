-- ----------------------------
-- KMatrix AI 模块全量数据库脚本 (PostgreSQL 版 - 整合版)
-- 整合日期: 2026-01-27
-- 包含内容：基础表结构 + 所有增量字段 + pgvector 集成 + 完整初始化数据
-- ----------------------------

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ----------------------------
-- 0. 模型供应商表
-- ----------------------------
DROP TABLE IF EXISTS km_model_provider CASCADE;
CREATE TABLE km_model_provider (
    provider_id     BIGINT          NOT NULL,
    provider_name   VARCHAR(64)     NOT NULL,
    provider_key    VARCHAR(64)     NOT NULL,
    provider_type   CHAR(1)         DEFAULT '1',
    default_endpoint VARCHAR(255)   DEFAULT '',
    site_url        VARCHAR(255)    DEFAULT '',
    icon_url        VARCHAR(500)    DEFAULT '',
    config_schema   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    sort            INTEGER         DEFAULT 0,
    models          JSONB           DEFAULT NULL,
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (provider_id),
    CONSTRAINT uk_provider_key UNIQUE (provider_key)
);

COMMENT ON TABLE km_model_provider IS '模型供应商表';
COMMENT ON COLUMN km_model_provider.provider_id IS '供应商ID';
COMMENT ON COLUMN km_model_provider.provider_name IS '供应商名称';
COMMENT ON COLUMN km_model_provider.provider_key IS '供应商标识(openai/ollama)';
COMMENT ON COLUMN km_model_provider.provider_type IS '供应商类型（1公用 2本地）';
COMMENT ON COLUMN km_model_provider.default_endpoint IS '默认API地址';
COMMENT ON COLUMN km_model_provider.site_url IS '官网URL';
COMMENT ON COLUMN km_model_provider.icon_url IS '图标URL';
COMMENT ON COLUMN km_model_provider.config_schema IS '配置参数定义';
COMMENT ON COLUMN km_model_provider.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN km_model_provider.sort IS '排序';
COMMENT ON COLUMN km_model_provider.models IS '支持的模型标识(JSON)';

-- ----------------------------
-- 1. 模型管理表
-- ----------------------------
DROP TABLE IF EXISTS km_model CASCADE;
CREATE TABLE km_model (
    model_id        BIGINT          NOT NULL,
    provider_id     BIGINT          DEFAULT NULL,
    model_name      VARCHAR(64)     NOT NULL,
    model_type      CHAR(1)         NOT NULL,
    model_key       VARCHAR(100)    NOT NULL,
    api_key         VARCHAR(255)    DEFAULT '',
    api_base        VARCHAR(255)    DEFAULT '',
    config          JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    is_builtin      CHAR(1)         DEFAULT 'N',
    model_source    CHAR(1)         DEFAULT '1',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (model_id)
);

COMMENT ON TABLE km_model IS 'AI模型配置表';

-- ----------------------------
-- 2. 知识库表
-- ----------------------------
DROP TABLE IF EXISTS km_knowledge CASCADE;
CREATE TABLE km_knowledge (
    knowledge_id    BIGINT          NOT NULL,
    knowledge_name  VARCHAR(64)     NOT NULL,
    description     VARCHAR(500)    DEFAULT '',
    embed_model_id  BIGINT          DEFAULT NULL,
    index_name      VARCHAR(64)     DEFAULT '',
    permission      CHAR(1)         DEFAULT '1',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (knowledge_id)
);

-- ----------------------------
-- 3. 知识库文档表
-- ----------------------------
DROP TABLE IF EXISTS km_document CASCADE;
CREATE TABLE km_document (
    doc_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    file_name       VARCHAR(128)    NOT NULL,
    file_url        VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(10)     DEFAULT '',
    file_size       BIGINT          DEFAULT 0,
    char_count      INTEGER         DEFAULT 0,
    status          CHAR(1)         DEFAULT '0',
    error_msg       VARCHAR(1000)   DEFAULT '',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (doc_id)
);
CREATE INDEX idx_km_doc_kb_id ON km_document(knowledge_id);

-- ----------------------------
-- 3.1 文档分段表 (Paragraph) 并集成 pgvector
-- ----------------------------
DROP TABLE IF EXISTS km_paragraph CASCADE;
CREATE TABLE km_paragraph (
    paragraph_id    BIGINT          NOT NULL,
    doc_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    title           VARCHAR(255)    DEFAULT '',
    status          CHAR(1)         DEFAULT '1',
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    embedding       vector(1536)    DEFAULT NULL, -- 向量字段
    PRIMARY KEY (paragraph_id)
);
CREATE INDEX idx_km_para_doc_id ON km_paragraph(doc_id);
CREATE INDEX idx_km_para_kb_id ON km_paragraph(knowledge_id);
-- HNSW 索引
CREATE INDEX idx_km_para_embedding ON km_paragraph USING hnsw (embedding vector_cosine_ops);

-- ----------------------------
-- 3.2 相关问题表 (Problem) 并集成 pgvector
-- ----------------------------
DROP TABLE IF EXISTS km_problem CASCADE;
CREATE TABLE km_problem (
    problem_id      BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    hit_count       INTEGER         DEFAULT 0,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    embedding       vector(1536)    DEFAULT NULL, -- 问题向量
    PRIMARY KEY (problem_id)
);
CREATE INDEX idx_km_prob_kb_id ON km_problem(knowledge_id);
CREATE INDEX idx_km_prob_embedding ON km_problem USING hnsw (embedding vector_cosine_ops);

-- ----------------------------
-- 3.3 问题与分段关联表
-- ----------------------------
DROP TABLE IF EXISTS km_problem_paragraph CASCADE;
CREATE TABLE km_problem_paragraph (
    id              BIGINT          NOT NULL,
    problem_id      BIGINT          NOT NULL,
    paragraph_id    BIGINT          NOT NULL,
    doc_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 4. AI应用表 (整合 parameters, enable_execution_detail, public_access)
-- ----------------------------
DROP TABLE IF EXISTS km_app CASCADE;
CREATE TABLE km_app (
    app_id          BIGINT          NOT NULL,
    app_name        VARCHAR(64)     NOT NULL,
    description     VARCHAR(500)    DEFAULT '',
    icon            VARCHAR(255)    DEFAULT '',
    app_type        CHAR(1)         DEFAULT '1',
    status          CHAR(1)         DEFAULT '0',
    prologue        VARCHAR(1000)   DEFAULT '',
    model_setting   JSONB           DEFAULT NULL,
    knowledge_setting JSONB         DEFAULT NULL,
    workflow_config JSONB           DEFAULT NULL,
    graph_data      JSONB           DEFAULT NULL,
    dsl_data        JSONB           DEFAULT NULL,
    parameters      JSONB           DEFAULT NULL, -- 新增
    model_id        BIGINT          DEFAULT NULL,
    enable_execution_detail CHAR(1) DEFAULT '0', -- 新增
    public_access   CHAR(1)         DEFAULT '1', -- 新增
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (app_id)
);

COMMENT ON COLUMN km_app.parameters IS '应用参数配置(全局/接口/会话)';
COMMENT ON COLUMN km_app.enable_execution_detail IS '是否启用执行详情（0禁用 1启用）';
COMMENT ON COLUMN km_app.public_access IS '公开访问（0关闭 1开启）';

-- ----------------------------
-- 4.1 应用关联表 (Token, 统计, 知识库)
-- ----------------------------
DROP TABLE IF EXISTS km_app_knowledge CASCADE;
CREATE TABLE km_app_knowledge (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    sort            INTEGER         DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_kb UNIQUE (app_id, knowledge_id)
);

DROP TABLE IF EXISTS km_app_version CASCADE;
CREATE TABLE km_app_version (
    version_id      BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    version         INTEGER         NOT NULL,
    app_snapshot    JSONB           NOT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (version_id)
);

DROP TABLE IF EXISTS km_app_access_stat CASCADE;
CREATE TABLE km_app_access_stat (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    access_count    BIGINT          DEFAULT 0,
    last_access_time TIMESTAMP      DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_user UNIQUE (app_id, user_id)
);

-- 新增：App嵌入Token表
DROP TABLE IF EXISTS km_app_token CASCADE;
CREATE TABLE km_app_token (
    token_id        BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    token           VARCHAR(64)     NOT NULL,
    token_name      VARCHAR(100)    NOT NULL,
    allowed_origins VARCHAR(500)    DEFAULT '*',
    expires_at      TIMESTAMP       DEFAULT NULL,
    status          CHAR(1)         DEFAULT '1',
    remark          VARCHAR(500)    DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    PRIMARY KEY (token_id),
    CONSTRAINT uk_km_app_token UNIQUE (token)
);
CREATE INDEX idx_km_app_token_app ON km_app_token(app_id);

-- ----------------------------
-- 5. 数据源与元数据 (SQL查询节点依赖)
-- ----------------------------
DROP TABLE IF EXISTS km_data_source CASCADE;
CREATE TABLE km_data_source (
    data_source_id      BIGINT          NOT NULL,
    data_source_name    VARCHAR(200)    NOT NULL,
    source_type         VARCHAR(20)     NOT NULL,
    ds_key              VARCHAR(100),
    driver_class_name   VARCHAR(200),
    jdbc_url            VARCHAR(500),
    username            VARCHAR(100),
    password            VARCHAR(500),
    db_type             VARCHAR(50),
    is_enabled          CHAR(1)         DEFAULT '1',
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (data_source_id),
    CONSTRAINT uk_km_ds_name UNIQUE (data_source_name)
);

DROP TABLE IF EXISTS km_database_meta CASCADE;
CREATE TABLE km_database_meta (
    meta_id             BIGINT          NOT NULL,
    data_source_id      BIGINT          NOT NULL,
    meta_source_type    VARCHAR(20)     NOT NULL,
    ddl_content         TEXT,
    table_name          VARCHAR(200)    NOT NULL,
    table_comment       VARCHAR(500),
    columns             JSONB           DEFAULT NULL,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (meta_id),
    CONSTRAINT uk_km_ds_table UNIQUE (data_source_id, table_name)
);

-- ----------------------------
-- 6. 工作流执行记录 (整合 Token 统计及耗时)
-- ----------------------------
DROP TABLE IF EXISTS km_workflow_instance CASCADE;
CREATE TABLE km_workflow_instance (
    instance_id     BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    workflow_config JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    current_node    VARCHAR(64),
    global_state    JSONB,
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    error_message   VARCHAR(1000),
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    PRIMARY KEY (instance_id)
);

DROP TABLE IF EXISTS km_node_execution CASCADE;
CREATE TABLE km_node_execution (
    execution_id    BIGINT          NOT NULL,
    instance_id     BIGINT          NOT NULL,
    node_id         VARCHAR(64)     NOT NULL,
    node_type       VARCHAR(64)     NOT NULL,
    node_name       VARCHAR(200), -- 新增
    status          VARCHAR(20)     NOT NULL,
    input_params    JSONB,
    output_params   JSONB,
    input_tokens    INTEGER         DEFAULT 0, -- 新增
    output_tokens   INTEGER         DEFAULT 0, -- 新增
    total_tokens    INTEGER         DEFAULT 0, -- 新增
    duration_ms     BIGINT,        -- 新增
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    error_message   VARCHAR(1000),
    retry_count     INTEGER         DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    PRIMARY KEY (execution_id)
);

-- ----------------------------
-- 7. 聊天会话与消息 (整合 user_type)
-- ----------------------------
DROP TABLE IF EXISTS km_chat_session CASCADE;
CREATE TABLE km_chat_session (
    session_id      BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    title           VARCHAR(128)    DEFAULT '新会话',
    user_id         BIGINT          NOT NULL,
    user_type       VARCHAR(20)     DEFAULT 'system_user', -- 新增
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    del_flag        CHAR(1)         DEFAULT '0',
    PRIMARY KEY (session_id)
);
COMMENT ON COLUMN km_chat_session.user_type IS '用户类型 (anonymous_user/system_user/third_user)';

DROP TABLE IF EXISTS km_chat_message CASCADE;
CREATE TABLE km_chat_message (
    message_id      BIGINT          NOT NULL,
    instance_id     BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    content         TEXT,
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    PRIMARY KEY (message_id)
);

-- ----------------------------
-- 8. 工作流节点定义 (整合自定义参数控制)
-- ----------------------------
DROP TABLE IF EXISTS km_node_definition CASCADE;
CREATE TABLE km_node_definition (
    node_def_id         BIGINT          NOT NULL,
    node_type           VARCHAR(100)    NOT NULL,
    node_label          VARCHAR(200)    NOT NULL,
    node_icon           VARCHAR(200),
    node_color          VARCHAR(50),
    category            VARCHAR(50)     NOT NULL,
    description         VARCHAR(500),
    is_system           CHAR(1)         DEFAULT '0',
    is_enabled          CHAR(1)         DEFAULT '1',
    allow_custom_input_params CHAR(1) DEFAULT '0', -- 新增
    allow_custom_output_params CHAR(1) DEFAULT '0', -- 新增
    input_params        TEXT,
    output_params       TEXT,
    version             INTEGER         DEFAULT 1,
    parent_version_id   BIGINT,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (node_def_id),
    CONSTRAINT uk_km_node_type_ver UNIQUE (node_type, version)
);

DROP TABLE IF EXISTS km_workflow_template CASCADE;
CREATE TABLE km_workflow_template (
    template_id         BIGINT          NOT NULL,
    template_name       VARCHAR(200)    NOT NULL,
    template_code       VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),
    icon                VARCHAR(200),
    category            VARCHAR(50),
    scope_type          CHAR(1)         NOT NULL,
    workflow_config     TEXT            NOT NULL,
    graph_data          TEXT,
    version             INTEGER         DEFAULT 1,
    parent_version_id   BIGINT,
    is_published        CHAR(1)         DEFAULT '0',
    publish_time        TIMESTAMP,
    is_enabled          CHAR(1)         DEFAULT '1',
    use_count           INTEGER         DEFAULT 0,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (template_id),
    CONSTRAINT uk_km_tpl_scope_code UNIQUE (scope_type, template_code, version)
);

DROP TABLE IF EXISTS km_node_connection_rule CASCADE;
CREATE TABLE km_node_connection_rule (
    rule_id             BIGINT          NOT NULL,
    source_node_type    VARCHAR(100)    NOT NULL,
    target_node_type    VARCHAR(100)    NOT NULL,
    rule_type           CHAR(1)         NOT NULL,
    priority            INTEGER         DEFAULT 0,
    is_enabled          CHAR(1)         DEFAULT '1',
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (rule_id),
    CONSTRAINT uk_km_node_conn UNIQUE (source_node_type, target_node_type)
);

DROP TABLE IF EXISTS km_tool CASCADE;
CREATE TABLE km_tool (
    tool_id         BIGINT          NOT NULL,
    tool_name       VARCHAR(64)     NOT NULL,
    tool_label      VARCHAR(64)     NOT NULL,
    description     VARCHAR(500)    DEFAULT '',
    tool_type       CHAR(1)         NOT NULL,
    icon            VARCHAR(255)    DEFAULT '',
    input_params_schema JSONB       DEFAULT NULL,
    init_params_schema JSONB        DEFAULT NULL,
    api_spec        TEXT,
    mcp_config      JSONB,
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500),
    PRIMARY KEY (tool_id)
);

-- ============================================================================
-- 初始化数据
-- ============================================================================

-- 1. 模型供应商数据 (整合最新 modelType 格式)
INSERT INTO km_model_provider(provider_id, provider_name, provider_key, provider_type, default_endpoint, site_url, icon_url, config_schema, status, sort, models, create_time) VALUES 
(1, 'OpenAI', 'openai', '1', 'https://api.openai.com/v1', 'https://openai.com', '/model-provider-icon/openai.png', NULL, '0', 1, '[{"modelKey": "gpt-4o", "modelType": "1"}, {"modelKey": "gpt-4o-mini", "modelType": "1"}, {"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-3.5-turbo", "modelType": "1"}, {"modelKey": "text-embedding-3-small", "modelType": "2"}, {"modelKey": "text-embedding-3-large", "modelType": "2"}, {"modelKey": "text-embedding-ada-002", "modelType": "2"}]', CURRENT_TIMESTAMP),
(2, 'Gemini', 'gemini', '1', 'https://generativelanguage.googleapis.com', 'https://ai.google.dev', '/model-provider-icon/gemini.svg', NULL, '0', 2, '[{"modelKey": "gemini-3-flash-preview", "modelType": "1"}, {"modelKey": "gemini-3-pro-preview", "modelType": "1"}, {"modelKey": "gemini-2.5-flash", "modelType": "1"}, {"modelKey": "text-embedding-004", "modelType": "2"}]', CURRENT_TIMESTAMP),
(3, 'Ollama', 'ollama', '2', 'http://localhost:11434', 'https://ollama.com', '/model-provider-icon/ollama.png', NULL, '0', 3, '[{"modelKey": "llama3", "modelType": "1"}, {"modelKey": "llama2", "modelType": "1"}, {"modelKey": "mistral", "modelType": "1"}, {"modelKey": "mixtral", "modelType": "1"}, {"modelKey": "phi3", "modelType": "1"}, {"modelKey": "qwen2", "modelType": "1"}, {"modelKey": "gemma2", "modelType": "1"}, {"modelKey": "nomic-embed-text", "modelType": "2"}, {"modelKey": "mxbai-embed-large", "modelType": "2"}]', CURRENT_TIMESTAMP),
(4, 'DeepSeek', 'deepseek', '1', 'https://api.deepseek.com', 'https://www.deepseek.com', '/model-provider-icon/deepseek.png', NULL, '0', 4, '[{"modelKey": "deepseek-chat", "modelType": "1"}, {"modelKey": "deepseek-coder", "modelType": "1"}]', CURRENT_TIMESTAMP),
(5, 'vLLM', 'vllm', '2', 'http://localhost:8000/v1', 'https://docs.vllm.ai', '/model-provider-icon/vllm.ico', NULL, '0', 5, '[]', CURRENT_TIMESTAMP),
(6, 'Azure OpenAI', 'azure', '1', 'https://{resource}.openai.azure.com', 'https://azure.microsoft.com/products/ai-services/openai-service', '/model-provider-icon/azure.png', NULL, '0', 6, '[{"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-4-turbo", "modelType": "1"}, {"modelKey": "gpt-35-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
(7, '阿里云百炼', 'bailian', '1', 'https://dashscope.aliyuncs.com/api/v1', 'https://www.aliyun.com/product/bailian', '/model-provider-icon/bailian.jpeg', NULL, '0', 7, '[{"modelKey": "qwen-max", "modelType": "1"}, {"modelKey": "qwen-plus", "modelType": "1"}, {"modelKey": "qwen-turbo", "modelType": "1"}, {"modelKey": "text-embedding-v1", "modelType": "2"}, {"modelKey": "text-embedding-v2", "modelType": "2"}]', CURRENT_TIMESTAMP),
(8, '智谱AI', 'zhipu', '1', 'https://open.bigmodel.cn/api/paas/v4', 'https://open.bigmodel.cn', '/model-provider-icon/zhipu.png', NULL, '0', 8, '[{"modelKey": "glm-4", "modelType": "1"}, {"modelKey": "glm-4-flash", "modelType": "1"}, {"modelKey": "glm-3-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
(9, '豆包', 'doubao', '1', 'https://ark.cn-beijing.volces.com/api/v3', 'https://www.volcengine.com/product/doubao', '/model-provider-icon/doubao.png', NULL, '0', 9, '[{"modelKey": "doubao-pro-32k", "modelType": "1"}, {"modelKey": "doubao-lite-32k", "modelType": "1"}]', CURRENT_TIMESTAMP),
(10, 'Moonshot', 'moonshot', '1', 'https://api.moonshot.cn/v1', 'https://www.moonshot.cn', '/model-provider-icon/moonshot.ico', NULL, '0', 10, '[{"modelKey": "moonshot-v1-8k", "modelType": "1"}, {"modelKey": "moonshot-v1-32k", "modelType": "1"}, {"modelKey": "moonshot-v1-128k", "modelType": "1"}]', CURRENT_TIMESTAMP);

-- 2. 节点定义数据 (1-10 号节点)
INSERT INTO km_node_definition(node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, allow_custom_input_params, allow_custom_output_params, input_params, output_params, version, parent_version_id, create_dept, create_by, create_time, update_by, update_time, remark) VALUES 
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

-- 3. 连接规则数据 (1-44 号规则)
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time) VALUES 
(1, 'START', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (2, 'START', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP), (3, 'START', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (4, 'START', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(5, 'LLM_CHAT', 'END', '0', 10, CURRENT_TIMESTAMP), (6, 'LLM_CHAT', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (7, 'LLM_CHAT', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (8, 'LLM_CHAT', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(9, 'INTENT_CLASSIFIER', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (10, 'INTENT_CLASSIFIER', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (11, 'INTENT_CLASSIFIER', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (12, 'INTENT_CLASSIFIER', 'END', '0', 10, CURRENT_TIMESTAMP),
(13, 'CONDITION', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (14, 'CONDITION', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (15, 'CONDITION', 'END', '0', 10, CURRENT_TIMESTAMP), (16, 'FIXED_RESPONSE', 'END', '0', 10, CURRENT_TIMESTAMP),
(17, 'START', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (18, 'LLM_CHAT', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (19, 'CONDITION', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (20, 'INTENT_CLASSIFIER', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
(21, 'DB_QUERY', 'END', '0', 10, CURRENT_TIMESTAMP), (22, 'DB_QUERY', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (23, 'DB_QUERY', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (24, 'DB_QUERY', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP), (25, 'DB_QUERY', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(26, 'START', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (27, 'LLM_CHAT', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (28, 'CONDITION', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (29, 'INTENT_CLASSIFIER', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
(30, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (31, 'SQL_GENERATE', 'END', '0', 10, CURRENT_TIMESTAMP), (32, 'SQL_GENERATE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (33, 'SQL_GENERATE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (34, 'SQL_GENERATE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(35, 'START', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (37, 'LLM_CHAT', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (38, 'CONDITION', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (39, 'INTENT_CLASSIFIER', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
(40, 'SQL_EXECUTE', 'END', '0', 10, CURRENT_TIMESTAMP), (41, 'SQL_EXECUTE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (42, 'SQL_EXECUTE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (43, 'SQL_EXECUTE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (44, 'SQL_EXECUTE', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP);

-- 4. 菜单数据
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES 
(2000, 'AI 管理', 0, 10, 'ai', NULL, 1, 0, 'M', '0', '0', '', 'robot', 1, CURRENT_TIMESTAMP, 'AI模块根菜单'),
(2001, '模型管理', 2000, 1, 'model-manager', 'ai/model-manager/index', 1, 0, 'C', '0', '0', 'ai:model:list', 'model-alt', 1, CURRENT_TIMESTAMP, '模型管理菜单'),
(2002, '应用管理', 2000, 2, 'app-manager', 'ai/app-manager/index', 1, 0, 'C', '0', '0', 'ai:app:list', 'app-store', 1, CURRENT_TIMESTAMP, 'AI应用管理菜单'),
(2003, '应用查询', 2002, 1, '', '', 1, 0, 'F', '0', '0', 'ai:app:query', '#', 1, CURRENT_TIMESTAMP, ''),
(2004, '应用新增', 2002, 2, '', '', 1, 0, 'F', '0', '0', 'ai:app:add', '#', 1, CURRENT_TIMESTAMP, ''),
(2005, '应用修改', 2002, 3, '', '', 1, 0, 'F', '0', '0', 'ai:app:edit', '#', 1, CURRENT_TIMESTAMP, ''),
(2006, '应用删除', 2002, 4, '', '', 1, 0, 'F', '0', '0', 'ai:app:remove', '#', 1, CURRENT_TIMESTAMP, ''),
(2007, '应用导出', 2002, 5, '', '', 1, 0, 'F', '0', '0', 'ai:app:export', '#', 1, CURRENT_TIMESTAMP, ''),
(2008, '工作流查询', 2002, 6, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:list', '#', 1, CURRENT_TIMESTAMP, ''),
(2009, '工作流保存', 2002, 7, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:add,ai:workflow:edit', '#', 1, CURRENT_TIMESTAMP, ''),
(2010, '工作流编排', 2000, 10, 'workflow', 'ai/workflow/index', 1, 0, 'C', '1', '0', 'ai:app:workflow', '#', 1, CURRENT_TIMESTAMP, '工作流编排页面（隐藏）'),
(2011, 'AI对话', 2000, 3, 'chat', 'ai/chat/index', 1, 0, 'C', '1', '0', 'ai:chat:view', 'chat', 1, CURRENT_TIMESTAMP, 'AI聊天对话页面'),
(2012, '发送消息', 2011, 1, '', '', 1, 0, 'F', '0', '0', 'ai:chat:send', '#', 1, CURRENT_TIMESTAMP, ''),
(2013, '查看历史', 2011, 2, '', '', 1, 0, 'F', '0', '0', 'ai:chat:history', '#', 1, CURRENT_TIMESTAMP, ''),
(2014, '清空对话', 2011, 3, '', '', 1, 0, 'F', '0', '0', 'ai:chat:clear', '#', 1, CURRENT_TIMESTAMP, ''),
(2015, '节点定义', 2000, 3, 'node-definition', 'ai/node-definition/index', 1, 1, 'C', '0', '0', 'ai:nodeDefinition:list', 'mdi:menu', 1, CURRENT_TIMESTAMP, ''),
(2016, '数据源管理', 2000, 4, 'datasource-manager', 'ai/datasource-manager/index', 1, 1, 'C', '0', '0', 'ai:datasourceManager:list', 'mdi:menu', 1, CURRENT_TIMESTAMP, ''),
(2017, 'APP详情', 2000, 10, 'app-detail', 'ai/app-detail/index', '', 1, 1, 'C', '1', '0', 'ai:appDetail:view', 'mdi:menu', 1, NOW());

INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id <= 2016;
