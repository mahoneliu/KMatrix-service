-- =============================================
-- 工作流模板初始化数据
-- 执行前请确保 km_workflow_template 表已创建
-- =============================================

-- 内置系统模板: 标准知识库问答
-- graph_data 结构说明:
-- 1. nodes.type 必须为 "custom"
-- 2. nodes.data 必须包含 nodeType, config, paramBindings 等完整属性
-- 3. edges.type 必须为 "custom"
INSERT INTO km_workflow_template (
    template_id, template_name, template_code, description, icon, category, scope_type,
    workflow_config, graph_data, version, is_published, is_enabled, use_count,
    create_dept, create_by, create_time, update_by, update_time, remark
) VALUES (
    1, '标准知识库问答', 'standard_knowledge_qa',
    '基于知识库的智能问答模板，包含开始节点、知识检索节点、大模型对话节点和回复节点，支持混合检索和上下文注入。',
    'mdi:frequently-asked-questions', 'knowledge_qa', '0',
    '{"enableDebug": true}',
    '{
  "edges": [
    {
      "id": "e_start_to_retrieval",
      "type": "custom",
      "label": "",
      "source": "start",
      "target": "knowledge_retrieval_1",
      "animated": true,
      "updatable": "target"
    },
    {
      "id": "e_retrieval_to_llm",
      "type": "custom",
      "label": "",
      "source": "knowledge_retrieval_1",
      "target": "llm_chat_1",
      "animated": true,
      "updatable": "target"
    },
    {
      "id": "e_llm_to_end",
      "type": "custom",
      "label": "",
      "source": "llm_chat_1",
      "target": "end",
      "animated": true,
      "updatable": "target"
    }
  ],
  "nodes": [
    {
      "id": "start",
      "type": "custom",
      "position": { "x": 100, "y": 250 },
      "data": {
        "id": "start",
        "nodeType": "START",
        "nodeLabel": "开始",
        "nodeIcon": "mdi:play-circle",
        "nodeColor": "#4d4e4dff",
        "description": "工作流的入口节点",
        "status": "idle",
        "config": {
          "globalParams": [
            {"key": "userInput", "type": "string", "label": "用户问题", "required": true}
          ]
        },
        "paramBindings": [],
        "customInputParams": [],
        "customOutputParams": []
      }
    },
    {
      "id": "knowledge_retrieval_1",
      "type": "custom",
      "position": { "x": 400, "y": 250 },
      "data": {
        "id": "knowledge_retrieval_1",
        "nodeType": "KNOWLEDGE_RETRIEVAL",
        "nodeLabel": "知识检索",
        "nodeIcon": "mdi:book-search",
        "nodeColor": "#d39e23ff",
        "description": "从知识库中检索相关文档片段",
        "status": "idle",
        "config": {
          "mode": "HYBRID",
          "topK": 5,
          "threshold": 0.5,
          "kbIds": [],
          "enableRerank": false
        },
        "paramBindings": [
          {
            "paramKey": "query",
            "sourceKey": "start",
            "sourceType": "node",
            "sourceParam": "userInput"
          }
        ],
        "customInputParams": [],
        "customOutputParams": []
      }
    },
    {
      "id": "llm_chat_1",
      "type": "custom",
      "position": { "x": 700, "y": 250 },
      "data": {
        "id": "llm_chat_1",
        "nodeType": "LLM_CHAT",
        "nodeLabel": "大模型对话",
        "nodeIcon": "mdi:robot",
        "nodeColor": "#b427ebff",
        "description": "调用大语言模型进行对话",
        "status": "idle",
        "config": {
          "temperature": 0.7,
          "systemPrompt": "你是一个智能助手，请根据提供的上下文信息回答用户的问题。如果上下文中没有相关信息，请如实告知用户。",
          "userPrompt": "已知信息：${context}\n问题：${userInput}",
          "historyEnabled": true,
          "historyCount": 10
        },
        "paramBindings": [
          {
            "paramKey": "userInput",
            "sourceKey": "start",
            "sourceType": "node",
            "sourceParam": "userInput"
          },
          {
            "paramKey": "context",
            "sourceKey": "knowledge_retrieval_1",
            "sourceType": "node",
            "sourceParam": "context"
          }
        ],
        "customInputParams": [],
        "customOutputParams": []
      }
    },
    {
      "id": "end",
      "type": "custom",
      "position": { "x": 1000, "y": 250 },
      "data": {
        "id": "end",
        "nodeType": "END",
        "nodeLabel": "结束",
        "nodeIcon": "mdi:stop-circle",
        "nodeColor": "#9875BFFF",
        "description": "工作流的结束节点",
        "status": "idle",
        "config": {},
        "paramBindings": [
          {
            "paramKey": "finalResponse",
            "sourceKey": "llm_chat_1",
            "sourceType": "node",
            "sourceParam": "response"
          }
        ],
        "customInputParams": [],
        "customOutputParams": []
      }
    }
  ]
}',
    1, '1', '1', 0,
    103, 1, NOW(), 1, NOW(), '系统内置模板，不可修改删除'
) ON CONFLICT (template_id) DO UPDATE 
SET graph_data = EXCLUDED.graph_data, 
    update_time = NOW();

-- =============================================
-- 菜单配置 (需要在 sys_menu 表中添加)
-- 菜单ID: 2020 (请根据实际情况调整，避免冲突)
-- =============================================

-- 工作流模板菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark) 
VALUES 
(2020, '工作流模板', 2000, 3, 'workflow-template', 'ai/workflow-template/index', '1', '0', 'C', '0', '0', 'ai:workflowTemplate:list', 'mdi:file-document-outline', 103, 1, NOW(), '工作流模板管理'),
(2025, '模板工作流编排', 2000, 10, 'template-editor', 'ai/template-editor/index', '1', '0', 'C', '1', '0', 'ai:templateEditor:view', 'mdi:database-search', 103, 1, NOW(), '模板工作流编排')
ON CONFLICT (menu_id) DO NOTHING;

-- 工作流模板按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark) 
VALUES (2021, '模板查询', 2020, 1, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:query', '#', 103, 1, NOW(), '') ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (2022, '模板新增', 2020, 2, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:add', '#', 103, 1, NOW(), '') ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (2023, '模板修改', 2020, 3, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:edit', '#', 103, 1, NOW(), '') ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (2024, '模板删除', 2020, 4, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:remove', '#', 103, 1, NOW(), '') ON CONFLICT (menu_id) DO NOTHING;
