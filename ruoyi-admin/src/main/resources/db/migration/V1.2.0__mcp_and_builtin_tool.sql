-- V1.2.0: MCP Server 与 内置 Tool 支持
-- 新增 km_mcp_server 表：管理 MCP Server 配置
-- 新增 km_builtin_tool 表：管理自定义 Python 工具

-- ----------------------------
-- km_mcp_server: MCP Server 配置管理
-- ----------------------------
CREATE TABLE IF NOT EXISTS km_mcp_server (
    server_id       BIGINT          NOT NULL,
    server_name     VARCHAR(64)     NOT NULL,
    description     VARCHAR(128)    DEFAULT '',
    transport_type  VARCHAR(20)     NOT NULL DEFAULT 'sse',
    server_config   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (server_id)
);

COMMENT ON TABLE km_mcp_server IS 'MCP Server 配置管理';
COMMENT ON COLUMN km_mcp_server.server_id IS 'MCP Server ID';
COMMENT ON COLUMN km_mcp_server.server_name IS 'MCP Server 名称';
COMMENT ON COLUMN km_mcp_server.description IS '描述';
COMMENT ON COLUMN km_mcp_server.transport_type IS '传输类型（sse/streamable_http）';
COMMENT ON COLUMN km_mcp_server.server_config IS 'MCP Server 配置(JSON)，如 url, transport 等';
COMMENT ON COLUMN km_mcp_server.status IS '状态（0正常 1停用）';

-- ----------------------------
-- km_builtin_tool: 内置 Python 工具管理
-- ----------------------------
CREATE TABLE IF NOT EXISTS km_builtin_tool (
    tool_id         BIGINT          NOT NULL,
    tool_name       VARCHAR(64)     NOT NULL,
    spec            VARCHAR(128)    DEFAULT '',
    init_params     JSONB           DEFAULT NULL,
    input_schema    JSONB           DEFAULT NULL,
    output_schema   JSONB           DEFAULT NULL,
    python_code     TEXT            DEFAULT '',
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (tool_id)
);

COMMENT ON TABLE km_builtin_tool IS '内置 Python 工具管理';
COMMENT ON COLUMN km_builtin_tool.tool_id IS '工具 ID';
COMMENT ON COLUMN km_builtin_tool.tool_name IS '工具名称（英文标识，作为 LLM Tool function name）';
COMMENT ON COLUMN km_builtin_tool.spec IS '工具描述（提供给 LLM 的说明）';
COMMENT ON COLUMN km_builtin_tool.init_params IS '启动参数定义 schema (JSON Array)';
COMMENT ON COLUMN km_builtin_tool.input_schema IS '输入参数 JSON Schema（供 LLM 解析字段）';
COMMENT ON COLUMN km_builtin_tool.output_schema IS '输出参数 JSON Schema（供 LLM 解析字段）';
COMMENT ON COLUMN km_builtin_tool.python_code IS 'Python 脚本内容';
COMMENT ON COLUMN km_builtin_tool.status IS '状态（0正常 1停用）';


-- ----------------------------
-- 新增加工具节点至工作流
-- ----------------------------
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    12, 'TOOL', '工具节点', 'mdi-tools', '#0d9488', 
    'action', '执行系统内置工具或MCP服务集成工具', '0', '1', 
    '1', '1', 
    '[]', 
    '[]', 
    1, NOW(), NOW()
);

-- TOOL 节点的连接规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
(100, 'START', 'TOOL', '0', 10, '1', NOW()),
(101, 'LLM_CHAT', 'TOOL', '0', 10, '1', NOW()),
(102, 'INTENT_CLASSIFIER', 'TOOL', '0', 10, '1', NOW()),
(103, 'CONDITION', 'TOOL', '0', 10, '1', NOW()),
(104, 'DB_QUERY', 'TOOL', '0', 10, '1', NOW()),
(105, 'SQL_GENERATE', 'TOOL', '0', 10, '1', NOW()),
(106, 'SQL_EXECUTE', 'TOOL', '0', 10, '1', NOW()),
(107, 'KNOWLEDGE_RETRIEVAL', 'TOOL', '0', 10, '1', NOW()),
(108, 'TOOL', 'TOOL', '0', 10, '1', NOW()),
(109, 'TOOL', 'LLM_CHAT', '0', 10, '1', NOW()),
(110, 'TOOL', 'INTENT_CLASSIFIER', '0', 10, '1', NOW()),
(111, 'TOOL', 'CONDITION', '0', 10, '1', NOW()),
(112, 'TOOL', 'DB_QUERY', '0', 10, '1', NOW()),
(113, 'TOOL', 'SQL_GENERATE', '0', 10, '1', NOW()),
(114, 'TOOL', 'SQL_EXECUTE', '0', 10, '1', NOW()),
(115, 'TOOL', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', NOW()),
(116, 'TOOL', 'FIXED_RESPONSE', '0', 10, '1', NOW()),
(117, 'TOOL', 'END', '0', 10, '1', NOW());

-- ----------------------------
-- 新增菜单数据：MCP 管理 / 内置工具管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2100, 'MCP管理', 2000, 5, 'mcp-manager', 'ai/mcp-manager/index', 1, 0, 'C', '0', '0', 'ai:mcpServer:list', 'mdi:cloud-braces', 1, NOW(), 'MCP Server 管理'),
(2101, 'MCP查询', 2100, 1, '', '', 1, 0, 'F', '0', '0', 'ai:mcpServer:query', '#', 1, NOW(), ''),
(2102, 'MCP新增', 2100, 2, '', '', 1, 0, 'F', '0', '0', 'ai:mcpServer:add', '#', 1, NOW(), ''),
(2103, 'MCP修改', 2100, 3, '', '', 1, 0, 'F', '0', '0', 'ai:mcpServer:edit', '#', 1, NOW(), ''),
(2104, 'MCP删除', 2100, 4, '', '', 1, 0, 'F', '0', '0', 'ai:mcpServer:remove', '#', 1, NOW(), ''),
(2110, '工具管理', 2000, 6, 'tool-manager', 'ai/tool-manager/index', 1, 0, 'C', '0', '0', 'ai:builtinTool:list', 'mdi:hammer-wrench', 1, NOW(), '内置 Python 工具管理'),
(2111, '工具查询', 2110, 1, '', '', 1, 0, 'F', '0', '0', 'ai:builtinTool:query', '#', 1, NOW(), ''),
(2112, '工具新增', 2110, 2, '', '', 1, 0, 'F', '0', '0', 'ai:builtinTool:add', '#', 1, NOW(), ''),
(2113, '工具修改', 2110, 3, '', '', 1, 0, 'F', '0', '0', 'ai:builtinTool:edit', '#', 1, NOW(), ''),
(2114, '工具删除', 2110, 4, '', '', 1, 0, 'F', '0', '0', 'ai:builtinTool:remove', '#', 1, NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2100, 2101, 2102, 2103, 2104, 2110, 2111, 2112, 2113, 2114);

-- demo环境标志生效
update sys_config set config_value = 'true' where config_key = 'sys.demo.enabled';

-- ----------------------------
-- 初始化一个样例工具：日期格式化
-- ----------------------------
INSERT INTO km_builtin_tool (
    tool_id, tool_name, spec, init_params, input_schema, output_schema, python_code, status, create_time, update_time
) VALUES (
    1, 
    'date_formatter', 
    '将日期格式化为中文显示格式（例如：2024年03月20日）', 
    '[]', 
    '{"type": "object", "properties": {"date": {"type": "string", "description": "ISO 格式日期 (如: 2024-03-20)，不传则用当前日期"}}}', 
    '{"type": "object", "properties": {"formatted_date": {"type": "string", "description": "格式化后的中文日期"}}}', 
    E'import json\nimport sys\nfrom datetime import datetime\n\ndef main():\n    try:\n        if len(sys.argv) < 2:\n            input_data = {}\n        else:\n            with open(sys.argv[1], "r", encoding="utf-8") as f:\n                input_data = json.load(f)\n        \n        date_str = input_data.get("date")\n        if date_str:\n            dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))\n        else:\n            dt = datetime.now()\n            \n        formatted_date = dt.strftime("%Y年%m月%d日")\n        print(json.dumps({"formatted_date": formatted_date}, ensure_ascii=False))\n    except Exception as e:\n        print(json.dumps({"error": str(e)}, ensure_ascii=False))\n        sys.exit(1)\n\nif __name__ == "__main__":\n    main()', 
    '0', 
    NOW(), 
    NOW()
) ON CONFLICT (tool_id) DO UPDATE SET python_code = EXCLUDED.python_code;
