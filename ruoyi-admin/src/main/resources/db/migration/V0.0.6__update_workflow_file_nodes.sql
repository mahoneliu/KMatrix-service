-- V0.0.6__update_workflow_file_nodes.sql
-- 更新工作流文件相关节点的输入输出定义以支持参数解耦与实时解析

-- 1. 更新 START 节点：增加 documentId 输出，规范化 files/file 输出
UPDATE km_node_definition 
SET output_params = '[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"用户的输入内容"},{"key":"sessionId","label":"会话ID","type":"string","required":true,"description":"当前会话的唯一标识"},{"key":"userId","label":"用户ID","type":"string","required":false,"description":"当前用户的ID"},{"key":"files","label":"所有文件","type":"array","required":false,"description":"本次交互上传的所有文件列表"},{"key":"file","label":"单个文件","type":"object","required":false,"description":"本次交互上传的第一个文件对象"},{"key":"documentId","label":"文档ID","type":"number","required":false,"description":"框架层注入的数据集处理文档ID"}]' 
WHERE node_type = 'START';

-- 2. 更新 FILE_STORAGE 节点：变更功能为“文档存储”，输入为 file 对象，输出为 documentId
UPDATE km_node_definition 
SET node_label = '文档存储',
    description = '将上传的临时文件收录到指定的知识库数据集，返回文档ID供下游使用',
    input_params = '[{"key":"file","label":"待存储文件","type":"object","required":true,"description":"StartNode透出的file对象"},{"key":"datasetId","label":"目标数据集","type":"number","required":false,"description":"可选，手动指定入库的数据集ID，未指定则使用节点配置或系统兜底"}]',
    output_params = '[{"key":"documentId","label":"收录文档ID","type":"number","required":true,"description":"入库成功后生成的唯一文档标识"}]'
WHERE node_type = 'FILE_STORAGE';

-- 3. 更新 FILE_PARSE 节点：增加对 file 对象的可选输入支持（支持不入库解析）
UPDATE km_node_definition 
SET input_params = '[{"key":"documentId","label":"待解析文档ID","type":"number","required":false,"description":"已入库的文档ID"},{"key":"file","label":"待解析文件对象","type":"object","required":false,"description":"未入库的实时文件对象，若提供则优先解析此文件"}]',
    output_params = '[{"key":"text","label":"解析文本","type":"string","required":true,"description":"从文件抽取出的所有纯文本内容"},{"key":"documentId","label":"关联文档ID","type":"number","required":false,"description":"如果解析的是库内文档，则透出原ID"}]'
WHERE node_type = 'FILE_PARSE';
