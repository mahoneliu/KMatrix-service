-- V0.1.8__add_mcp_registry_integration.sql
-- MCP 注册源集成功能：
--   1. 新增 km_mcp_registry_source 表（注册源配置）
--   2. 新增 km_mcp_registry_entry 表（注册源条目缓存）
--   3. 为 km_mcp_server 表新增导入来源追踪字段
--   4. 插入两条初始注册源数据（官方 Registry 和 Smithery）

-- ============================================
-- 第一部分：DDL 语句（表结构定义）
-- ============================================

-- ======================================================================
-- 表: km_mcp_registry_source（注册源配置）
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_mcp_registry_source (
    source_id        BIGINT          NOT NULL,
    source_name      VARCHAR(64)     NOT NULL,
    source_type      VARCHAR(20)     NOT NULL,                   -- 'official' | 'community'
    platform         VARCHAR(32)     NOT NULL,                   -- 'official' | 'smithery'
    api_base_url     VARCHAR(256)    NOT NULL,
    api_key          VARCHAR(256)    DEFAULT NULL,               -- 加密存储
    sync_interval    INT             NOT NULL DEFAULT 3600,      -- 同步间隔（秒），最小 3600，最大 604800
    is_enabled       CHAR(1)         NOT NULL DEFAULT '1',
    last_sync_time   TIMESTAMP       DEFAULT NULL,
    last_sync_count  INT             DEFAULT 0,
    last_sync_status VARCHAR(20)     DEFAULT NULL,               -- 'success' | 'failed' | 'running'
    last_sync_error  TEXT            DEFAULT NULL,
    create_dept      BIGINT          DEFAULT NULL,
    create_by        BIGINT          DEFAULT NULL,
    create_time      TIMESTAMP       DEFAULT NULL,
    update_by        BIGINT          DEFAULT NULL,
    update_time      TIMESTAMP       DEFAULT NULL,
    del_flag         CHAR(1)         DEFAULT '0',
    remark           VARCHAR(500)    DEFAULT NULL,
    CONSTRAINT pk_km_mcp_registry_source PRIMARY KEY (source_id)
);

COMMENT ON TABLE km_mcp_registry_source IS 'MCP 注册源配置表';
COMMENT ON COLUMN km_mcp_registry_source.source_id IS '注册源 ID';
COMMENT ON COLUMN km_mcp_registry_source.source_name IS '注册源名称';
COMMENT ON COLUMN km_mcp_registry_source.source_type IS '注册源类型（official=官方，community=社区）';
COMMENT ON COLUMN km_mcp_registry_source.platform IS '平台标识（official=官方注册源，smithery=Smithery 社区市场）';
COMMENT ON COLUMN km_mcp_registry_source.api_base_url IS '注册源 API 基础 URL';
COMMENT ON COLUMN km_mcp_registry_source.api_key IS 'API 密钥（加密存储，部分平台需要）';
COMMENT ON COLUMN km_mcp_registry_source.sync_interval IS '同步间隔（秒），最小 3600（1小时），最大 604800（7天）';
COMMENT ON COLUMN km_mcp_registry_source.is_enabled IS '是否启用（1=启用，0=禁用）';
COMMENT ON COLUMN km_mcp_registry_source.last_sync_time IS '最后同步时间';
COMMENT ON COLUMN km_mcp_registry_source.last_sync_count IS '最后同步条目数量';
COMMENT ON COLUMN km_mcp_registry_source.last_sync_status IS '最后同步状态（success=成功，failed=失败，running=同步中）';
COMMENT ON COLUMN km_mcp_registry_source.last_sync_error IS '最后同步错误信息';
COMMENT ON COLUMN km_mcp_registry_source.create_dept IS '创建部门';
COMMENT ON COLUMN km_mcp_registry_source.create_by IS '创建者';
COMMENT ON COLUMN km_mcp_registry_source.create_time IS '创建时间';
COMMENT ON COLUMN km_mcp_registry_source.update_by IS '更新者';
COMMENT ON COLUMN km_mcp_registry_source.update_time IS '更新时间';
COMMENT ON COLUMN km_mcp_registry_source.del_flag IS '删除标志（0=未删除，2=已删除）';
COMMENT ON COLUMN km_mcp_registry_source.remark IS '备注';

-- ======================================================================
-- 表: km_mcp_registry_entry（注册源条目缓存）
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_mcp_registry_entry (
    entry_id         BIGINT          NOT NULL,
    source_id        BIGINT          NOT NULL,                   -- 关联 km_mcp_registry_source
    external_id      VARCHAR(256)    NOT NULL,                   -- 外部平台的唯一标识
    entry_name       VARCHAR(128)    NOT NULL,
    display_name     VARCHAR(128)    DEFAULT NULL,
    description      TEXT            DEFAULT NULL,
    author           VARCHAR(128)    DEFAULT NULL,
    version          VARCHAR(64)     DEFAULT NULL,
    transport_type   VARCHAR(20)     DEFAULT NULL,               -- 'sse' | 'stdio' | 'streamable_http'
    endpoint_url     VARCHAR(512)    DEFAULT NULL,               -- SSE/HTTP 端点
    command          VARCHAR(512)    DEFAULT NULL,               -- Stdio 命令
    args             JSONB           DEFAULT NULL,               -- Stdio 参数列表
    env_vars         JSONB           DEFAULT NULL,               -- 环境变量
    packages         JSONB           DEFAULT NULL,               -- 包信息（npm/docker 等）
    dns_verified     BOOLEAN         DEFAULT FALSE,
    source_platform  VARCHAR(32)     NOT NULL,                   -- 'official' | 'smithery'
    entry_status     VARCHAR(20)     DEFAULT 'active',           -- 'active' | 'deprecated' | 'deleted' | 'offline'
    rating           NUMERIC(3,1)    DEFAULT NULL,               -- 评分 0.0~5.0
    use_count        INT             DEFAULT NULL,
    tags             JSONB           DEFAULT NULL,               -- 分类标签列表
    icon_url         VARCHAR(512)    DEFAULT NULL,
    homepage_url     VARCHAR(512)    DEFAULT NULL,
    create_time      TIMESTAMP       DEFAULT NULL,
    update_time      TIMESTAMP       DEFAULT NULL,
    CONSTRAINT pk_km_mcp_registry_entry PRIMARY KEY (entry_id),
    CONSTRAINT uk_registry_entry UNIQUE (source_id, external_id)
);

CREATE INDEX IF NOT EXISTS idx_registry_entry_source ON km_mcp_registry_entry(source_id);
CREATE INDEX IF NOT EXISTS idx_registry_entry_status ON km_mcp_registry_entry(entry_status);
CREATE INDEX IF NOT EXISTS idx_registry_entry_platform ON km_mcp_registry_entry(source_platform);
CREATE INDEX IF NOT EXISTS idx_registry_entry_fts ON km_mcp_registry_entry
    USING GIN (to_tsvector('simple', coalesce(entry_name,'') || ' ' || coalesce(display_name,'') || ' ' || coalesce(description,'')));

COMMENT ON TABLE km_mcp_registry_entry IS 'MCP 注册源条目缓存表';
COMMENT ON COLUMN km_mcp_registry_entry.entry_id IS '条目 ID';
COMMENT ON COLUMN km_mcp_registry_entry.source_id IS '所属注册源 ID，关联 km_mcp_registry_source';
COMMENT ON COLUMN km_mcp_registry_entry.external_id IS '外部平台的唯一标识符';
COMMENT ON COLUMN km_mcp_registry_entry.entry_name IS '条目名称（英文标识）';
COMMENT ON COLUMN km_mcp_registry_entry.display_name IS '显示名称';
COMMENT ON COLUMN km_mcp_registry_entry.description IS '描述';
COMMENT ON COLUMN km_mcp_registry_entry.author IS '作者';
COMMENT ON COLUMN km_mcp_registry_entry.version IS '版本号';
COMMENT ON COLUMN km_mcp_registry_entry.transport_type IS '传输类型（sse=SSE，stdio=标准输入输出，streamable_http=流式 HTTP）';
COMMENT ON COLUMN km_mcp_registry_entry.endpoint_url IS 'SSE/HTTP 端点 URL';
COMMENT ON COLUMN km_mcp_registry_entry.command IS 'Stdio 启动命令';
COMMENT ON COLUMN km_mcp_registry_entry.args IS 'Stdio 启动参数列表（JSON 数组）';
COMMENT ON COLUMN km_mcp_registry_entry.env_vars IS '环境变量（JSON 对象）';
COMMENT ON COLUMN km_mcp_registry_entry.packages IS '包信息，如 npm/docker 等（JSON）';
COMMENT ON COLUMN km_mcp_registry_entry.dns_verified IS '是否经过 DNS 验证';
COMMENT ON COLUMN km_mcp_registry_entry.source_platform IS '来源平台（official=官方注册源，smithery=Smithery）';
COMMENT ON COLUMN km_mcp_registry_entry.entry_status IS '条目状态（active=活跃，deprecated=已废弃，deleted=已删除，offline=已下线）';
COMMENT ON COLUMN km_mcp_registry_entry.rating IS '社区评分（0.0~5.0）';
COMMENT ON COLUMN km_mcp_registry_entry.use_count IS '使用次数';
COMMENT ON COLUMN km_mcp_registry_entry.tags IS '分类标签列表（JSON 数组）';
COMMENT ON COLUMN km_mcp_registry_entry.icon_url IS '图标 URL';
COMMENT ON COLUMN km_mcp_registry_entry.homepage_url IS '主页 URL';
COMMENT ON COLUMN km_mcp_registry_entry.create_time IS '条目在外部平台的创建时间';
COMMENT ON COLUMN km_mcp_registry_entry.update_time IS '条目在外部平台的最后更新时间';

-- ======================================================================
-- 表: km_mcp_server（新增导入来源追踪字段）
-- ======================================================================
ALTER TABLE km_mcp_server
    ADD COLUMN IF NOT EXISTS source_registry_id BIGINT      DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS source_entry_id    BIGINT      DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS import_source      VARCHAR(20) DEFAULT 'manual';  -- 'manual' | 'registry'

COMMENT ON COLUMN km_mcp_server.source_registry_id IS '导入来源注册源 ID，关联 km_mcp_registry_source（手工添加时为 NULL）';
COMMENT ON COLUMN km_mcp_server.source_entry_id IS '导入来源条目 ID，关联 km_mcp_registry_entry（手工添加时为 NULL）';
COMMENT ON COLUMN km_mcp_server.import_source IS '导入方式（manual=手工添加，registry=从注册源导入）';

-- ============================================
-- 第二部分：DML 语句（初始数据）
-- ============================================

-- km_mcp_registry_source 初始注册源数据
INSERT INTO km_mcp_registry_source (source_id, source_name, source_type, platform, api_base_url, sync_interval, is_enabled, create_time, update_time, del_flag) VALUES
(1, 'MCP 官方注册源', 'official', 'official', 'https://registry.modelcontextprotocol.io', 86400, '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '0'),
(2, 'Smithery 社区市场', 'community', 'smithery', 'https://registry.smithery.ai', 86400, '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '0')
ON CONFLICT (source_id) DO NOTHING;

-- ============================================
-- 第三部分：菜单与权限数据
-- ============================================

-- V0.1.8: 新增菜单数据：MCP 注册源集成
-- Parent ID 2100 是 "MCP管理"（挂在 MCP 管理父菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2140, 'MCP注册源', 2100, 5, 'mcp-registry', 'execution/mcp-manager/registry/index', 1, 0, 'C', '0', '0', 'ai:mcpRegistry:list', 'mdi:store-search', 1, NOW(), 'MCP 注册源浏览与导入'),
(2141, '注册源查询', 2140, 1, '', '', 1, 0, 'F', '0', '0', 'ai:mcpRegistry:list', '#', 1, NOW(), ''),
(2142, '注册源配置', 2140, 2, '', '', 1, 0, 'F', '0', '0', 'ai:mcpRegistry:edit', '#', 1, NOW(), ''),
(2143, '注册源删除', 2140, 3, '', '', 1, 0, 'F', '0', '0', 'ai:mcpRegistry:remove', '#', 1, NOW(), ''),
(2144, '注册源同步', 2140, 4, '', '', 1, 0, 'F', '0', '0', 'ai:mcpRegistry:sync', '#', 1, NOW(), '')
ON CONFLICT (menu_id) DO NOTHING;

-- 为超级管理员关联菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2140, 2141, 2142, 2143, 2144)
ON CONFLICT (role_id, menu_id) DO NOTHING;
