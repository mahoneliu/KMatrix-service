-- ----------------------------
-- Menus for AI Application Manager
-- Parent Menu: AI Management (2000)
-- ----------------------------

-- 1. App Manager Menu
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2002, '应用管理', 2000, 2, 'app-manager', 'ai/app-manager/index', 1, 0, 'C', '0', '0', 'ai:app:list', 'app-store', 1, NOW(), NULL, NULL, 'AI应用管理菜单');

-- 2. Buttons for App Manager
-- Query
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2003, '应用查询', 2002, 1, '', '', 1, 0, 'F', '0', '0', 'ai:app:query', '#', 1, NOW(), NULL, NULL, '');
-- Add
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2004, '应用新增', 2002, 2, '', '', 1, 0, 'F', '0', '0', 'ai:app:add', '#', 1, NOW(), NULL, NULL, '');
-- Edit
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2005, '应用修改', 2002, 3, '', '', 1, 0, 'F', '0', '0', 'ai:app:edit', '#', 1, NOW(), NULL, NULL, '');
-- Remove
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2006, '应用删除', 2002, 4, '', '', 1, 0, 'F', '0', '0', 'ai:app:remove', '#', 1, NOW(), NULL, NULL, '');
-- Export
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2007, '应用导出', 2002, 5, '', '', 1, 0, 'F', '0', '0', 'ai:app:export', '#', 1, NOW(), NULL, NULL, '');

-- 3. Buttons for Workflow (associated permissions)
-- List
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2008, '工作流查询', 2002, 6, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:list', '#', 1, NOW(), NULL, NULL, '');
-- Add/Edit (Save)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2009, '工作流保存', 2002, 7, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:add,ai:workflow:edit', '#', 1, NOW(), NULL, NULL, '');


-- 4. Role-Menu Association (Admin - 1)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2002);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2003);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2004);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2005);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2006);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2007);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2008);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2009);


-- ----------------------------
-- Menus for AI Chat
-- ----------------------------

-- 5. Chat Page Menu (Hidden, accessed via app-manager)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2011, 'AI对话', 2000, 3, 'chat', 'ai/chat/index', 1, 0, 'C', '1', '0', 'ai:chat:view', 'chat', 1, NOW(), NULL, NULL, 'AI聊天对话页面');

-- 6. Buttons for Chat
-- Send Message
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2012, '发送消息', 2011, 1, '', '', 1, 0, 'F', '0', '0', 'ai:chat:send', '#', 1, NOW(), NULL, NULL, '');
-- View History
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2013, '查看历史', 2011, 2, '', '', 1, 0, 'F', '0', '0', 'ai:chat:history', '#', 1, NOW(), NULL, NULL, '');
-- Clear History
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2014, '清空对话', 2011, 3, '', '', 1, 0, 'F', '0', '0', 'ai:chat:clear', '#', 1, NOW(), NULL, NULL, '');

-- 7. Role-Menu Association for Chat (Admin - 1)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2011);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2012);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2013);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2014);
