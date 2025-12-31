-- ----------------------------
-- Fix 403 Error for Workflow Orchestration Page
-- ----------------------------

-- Add Permission for Workflow Page (Hidden Menu)
-- Parent ID: 2002 (应用管理)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2010, '工作流编排', 2002, 10, 'workflow', 'ai/app-manager/workflow/index', 1, 0, 'C', '1', '0', 'ai:app:workflow', '#', 1, NOW(), NULL, NULL, '工作流编排页面（隐藏）');

-- Assign Permission to Admin Role (Role ID: 1)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2010);
