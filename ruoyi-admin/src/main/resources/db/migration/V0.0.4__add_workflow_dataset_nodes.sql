-- V0.0.5__add_workflow_dataset_nodes.sql
-- 新增知识库工作流(fileProcessing)处理所需要的解析和存储节点定义

-- 为 km_app 表新增 use_type 字段
-- 区分应用用途：1=Chat对话应用(默认)，2=FileProcess文件处理工作流
ALTER TABLE km_app ADD COLUMN IF NOT EXISTS use_type VARCHAR(1) NOT NULL DEFAULT '1' ;

COMMENT ON COLUMN km_app.use_type IS '应用用途(1-chat对话 2-fileProcess文件处理)';

INSERT INTO km_node_definition (
  node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, allow_custom_input_params, allow_custom_output_params, input_params, output_params, version, create_time
) VALUES (
  18, 'FILE_PARSE', '文件解析', 'mdi-file-document-outline', '#10b981', 'fileProcessing', '将KmDocument关联的底层存储文件内容加载并转为纯文本信息', '0', '1', '1', '1',
  '[{"key":"documentId","label":"文档ID","type":"number","required":true,"description":"需要解析关联的知识库文件对象ID"}]',
  '[{"key":"text","label":"解析文本","type":"string","required":true,"description":"从文件抽取出的所有纯文本内容"}]',
  1, CURRENT_TIMESTAMP
) ON CONFLICT (node_def_id) DO NOTHING;

-- 追加 START 节点对 documentId 的原生透出支持，避免下游组件必须强行连入 userInput 造成混淆
UPDATE km_node_definition 
SET output_params = '[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"用户的输入内容"},{"key":"sessionId","label":"会话ID","type":"string","required":true,"description":"当前会话的唯一标识"},{"key":"userId","label":"用户ID","type":"string","required":false,"description":"当前用户的ID"},{"key":"files","label":"多模态文件上传(全局)","type":"array","required":false,"description":"调试与对话窗口直接上传的多模态文件信息列表"},{"key":"ossIds","label":"文件ID列表","type":"array","required":false,"description":"调试窗口上传的文件对应的 OSS ID 列表"},{"key":"ossId","label":"单文件ID","type":"string","required":false,"description":"调试窗口上传的文件对应的首个 OSS ID"},{"key":"documentId","label":"绑定文档ID","type":"string","required":false,"description":"若是处理文档等工作流，将原生抛出数字类主键ID"}]' 
WHERE node_type = 'START';

INSERT INTO km_node_definition (
  node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, allow_custom_input_params, allow_custom_output_params, input_params, output_params, version, create_time
) VALUES (
  19, 'DATASET_STORAGE', '自动切分入库', 'mdi-database-import', '#0ea5e9', 'fileProcessing', '将提取出的长文本使用系统ETL配置切块后向量化入库', '0', '1', '1', '1',
  '[{"key":"text","label":"待切分文本","type":"string","required":true,"description":"上游文本解析节点传来的全量纯文本内容"},{"key":"documentId","label":"文档ID","type":"number","required":true,"description":"系统回调状态所需的全局关联文件ID"},{"key":"chunkSize","label":"单切片最大长度","type":"number","required":false,"defaultValue":500,"description":"长文本分段的最大容量界限"},{"key":"overlap","label":"切片重叠字符","type":"number","required":false,"defaultValue":50,"description":"上下文过渡所需的交叉重叠长度"}]',
  '[{"key":"chunkCount","label":"生成组数量","type":"number","required":true,"description":"总共被切分出的段落实体总数"}]',
  1, CURRENT_TIMESTAMP
) ON CONFLICT (node_def_id) DO NOTHING;

 update sys_menu set menu_name = '工作流配置' where menu_id = 2300;

-- 插入文件处理相关节点的连线规则
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
(300, 'START', 'FILE_PARSE', '0', 10, '1', CURRENT_TIMESTAMP),
(301, 'FILE_PARSE', 'DATASET_STORAGE', '0', 10, '1', CURRENT_TIMESTAMP),
(302, 'DATASET_STORAGE', 'END', '0', 10, '1', CURRENT_TIMESTAMP),
(303, 'FILE_PARSE', 'END', '0', 10, '1', CURRENT_TIMESTAMP)
ON CONFLICT (rule_id) DO NOTHING;

-- 为 km_node_definition 表新增 require_ai_config 字段
ALTER TABLE km_node_definition ADD COLUMN IF NOT EXISTS require_ai_config VARCHAR(1) DEFAULT '0';
COMMENT ON COLUMN km_node_definition.require_ai_config IS '是否需要大模型高级配置选项(0-否 1-是)';

-- 更新已有的符合条件的AI相关节点
UPDATE km_node_definition 
SET require_ai_config = '1' 
WHERE node_type IN ('LLM_CHAT', 'DB_QUERY', 'SQL_GENERATE', 'INTENT_CLASSIFIER', 'AUDIO_ASR', 'IMAGE_OCR');
