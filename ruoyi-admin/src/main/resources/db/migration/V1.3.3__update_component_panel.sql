-- V1.3.3: 调整组件面板展示
-- 1. 将 TOOL 节点设为系统节点（is_system='1'），从组件面板中隐藏（仅通过工具面板选择）

UPDATE km_node_definition
SET is_system = '1', update_time = NOW()
WHERE node_type in('TOOL','SKILL');

-- 增加内置工具
-- 1. 先插入底层的内置工具 (Python 实现)
INSERT INTO public.km_builtin_tool
(tool_id, tool_name, spec, init_params, input_schema, output_schema, python_code, status, create_dept, create_by, create_time, update_by, update_time, del_flag, remark)
VALUES(2, 'builtin_increment', '加1的核心逻辑', '[{"name": "init_number", "type": "number", "required": false, "description": "初始数值", "displayName": "初始数值", "defaultValue": "0"}]'::jsonb, '{"type": "object", "properties": {"number": {"type": "number", "description": "初始数值"}}}'::jsonb, '{"type": "object", "required": ["new_number"], "properties": {"new_number": {"type": "number", "description": "增加 1 后的结果值"}}}'::jsonb, 'import json
import sys
def main(params):
    # 获取 "number", 如果不存在则默认为 None
    number = params.get("number")
    
    # Python 中使用 None 而不是 null, 且 if 语句需要冒号
    if number is None:
        number = params.get("init_number", 0)
        
    # 确保返回结果
    return {"new_number": int(number) + 1}
# 1. 按照 KMatrix 的约定从 sys.argv[1] 文件中读取输入参数 JSON
with open(sys.argv[1], ''r'', encoding=''utf-8'') as f:
    args = json.load(f)
# 2. 调用逻辑函数
result = main(args)
# 3. 将结果输出为 JSON 字符串，Java 后端会自动解析
print(json.dumps(result))', '0', NULL, 1, '2026-03-22 16:57:16.287', 1, '2026-03-22 18:43:47.229', '0', NULL) on conflict (tool_id) do nothing;

-- 2. 再插入封装好的技能 (Skill)
INSERT INTO public.km_skill
(skill_id, skill_name, spec, tool_bindings, input_schema, output_schema, status, create_dept, create_by, create_time, update_by, update_time, del_flag, remark)
VALUES(2, 'integer_increment_skill', '这是一个通用的整数自增技能，接收一个数字并返回其加1后的结果。', '[{"id": 2, "type": "builtin"}]'::jsonb, '{"type": "object", "required": ["number"], "properties": {"number": {"type": "number", "description": "要增加的整数数值"}}}'::jsonb, '{"type": "object", "required": ["result"], "properties": {"result": {"type": "number", "description": "计算得到的结果"}}}'::jsonb, '0', NULL, 1, '2026-03-22 16:57:16.297', 1, '2026-03-22 16:57:16.297', '0', NULL) on conflict (skill_id) do nothing;