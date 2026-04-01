-- V0.0.3__add_multimodal_params_to_nodes.sql
-- 为 START 节点添加多模态输出参数以支持文件和图片的传递
UPDATE km_node_definition 
SET output_params = '[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"用户的输入内容"},{"key":"sessionId","label":"会话ID","type":"string","required":true,"description":"当前会话的唯一标识"},{"key":"userId","label":"用户ID","type":"string","required":false,"description":"当前用户的ID"},{"key":"files","label":"多模态文件上传(全局)","type":"array","required":false,"description":"调试与对话窗口直接上传的多模态文件信息列表"},{"key":"ossIds","label":"文件ID列表","type":"array","required":false,"description":"调试窗口上传的文件对应的 OSS ID 列表"},{"key":"ossId","label":"单文件ID","type":"string","required":false,"description":"调试窗口上传的文件对应的首个 OSS ID"}]' 
WHERE node_type = 'START';

-- 为 LLM_CHAT 节点添加接受多模态组件的输入参数
UPDATE km_node_definition 
SET input_params = '[{"key":"userInput","label":"用户输入","type":"string","required":true,"description":"传递给 LLM 的用户输入"},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"图片或音频的 OSS ID 列表"}]' 
WHERE node_type = 'LLM_CHAT';

-- 为 IMAGE_OCR 节点添加接受多模态组件的输入参数
UPDATE km_node_definition 
SET input_params = '[{"key":"ossId","label":"单文件ID","type":"string","required":false,"description":"上游传入的首个OSS ID"},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"图片的 OSS ID 列表"}]' 
WHERE node_type = 'IMAGE_OCR';

-- 为 AUDIO_ASR 节点添加接受多模态组件的输入参数
UPDATE km_node_definition 
SET input_params = '[{"key":"ossId","label":"单文件ID","type":"string","required":false,"description":"上游传入的首个OSS ID"},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"音频的 OSS ID 列表"}]' 
WHERE node_type = 'AUDIO_ASR';
