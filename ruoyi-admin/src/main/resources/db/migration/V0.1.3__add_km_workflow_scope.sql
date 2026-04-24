-- V0.1.3: 新增 km_workflow_scope 表，为 AgenticScopeStore 持久化预留
-- 用于 LangGraph4j 的 Checkpointing 机制，支持复杂 Agent 工作流的暂停与恢复

CREATE TABLE IF NOT EXISTS km_workflow_scope (
    id                 int8             not null,
    instance_id        int8             not null,
    node_id            varchar(100)     not null,
    scope_data         text             not null,
    -- 建议使用 timestamptz
    create_time        timestamptz      not null default CURRENT_TIMESTAMP,
    create_by          varchar(64)      not null default '',
    update_time        timestamptz      default CURRENT_TIMESTAMP,
    update_by          varchar(64)      not null default '',
    constraint pk_km_workflow_scope primary key (id)
    );

-- 注释部分保持不变
COMMENT ON TABLE km_workflow_scope IS '工作流执行域状态表 (用于 Checkpointing)';
COMMENT ON COLUMN km_workflow_scope.id IS '主键';
COMMENT ON COLUMN km_workflow_scope.instance_id IS '工作流实例ID';
COMMENT ON COLUMN km_workflow_scope.node_id IS '节点ID';
COMMENT ON COLUMN km_workflow_scope.scope_data IS '状态域数据(JSON序列化)';
COMMENT ON COLUMN km_workflow_scope.create_time IS '创建时间';
COMMENT ON COLUMN km_workflow_scope.create_by IS '创建者';
COMMENT ON COLUMN km_workflow_scope.update_time IS '更新时间';
COMMENT ON COLUMN km_workflow_scope.update_by IS '更新者';

-- 索引
CREATE INDEX IF NOT EXISTS idx_workflow_scope_instance_id ON km_workflow_scope(instance_id);