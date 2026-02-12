-- ----------------------------
-- 为 km_app 表添加"启用执行详情"字段
-- @author Mahone
-- @date 2026-01-25
-- ----------------------------

ALTER TABLE km_app ADD COLUMN enable_execution_detail CHAR(1) DEFAULT '0' COMMENT '是否启用执行详情（0禁用 1启用）';

-- 执行详情查看权限（按钮级别）
-- 需要先查询 AI应用管理菜单的 menu_id，假设通过 perms='ai:app:list' 查找
-- INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
-- SELECT FLOOR(RAND() * 1000000000), '查看执行详情', menu_id, 10, '', '', 1, 0, 'F', '0', '0', 'ai:app:execution-detail', '', 1, NOW()
-- FROM sys_menu WHERE perms = 'ai:app:list' LIMIT 1;
