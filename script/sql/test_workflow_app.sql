-- ============================================
-- 可扩展工作流测试配置 SQL 脚本
-- ============================================

-- 插入一个基于LLM的意图识别工作流应用
INSERT INTO `km_app` (
    `app_id`,
    `app_name`,
    `description`,
    `icon`,
    `app_type`,
    `status`,
    `prologue`,
    `model_setting`,
    `knowledge_setting`,
    `workflow_config`,
    `graph_data`,
    `dsl_data`,
    `model_id`,
    `create_dept`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`,
    `del_flag`,
    `remark`
) VALUES (
    1002,                                  -- app_id
    '科亿智能助手V3(意图分支)',             -- app_name
    '基于意图识别的条件分支工作流',        -- description
    '',                                    -- icon
    '2',                                   -- app_type (2=工作流)
    '1',                                   -- status (1=发布)
    '你好，我是科亿',                      -- prologue
    NULL,                                  -- model_setting (工作流中指定)
    NULL,                                  -- knowledge_setting
    NULL,                                  -- workflow_config (元配置)
    NULL,                                  -- graph_data (前端画布数据)
    '{
  "workflowId": "intent_workflow_v3",
  "name": "意图识别工作流V3",
  "entryPoint": "start",
  "nodes": [
    {
      "id": "start",
      "type": "START",
      "name": "开始",
      "config": {},
      "inputs": {
        "userInput": "${userInput}"
      }
    },
    {
      "id": "intent_detect",
      "type": "INTENT_CLASSIFIER",
      "name": "意图识别",
      "config": {
        "modelId": 1,
        "intents": ["greeting", "question", "other"]
      },
      "inputs": {
        "text": "${start.userInput}"
      }
    },
    {
      "id": "greeting_response",
      "type": "FIXED_RESPONSE",
      "name": "打招呼回复",
      "config": {
        "responseText": "您好！！"
      },
      "inputs": {}
    },
    {
      "id": "question_response",
      "type": "LLM_CHAT",
      "name": "问题回复",
      "config": {
        "modelId": 2004212458654392322
      },
      "inputs": {
        "systemPrompt": "你是科亿智能助手，请根据用户的问题提供专业的解答。"
      }
    },
    {
      "id": "other_response",
      "type": "FIXED_RESPONSE",
      "name": "其他回复",
      "config": {
        "responseText": "我还不懂这个问题"
      },
      "inputs": {}
    },
    {
      "id": "end",
      "type": "END",
      "name": "结束",
      "config": {},
      "inputs": {
        "finalResponse": "${response}"
      }
    }
  ],
  "edges": [
    {"from": "start", "to": "intent_detect"},
    {"from": "intent_detect", "to": "greeting_response", "condition": "intent == ''greeting''"},
    {"from": "intent_detect", "to": "question_response", "condition": "intent == ''question''"},
    {"from": "intent_detect", "to": "other_response", "condition": "intent == ''other''"},
    {"from": "greeting_response", "to": "end"},
    {"from": "question_response", "to": "end"},
    {"from": "other_response", "to": "end"}
  ]
}',                                         -- dsl_data
    NULL,                                   -- model_id
    NULL,                                   -- create_dept
    1,                                      -- create_by (管理员)
    NOW(),                                  -- create_time
    1,                                      -- update_by  
    NOW(),                                  -- update_time
    '0',                                    -- del_flag
    '基于意图识别的条件分支工作流，演示固定回复和LLM节点的组合使用'  -- remark
);

-- 说明:
-- 1. 使用可扩展工作流架构,支持节点扩展和条件路由
-- 2. 意图识别使用INTENT_CLASSIFIER节点(调用LLM)识别用户意图
-- 3. 根据不同意图通过条件边路由到不同节点:
--    - greeting → FIXED_RESPONSE节点返回固定文本"您好！！"
--    - question → LLM_CHAT节点调用大模型回答问题
--    - other → FIXED_RESPONSE节点返回固定文本"我还不懂这个问题"
-- 4. 所有节点执行记录都保存到 km_node_execution 表
-- 5. 工作流实例信息保存到 km_workflow_instance 表
-- 6. 条件边格式: {"condition": "intent == 'greeting'"} 支持 ==、!=、contains 等运算符
