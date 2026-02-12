-- ----------------------------
-- 数据库查询节点相关表结构和初始化数据
-- 执行顺序: 1.建表 -> 2.节点定义 -> 3.连接规则
-- ----------------------------
-- ========================================
-- 1. 数据源配置表
-- ========================================
DROP TABLE IF EXISTS km_data_source;
CREATE TABLE km_data_source (
    -- 主键
    data_source_id      BIGINT(20)      NOT NULL        COMMENT '数据源ID',
    
    -- 基本信息
    data_source_name    VARCHAR(200)    NOT NULL        COMMENT '数据源名称',
    source_type         VARCHAR(20)     NOT NULL        COMMENT '数据源类型 (DYNAMIC/MANUAL)',
    
    -- dynamic-datasource 配置
    ds_key              VARCHAR(100)                    COMMENT 'dynamic-datasource数据源标识',
    
    -- JDBC 连接配置
    driver_class_name   VARCHAR(200)                    COMMENT 'JDBC驱动类',
    jdbc_url            VARCHAR(500)                    COMMENT 'JDBC连接URL',
    username            VARCHAR(100)                    COMMENT '用户名',
    password            VARCHAR(500)                    COMMENT '密码(加密存储)',
    
    -- 其他信息
    db_type             VARCHAR(50)                     COMMENT '数据库类型 (mysql/postgresql/oracle)',
    is_enabled          CHAR(1)         DEFAULT '1'     COMMENT '是否启用 (0停用/1启用)',
    
    -- BaseEntity审计字段
    create_dept         BIGINT(20)                      COMMENT '创建部门',
    create_by           BIGINT(20)                      COMMENT '创建者',
    create_time         DATETIME                        COMMENT '创建时间',
    update_by           BIGINT(20)                      COMMENT '更新者',
    update_time         DATETIME                        COMMENT '更新时间',
    remark              VARCHAR(500)                    COMMENT '备注',
    
    PRIMARY KEY (data_source_id),
    UNIQUE KEY uk_data_source_name (data_source_name),
    KEY idx_source_type (source_type)
) ENGINE=InnoDB COMMENT='数据源配置表';
-- ========================================
-- 2. 数据库元数据表
-- ========================================
DROP TABLE IF EXISTS km_database_meta;
CREATE TABLE km_database_meta (
    -- 主键
    meta_id             BIGINT(20)      NOT NULL        COMMENT '元数据ID',
    
    -- 关联数据源
    data_source_id      BIGINT(20)      NOT NULL        COMMENT '关联数据源ID',
    
    -- 元数据信息
    meta_source_type    VARCHAR(20)     NOT NULL        COMMENT '元数据来源类型 (DDL/JDBC)',
    ddl_content         TEXT                            COMMENT '建表SQL原文',
    table_name          VARCHAR(200)    NOT NULL        COMMENT '表名',
    table_comment       VARCHAR(500)                    COMMENT '表注释',
    columns             JSON                            COMMENT '列信息 (JSON Array)',
    
    -- BaseEntity审计字段
    create_dept         BIGINT(20)                      COMMENT '创建部门',
    create_by           BIGINT(20)                      COMMENT '创建者',
    create_time         DATETIME                        COMMENT '创建时间',
    update_by           BIGINT(20)                      COMMENT '更新者',
    update_time         DATETIME                        COMMENT '更新时间',
    remark              VARCHAR(500)                    COMMENT '备注',
    
    PRIMARY KEY (meta_id),
    UNIQUE KEY uk_ds_table (data_source_id, table_name),
    KEY idx_data_source_id (data_source_id)
) ENGINE=InnoDB COMMENT='数据库元数据表';
-- ========================================
-- 3. 插入 DB_QUERY 节点定义
-- ========================================
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params,
    input_params, output_params, version, create_time
) VALUES (
    8,
    'DB_QUERY', 
    '数据库查询', 
    'mdi:database-search', 
    '#06b6d4',
    'ai', 
    '结合LLM智能分析用户问题，生成SQL查询并返回自然语言回答', 
    '0', 
    '1',
    '0',
    '0',
    '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"description":"用户提出的业务问题"}]',
    '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"description":"LLM生成的SQL语句"},{"key":"queryResult","label":"查询结果","type":"object","required":true,"description":"SQL执行结果(JSON)"},{"key":"response","label":"AI回复","type":"string","required":true,"description":"基于查询结果生成的自然语言回答"}]',
    1,
    NOW()
);
-- ========================================
-- 4. 插入 DB_QUERY 节点连接规则
-- ========================================
-- DB_QUERY 可以从以下节点连入
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(17, 'START', 'DB_QUERY', '0', 10, NOW()),
(18, 'LLM_CHAT', 'DB_QUERY', '0', 10, NOW()),
(19, 'CONDITION', 'DB_QUERY', '0', 10, NOW()),
(20, 'INTENT_CLASSIFIER', 'DB_QUERY', '0', 10, NOW());
-- DB_QUERY 可以连接到以下节点INTENT_CLASSIFIER
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES 
(21, 'DB_QUERY', 'END', '0', 10, NOW()),
(22, 'DB_QUERY', 'LLM_CHAT', '0', 10, NOW()),
(23, 'DB_QUERY', 'CONDITION', '0', 10, NOW()),
(24, 'DB_QUERY', 'INTENT_CLASSIFIER', '0', 10, NOW()),
(25, 'DB_QUERY', 'FIXED_RESPONSE', '0', 10, NOW());