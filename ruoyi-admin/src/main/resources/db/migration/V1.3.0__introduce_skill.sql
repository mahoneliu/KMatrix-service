-- V1.3.0: 引入 Skill (技能) 支持
-- 新增 km_skill 表：管理技能相关的定义与工具绑定

-- ----------------------------
-- km_skill: 技能配置管理
-- ----------------------------
CREATE TABLE IF NOT EXISTS km_skill (
    skill_id        BIGINT          NOT NULL,
    skill_name      VARCHAR(64)     NOT NULL,
    spec            VARCHAR(500)    DEFAULT '',
    tool_bindings   JSONB           DEFAULT NULL,
    input_schema    JSONB           DEFAULT NULL,
    output_schema   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (skill_id)
);

COMMENT ON TABLE km_skill IS '技能配置管理';
COMMENT ON COLUMN km_skill.skill_id IS '技能ID';
COMMENT ON COLUMN km_skill.skill_name IS '技能名称（英文标识，作为 LLM 函数名）';
COMMENT ON COLUMN km_skill.spec IS '技能说明（提供给大模型参考用）';
COMMENT ON COLUMN km_skill.tool_bindings IS '绑定的工具配置集合 JSON Array [{type:"builtin",id:1}, ...]';
COMMENT ON COLUMN km_skill.input_schema IS '技能入参 JSON Schema';
COMMENT ON COLUMN km_skill.output_schema IS '技能出参 JSON Schema';
COMMENT ON COLUMN km_skill.status IS '状态（0正常 1停用）';

-- ----------------------------
-- 新增加技能节点至工作流
-- ----------------------------
INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    13, 'SKILL', '技能节点', 'mdi-brain', '#ef4444', 
    'action', '统一执行由多个工具编排而成的技能', '0', '1', 
    '1', '1', 
    '[]', 
    '[]', 
    1, NOW(), NOW()
);

-- SKILL 节点的连接规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
(120, 'START', 'SKILL', '0', 10, '1', NOW()),
(121, 'LLM_CHAT', 'SKILL', '0', 10, '1', NOW()),
(122, 'INTENT_CLASSIFIER', 'SKILL', '0', 10, '1', NOW()),
(123, 'CONDITION', 'SKILL', '0', 10, '1', NOW()),
(124, 'DB_QUERY', 'SKILL', '0', 10, '1', NOW()),
(125, 'SQL_GENERATE', 'SKILL', '0', 10, '1', NOW()),
(126, 'SQL_EXECUTE', 'SKILL', '0', 10, '1', NOW()),
(127, 'KNOWLEDGE_RETRIEVAL', 'SKILL', '0', 10, '1', NOW()),
(128, 'TOOL', 'SKILL', '0', 10, '1', NOW()),
(129, 'SKILL', 'SKILL', '0', 10, '1', NOW()),
(130, 'SKILL', 'TOOL', '0', 10, '1', NOW()),
(131, 'SKILL', 'LLM_CHAT', '0', 10, '1', NOW()),
(132, 'SKILL', 'INTENT_CLASSIFIER', '0', 10, '1', NOW()),
(133, 'SKILL', 'CONDITION', '0', 10, '1', NOW()),
(134, 'SKILL', 'DB_QUERY', '0', 10, '1', NOW()),
(135, 'SKILL', 'SQL_GENERATE', '0', 10, '1', NOW()),
(136, 'SKILL', 'SQL_EXECUTE', '0', 10, '1', NOW()),
(137, 'SKILL', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', NOW()),
(138, 'SKILL', 'FIXED_RESPONSE', '0', 10, '1', NOW()),
(139, 'SKILL', 'END', '0', 10, '1', NOW());

-- ----------------------------
-- 新增菜单数据：技能管理
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2120, '技能管理', 2000, 7, 'skill-manager', 'ai/skill-manager/index', 1, 0, 'C', '0', '0', 'ai:skill:list', 'mdi:brain', 1, NOW(), '技能抽象管理'),
(2121, '技能查询', 2120, 1, '', '', 1, 0, 'F', '0', '0', 'ai:skill:query', '#', 1, NOW(), ''),
(2122, '技能新增', 2120, 2, '', '', 1, 0, 'F', '0', '0', 'ai:skill:add', '#', 1, NOW(), ''),
(2123, '技能修改', 2120, 3, '', '', 1, 0, 'F', '0', '0', 'ai:skill:edit', '#', 1, NOW(), ''),
(2124, '技能删除', 2120, 4, '', '', 1, 0, 'F', '0', '0', 'ai:skill:remove', '#', 1, NOW(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2120, 2121, 2122, 2123, 2124);

-- ----------------------------
-- 初始化一个样例技能：日期格式化 (调用内置日期格式化工具)
-- ----------------------------
INSERT INTO km_skill (
    skill_id, skill_name, spec, tool_bindings, input_schema, output_schema, status, create_time, update_time
) VALUES (
    1, 
    'date_formatter_skill', 
    '调用内置日期格式化工具', 
    '[{"type":"builtin", "id":1}]', 
    '{"type": "object", "properties": {"date": {"type": "string", "description": "ISO 格式日期 (如: 2024-03-20)，不传则用当前日期"}}}', 
    '{"type": "object", "properties": {"formatted_date": {"type": "string", "description": "格式化后的中文日期"}}}', 
    '0', 
    NOW(), 
    NOW()
) ON CONFLICT (skill_id) DO UPDATE SET spec = EXCLUDED.spec;
