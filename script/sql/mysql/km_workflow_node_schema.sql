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
