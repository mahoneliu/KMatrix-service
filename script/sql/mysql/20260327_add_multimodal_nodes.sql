-- 20260327_add_multimodal_nodes.sql

-- 新增加 FILE_STORAGE, AUDIO_ASR, IMAGE_OCR 节点定义
INSERT IGNORE INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    15, 'FILE_STORAGE', '文件处理', 'mdi-file-upload-outline', '#10B981', 
    'action', '上传图片或语音附件，并返回OSS ID供下游使用', '0', '1', 
    '1', '0', 
    '[]', 
    '[{"key": "ossIds", "type": "array", "label": "文件ID集合", "required": true, "description": "上传到系统的OSS ID列表"}]', 
    1, NOW(), NOW()
);

INSERT IGNORE INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    16, 'AUDIO_ASR', '语音转录', 'mdi-microphone', '#3B82F6', 
    'ai', '调用ASR模型将音频文件转录为文字', '0', '1', 
    '0', '0', 
    '[{"key": "ossId", "type": "string", "label": "音频附件ID", "required": false, "description": "上游传入的音频OSS ID"},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"音频的 OSS ID 列表"}]', 
    '[{"key": "transcription", "type": "string", "label": "转录文本", "required": true, "description": "语音转录后的文字内容"}]', 
    1, NOW(), NOW()
);

INSERT IGNORE INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    17, 'IMAGE_OCR', '图像识别', 'mdi-text-recognition', '#F59E0B', 
    'ai', '调用视觉大模型或OCR服务识别图像中的内容', '0', '1', 
    '0', '0', 
    '[{"key": "ossId", "type": "string", "label": "图片附件ID", "required": false, "description": "上游传入的图片OSS ID"},{"key":"files","label":"多模态文件对象","type":"array","required":false,"description":"多模态组件的文件对象列表(优先识别)"},{"key":"ossIds","label":"文件ID列表(ossIds)","type":"array","required":false,"description":"图片的 OSS ID 列表"}]', 
    '[{"key": "text", "type": "string", "label": "识别结果", "required": true, "description": "图像识别出来的文本"}]', 
    1, NOW(), NOW()
);

-- 插入 km_node_connection_rule 默认连接规则
INSERT IGNORE INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
-- START -> Multimodal Nodes
(200, 'START', 'FILE_STORAGE', '0', 10, '1', NOW()),
(201, 'START', 'AUDIO_ASR', '0', 10, '1', NOW()),
(202, 'START', 'IMAGE_OCR', '0', 10, '1', NOW()),

-- FILE_STORAGE -> Linked Nodes
(203, 'FILE_STORAGE', 'LLM_CHAT', '0', 10, '1', NOW()),
(204, 'FILE_STORAGE', 'AUDIO_ASR', '0', 10, '1', NOW()),
(205, 'FILE_STORAGE', 'IMAGE_OCR', '0', 10, '1', NOW()),
(206, 'FILE_STORAGE', 'CONDITION', '0', 10, '1', NOW()),
(207, 'FILE_STORAGE', 'END', '0', 10, '1', NOW()),

-- AUDIO_ASR -> Linked Nodes
(208, 'AUDIO_ASR', 'LLM_CHAT', '0', 10, '1', NOW()),
(209, 'AUDIO_ASR', 'CONDITION', '0', 10, '1', NOW()),
(210, 'AUDIO_ASR', 'END', '0', 10, '1', NOW()),

-- IMAGE_OCR -> Linked Nodes
(211, 'IMAGE_OCR', 'LLM_CHAT', '0', 10, '1', NOW()),
(212, 'IMAGE_OCR', 'CONDITION', '0', 10, '1', NOW()),
(213, 'IMAGE_OCR', 'END', '0', 10, '1', NOW());
