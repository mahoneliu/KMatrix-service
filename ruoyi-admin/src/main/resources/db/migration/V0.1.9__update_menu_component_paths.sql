-- V0.1.9__update_menu_component_paths.sql

-- 菜单层级调整，AI往上移
update sys_menu set parent_id=0 where parent_id=2000;
delete from sys_menu where menu_id = 2000;
update sys_menu set  order_num=1 where menu_id=2100;
update sys_menu set  order_num=3 where menu_id=2400;
update sys_menu set  order_num=4 where menu_id=2200;
update sys_menu set  order_num=5 where menu_id=2300;
update sys_menu set  order_num=6 where menu_id=1;
update sys_menu set  order_num=7 where menu_id=3;
update sys_menu set  order_num=8 where menu_id=2;
update sys_menu set  order_num=9 where menu_id=4;
update sys_menu set  order_num=10 where menu_id=5;

-- 新增工具执行一级菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES ('2500', '工具执行', '0', '2', 'execution', null, '', '1', '0', 'M', '0', '0', '', 'mdi-tools', 103, 1, now(), null, null, '工具执行模块根菜单')
ON CONFLICT (menu_id) DO NOTHING;
     
-- 菜单 component 字段路径更新：
--   将四个模块（应用、知识库、大模型、工作流）的菜单 component 字段
--   从 ai/xxx/... 格式更新为 xxx/... 格式，
--   与前端视图目录从 views/ai/xxx/ 迁移到 views/xxx/ 保持一致。

-- 大模型模块（原 ai/model/）
UPDATE sys_menu SET component = 'model/model-manager/index'   WHERE menu_id = 2101;

-- 知识库模块（原 ai/knowledge/）
UPDATE sys_menu SET component = 'knowledge/knowledge-manager/index'  WHERE menu_id = 2201;
UPDATE sys_menu SET component = 'knowledge/knowledge-detail/index'   WHERE menu_id = 2202;
UPDATE sys_menu SET component = 'knowledge/chunk-manager/index'      WHERE menu_id = 2203;
UPDATE sys_menu SET component = 'knowledge/document-upload/index'    WHERE menu_id = 2204;

-- 工作流模块（原 ai/workflow/）
UPDATE sys_menu SET component = 'workflow/workflow-template/index'   WHERE menu_id = 2301;
UPDATE sys_menu SET component = 'workflow/node-definition/index'     WHERE menu_id = 2302;
UPDATE sys_menu SET component = 'workflow/editor/index'              WHERE menu_id = 2303;
UPDATE sys_menu SET component = 'workflow/template-editor/index'     WHERE menu_id = 2304;
UPDATE sys_menu SET component = 'workflow/connection-rule-manager/index'     WHERE menu_id = 2350;
UPDATE sys_menu SET component = 'workflow/datasource-manager/index',parent_id=2300  WHERE menu_id = 2404;

-- 应用管理模块（原 ai/app/）
UPDATE sys_menu SET component = 'app/app-manager/index'              WHERE menu_id = 2401;
UPDATE sys_menu SET component = 'app/app-detail/index'               WHERE menu_id = 2402;
UPDATE sys_menu SET component = 'app/chat/index'                     WHERE menu_id = 2403;
UPDATE sys_menu SET component = 'app/rate-limit/index'               WHERE menu_id = 2405;


-- 执行引擎模块重构：
--   将 MCP服务、MCP市场、工具管理、技能管理 从「大模型」模块
--   迁移到新的「执行层」顶级模块（views/execution/）。
--   同步更新 sys_menu 的 component 字段路径。

-- 更新 component 字段：去掉 model/ 前缀，改为 execution/
UPDATE sys_menu SET parent_id=2500, component = 'execution/mcp-manager/index'   WHERE menu_id = 2102;
UPDATE sys_menu SET parent_id=2500, component = 'execution/tool-manager/index'  WHERE menu_id = 2103;
UPDATE sys_menu SET parent_id=2500, component = 'execution/skill-manager/index' WHERE menu_id = 2104;

-- menu_id=2140（MCP注册源）的 component 原为 extend/mcp-manager/registry/index，
-- 对应前端 views/execution/mcp-manager/registry/index.vue，需同步更新。
UPDATE sys_menu SET parent_id=2500, component = 'execution/mcp-manager/registry/index' WHERE menu_id = 2140;

