-- =============================================
-- 工作流模板分类字典初始化脚本
-- 创建日期: 2026-02-08
-- 说明: 将工作流模板分类从硬编码改为字典动态维护
-- =============================================

-- 1. 插入字典类型
INSERT INTO sys_dict_type (dict_id, tenant_id, version, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (
    nextval('sys_dict_type_seq'),  -- 使用序列生成ID
    '000000',                       -- 租户ID
    1,                              -- 版本号
    '工作流模板分类',               -- 字典名称
    'km_workflow_template_category', -- 字典类型
    103,                            -- 创建部门
    1,                              -- 创建者
    now(),                          -- 创建时间
    NULL,                           -- 更新者
    NULL,                           -- 更新时间
    '工作流模板分类列表'            -- 备注
);

-- 2. 插入字典数据项
-- 知识问答
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (
    nextval('sys_dict_data_seq'),    -- 使用序列生成ID
    '000000',                         -- 租户ID
    1,                                -- 排序
    '知识问答',                       -- 字典标签
    'knowledge_qa',                   -- 字典键值
    'km_workflow_template_category',  -- 字典类型
    '',                               -- 样式属性
    'primary',                        -- 表格样式
    'N',                              -- 是否默认
    103,                              -- 创建部门
    1,                                -- 创建者
    now(),                            -- 创建时间
    NULL,                             -- 更新者
    NULL,                             -- 更新时间
    '知识问答类型的工作流模板'        -- 备注
);

-- 智能客服
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (
    nextval('sys_dict_data_seq'),
    '000000',
    2,
    '智能客服',
    'customer_service',
    'km_workflow_template_category',
    '',
    'success',
    'N',
    103,
    1,
    now(),
    NULL,
    NULL,
    '智能客服类型的工作流模板'
);

-- 营销
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (
    nextval('sys_dict_data_seq'),
    '000000',
    3,
    '营销',
    'marketing',
    'km_workflow_template_category',
    '',
    'warning',
    'N',
    103,
    1,
    now(),
    NULL,
    NULL,
    '营销类型的工作流模板'
);

-- 提交事务
COMMIT;
