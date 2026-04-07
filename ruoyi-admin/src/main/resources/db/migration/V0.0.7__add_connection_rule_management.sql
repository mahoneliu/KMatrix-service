-- V0.0.7__add_connection_rule_management.sql
-- 节点连接规则管理功能：
--   1. 补充白名单模式下遗漏的允许规则（基于数据库实际数据对比分析）
--   2. 新增 sys_config 参数：workflow.connection.mode（默认 whitelist）
--   3. 新增黑名单模式下的禁止规则（rule_type=1）供切换后使用
--   4. 新增菜单与权限按钮
--
-- 实际节点类型（共18种）：
--   基础: START, END
--   AI:   LLM_CHAT, INTENT_CLASSIFIER, DB_QUERY, SQL_GENERATE, SQL_EXECUTE,
--         KNOWLEDGE_RETRIEVAL, AUDIO_ASR, IMAGE_OCR
--   逻辑: CONDITION, LOOP
--   动作: FIXED_RESPONSE, FILE_STORAGE, TOOL, SKILL
--   文件: FILE_PARSE, DATASET_STORAGE

-- ============================================================
-- 第一部分：补充遗漏的白名单规则（rule_type=0，允许连接）
-- ============================================================
-- 对比数据库实际数据，以下连接在业务上合理但尚未录入。
-- 通用中间节点（TOOL/SKILL/LOOP/CONDITION/INTENT_CLASSIFIER/LLM_CHAT）
-- 应能连入多模态和文件处理节点，保持与其他节点的对称性。

INSERT INTO km_node_connection_rule
    (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time)
VALUES

-- ── FILE_STORAGE 补充连入（目前只有 START->FILE_STORAGE） ──
(400, 'CONDITION',         'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(401, 'INTENT_CLASSIFIER', 'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(402, 'LLM_CHAT',          'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(403, 'TOOL',              'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(404, 'SKILL',             'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(405, 'LOOP',              'FILE_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),

-- ── AUDIO_ASR 补充连入（目前只有 START/FILE_STORAGE->AUDIO_ASR） ──
(410, 'CONDITION',         'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),
(411, 'INTENT_CLASSIFIER', 'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),
(412, 'LLM_CHAT',          'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),
(413, 'TOOL',              'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),
(414, 'SKILL',             'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),
(415, 'LOOP',              'AUDIO_ASR', '0', 10, '1', CURRENT_TIMESTAMP),

-- ── IMAGE_OCR 补充连入（目前只有 START/FILE_STORAGE->IMAGE_OCR） ──
(420, 'CONDITION',         'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),
(421, 'INTENT_CLASSIFIER', 'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),
(422, 'LLM_CHAT',          'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),
(423, 'TOOL',              'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),
(424, 'SKILL',             'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),
(425, 'LOOP',              'IMAGE_OCR', '0', 10, '1', CURRENT_TIMESTAMP),

-- ── AUDIO_ASR 补充连出（目前只有 AUDIO_ASR->{LLM_CHAT,CONDITION,END}） ──
(430, 'AUDIO_ASR', 'FIXED_RESPONSE',    '0', 10, '1', CURRENT_TIMESTAMP),
(431, 'AUDIO_ASR', 'INTENT_CLASSIFIER', '0', 10, '1', CURRENT_TIMESTAMP),
(432, 'AUDIO_ASR', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP),
(433, 'AUDIO_ASR', 'TOOL',              '0', 10, '1', CURRENT_TIMESTAMP),
(434, 'AUDIO_ASR', 'SKILL',             '0', 10, '1', CURRENT_TIMESTAMP),
(435, 'AUDIO_ASR', 'LOOP',              '0', 10, '1', CURRENT_TIMESTAMP),
(436, 'AUDIO_ASR', 'SQL_GENERATE',      '0', 10, '1', CURRENT_TIMESTAMP),
(437, 'AUDIO_ASR', 'DB_QUERY',          '0', 10, '1', CURRENT_TIMESTAMP),

-- ── IMAGE_OCR 补充连出（目前只有 IMAGE_OCR->{LLM_CHAT,CONDITION,END}） ──
(440, 'IMAGE_OCR', 'FIXED_RESPONSE',    '0', 10, '1', CURRENT_TIMESTAMP),
(441, 'IMAGE_OCR', 'INTENT_CLASSIFIER', '0', 10, '1', CURRENT_TIMESTAMP),
(442, 'IMAGE_OCR', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP),
(443, 'IMAGE_OCR', 'TOOL',              '0', 10, '1', CURRENT_TIMESTAMP),
(444, 'IMAGE_OCR', 'SKILL',             '0', 10, '1', CURRENT_TIMESTAMP),
(445, 'IMAGE_OCR', 'LOOP',              '0', 10, '1', CURRENT_TIMESTAMP),
(446, 'IMAGE_OCR', 'SQL_GENERATE',      '0', 10, '1', CURRENT_TIMESTAMP),
(447, 'IMAGE_OCR', 'DB_QUERY',          '0', 10, '1', CURRENT_TIMESTAMP),

-- ── FILE_STORAGE 补充连出（目前只有 FILE_STORAGE->{LLM_CHAT,AUDIO_ASR,IMAGE_OCR,CONDITION,END}） ──
(450, 'FILE_STORAGE', 'FILE_PARSE',          '0', 10, '1', CURRENT_TIMESTAMP),
(451, 'FILE_STORAGE', 'INTENT_CLASSIFIER',   '0', 10, '1', CURRENT_TIMESTAMP),
(452, 'FILE_STORAGE', 'FIXED_RESPONSE',      '0', 10, '1', CURRENT_TIMESTAMP),
(453, 'FILE_STORAGE', 'KNOWLEDGE_RETRIEVAL', '0', 10, '1', CURRENT_TIMESTAMP),
(454, 'FILE_STORAGE', 'TOOL',                '0', 10, '1', CURRENT_TIMESTAMP),
(455, 'FILE_STORAGE', 'SKILL',               '0', 10, '1', CURRENT_TIMESTAMP),
(456, 'FILE_STORAGE', 'LOOP',                '0', 10, '1', CURRENT_TIMESTAMP),

-- ── FILE_PARSE 补充连入（目前只有 START->FILE_PARSE） ──
(460, 'FILE_STORAGE', 'FILE_PARSE', '0', 10, '1', CURRENT_TIMESTAMP)  -- 文件存储后直接解析是典型流程

ON CONFLICT (source_node_type, target_node_type) DO NOTHING;

-- ============================================================
-- 第二部分：新增系统参数 workflow.connection.mode
-- ============================================================
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_time, remark)
VALUES (
    2100,
    '工作流节点连接模式',
    'workflow.connection.mode',
    'whitelist',
    'Y',
    CURRENT_TIMESTAMP,
    '工作流节点连接校验模式：whitelist=白名单（仅允许 rule_type=0 的连接），blacklist=黑名单（仅禁止 rule_type=1 的连接）'
)
ON CONFLICT (config_id) DO NOTHING;

-- ============================================================
-- 第三部分：黑名单禁止规则（rule_type=1）
-- ============================================================
-- 推导原则：
--   1. END 不能作为源节点（终止节点，无出边）
--   2. START 不能作为目标节点（入口节点，无入边）
--   3. DATASET_STORAGE 只能连 END（文件处理专用终止节点）
--   4. 其余节点在黑名单模式下默认互通，不预设额外禁止规则，
--      由管理员按业务需要通过管理界面补充

-- END 作为源节点（END 无出边）
INSERT INTO km_node_connection_rule
    (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time)
VALUES
(500, 'END', 'START',               '1', 10, '1', CURRENT_TIMESTAMP),
(501, 'END', 'LLM_CHAT',            '1', 10, '1', CURRENT_TIMESTAMP),
(502, 'END', 'INTENT_CLASSIFIER',   '1', 10, '1', CURRENT_TIMESTAMP),
(503, 'END', 'CONDITION',           '1', 10, '1', CURRENT_TIMESTAMP),
(504, 'END', 'FIXED_RESPONSE',      '1', 10, '1', CURRENT_TIMESTAMP),
(505, 'END', 'DB_QUERY',            '1', 10, '1', CURRENT_TIMESTAMP),
(506, 'END', 'SQL_GENERATE',        '1', 10, '1', CURRENT_TIMESTAMP),
(507, 'END', 'SQL_EXECUTE',         '1', 10, '1', CURRENT_TIMESTAMP),
(508, 'END', 'KNOWLEDGE_RETRIEVAL', '1', 10, '1', CURRENT_TIMESTAMP),
(509, 'END', 'AUDIO_ASR',           '1', 10, '1', CURRENT_TIMESTAMP),
(510, 'END', 'IMAGE_OCR',           '1', 10, '1', CURRENT_TIMESTAMP),
(511, 'END', 'FILE_STORAGE',        '1', 10, '1', CURRENT_TIMESTAMP),
(512, 'END', 'FILE_PARSE',          '1', 10, '1', CURRENT_TIMESTAMP),
(513, 'END', 'DATASET_STORAGE',     '1', 10, '1', CURRENT_TIMESTAMP),
(514, 'END', 'TOOL',                '1', 10, '1', CURRENT_TIMESTAMP),
(515, 'END', 'SKILL',               '1', 10, '1', CURRENT_TIMESTAMP),
(516, 'END', 'LOOP',                '1', 10, '1', CURRENT_TIMESTAMP),
(517, 'END', 'END',                 '1', 10, '1', CURRENT_TIMESTAMP),

-- START 作为目标节点（START 无入边）
(520, 'LLM_CHAT',            'START', '1', 10, '1', CURRENT_TIMESTAMP),
(521, 'INTENT_CLASSIFIER',   'START', '1', 10, '1', CURRENT_TIMESTAMP),
(522, 'CONDITION',           'START', '1', 10, '1', CURRENT_TIMESTAMP),
(523, 'FIXED_RESPONSE',      'START', '1', 10, '1', CURRENT_TIMESTAMP),
(524, 'DB_QUERY',            'START', '1', 10, '1', CURRENT_TIMESTAMP),
(525, 'SQL_GENERATE',        'START', '1', 10, '1', CURRENT_TIMESTAMP),
(526, 'SQL_EXECUTE',         'START', '1', 10, '1', CURRENT_TIMESTAMP),
(527, 'KNOWLEDGE_RETRIEVAL', 'START', '1', 10, '1', CURRENT_TIMESTAMP),
(528, 'AUDIO_ASR',           'START', '1', 10, '1', CURRENT_TIMESTAMP),
(529, 'IMAGE_OCR',           'START', '1', 10, '1', CURRENT_TIMESTAMP),
(530, 'FILE_STORAGE',        'START', '1', 10, '1', CURRENT_TIMESTAMP),
(531, 'FILE_PARSE',          'START', '1', 10, '1', CURRENT_TIMESTAMP),
(532, 'DATASET_STORAGE',     'START', '1', 10, '1', CURRENT_TIMESTAMP),
(533, 'TOOL',                'START', '1', 10, '1', CURRENT_TIMESTAMP),
(534, 'SKILL',               'START', '1', 10, '1', CURRENT_TIMESTAMP),
(535, 'LOOP',                'START', '1', 10, '1', CURRENT_TIMESTAMP),
(536, 'START',               'START', '1', 10, '1', CURRENT_TIMESTAMP),

-- DATASET_STORAGE 只能连 END
(540, 'DATASET_STORAGE', 'LLM_CHAT',            '1', 10, '1', CURRENT_TIMESTAMP),
(541, 'DATASET_STORAGE', 'INTENT_CLASSIFIER',   '1', 10, '1', CURRENT_TIMESTAMP),
(542, 'DATASET_STORAGE', 'CONDITION',           '1', 10, '1', CURRENT_TIMESTAMP),
(543, 'DATASET_STORAGE', 'FIXED_RESPONSE',      '1', 10, '1', CURRENT_TIMESTAMP),
(544, 'DATASET_STORAGE', 'DB_QUERY',            '1', 10, '1', CURRENT_TIMESTAMP),
(545, 'DATASET_STORAGE', 'SQL_GENERATE',        '1', 10, '1', CURRENT_TIMESTAMP),
(546, 'DATASET_STORAGE', 'SQL_EXECUTE',         '1', 10, '1', CURRENT_TIMESTAMP),
(547, 'DATASET_STORAGE', 'KNOWLEDGE_RETRIEVAL', '1', 10, '1', CURRENT_TIMESTAMP),
(548, 'DATASET_STORAGE', 'AUDIO_ASR',           '1', 10, '1', CURRENT_TIMESTAMP),
(549, 'DATASET_STORAGE', 'IMAGE_OCR',           '1', 10, '1', CURRENT_TIMESTAMP),
(550, 'DATASET_STORAGE', 'FILE_STORAGE',        '1', 10, '1', CURRENT_TIMESTAMP),
(551, 'DATASET_STORAGE', 'FILE_PARSE',          '1', 10, '1', CURRENT_TIMESTAMP),
(552, 'DATASET_STORAGE', 'TOOL',                '1', 10, '1', CURRENT_TIMESTAMP),
(553, 'DATASET_STORAGE', 'SKILL',               '1', 10, '1', CURRENT_TIMESTAMP),
(554, 'DATASET_STORAGE', 'LOOP',                '1', 10, '1', CURRENT_TIMESTAMP),
(555, 'DATASET_STORAGE', 'START',               '1', 10, '1', CURRENT_TIMESTAMP),
(556, 'DATASET_STORAGE', 'DATASET_STORAGE',     '1', 10, '1', CURRENT_TIMESTAMP)

ON CONFLICT (source_node_type, target_node_type) DO NOTHING;

-- ============================================================
-- 第四部分：菜单与权限按钮
-- ============================================================
-- 连接规则管理挂在"工作流"目录(2300)下，作为三级菜单
-- menu_id 从 2350 开始，避开 baseline 中 2311-2332 的已有按钮
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(2350, '连接规则管理', 2300, 5,
 'connection-rule-manager', 'ai/workflow/connection-rule-manager/index', '',
 '1', '0', 'C', '0', '0', 'ai:connectionRule:list',
 'mdi-link-variant', 103, 1, CURRENT_TIMESTAMP, null, null, '工作流节点连接规则管理'),
(2351, '规则查询', 2350, 1, '', '', '', '1', '0', 'F', '0', '0', 'ai:connectionRule:query',  '#', 103, 1, CURRENT_TIMESTAMP, null, null, ''),
(2352, '规则新增', 2350, 2, '', '', '', '1', '0', 'F', '0', '0', 'ai:connectionRule:add',    '#', 103, 1, CURRENT_TIMESTAMP, null, null, ''),
(2353, '规则修改', 2350, 3, '', '', '', '1', '0', 'F', '0', '0', 'ai:connectionRule:edit',   '#', 103, 1, CURRENT_TIMESTAMP, null, null, ''),
(2354, '规则删除', 2350, 4, '', '', '', '1', '0', 'F', '0', '0', 'ai:connectionRule:remove', '#', 103, 1, CURRENT_TIMESTAMP, null, null, ''),
(2355, '模式切换', 2350, 5, '', '', '', '1', '0', 'F', '0', '0', 'ai:connectionRule:config', '#', 103, 1, CURRENT_TIMESTAMP, null, null, '')

ON CONFLICT (menu_id) DO NOTHING;
