-- ======================================================================
-- KMatrix 数据库初始化脚本 (合并版)
-- PostgreSQL 17+
-- 生成时间: 2026-03-27
-- 说明: 合并所有Flyway增量脚本，包含幂等性处理
-- ======================================================================

-- ======================================================================
-- 第一部分: 扩展和函数定义
-- ======================================================================

CREATE EXTENSION IF NOT EXISTS pgroonga;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE OR REPLACE FUNCTION cast_varchar_to_timestamp(character varying) RETURNS timestamp with time zone
    LANGUAGE sql STRICT
    AS $_$
select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');
$_$;

-- ======================================================================
-- 第二部分: 表结构定义 (DDL集中)
-- ======================================================================

-- ======================================================================
-- 表: sys_social
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_social (
    id                 int8             not null,
    user_id            int8             not null,
    auth_id            varchar(255)     not null,
    source             varchar(255)     not null,
    open_id            varchar(255)     default null::varchar,
    user_name          varchar(30)      not null,
    nick_name          varchar(30)      default ''::varchar,
    email              varchar(255)     default ''::varchar,
    avatar             varchar(500)     default ''::varchar,
    access_token       varchar(2000)    not null,
    expire_in          int8             default null,
    refresh_token      varchar(2000)    default null::varchar,
    access_code        varchar(255)     default null::varchar,
    union_id           varchar(255)     default null::varchar,
    scope              varchar(255)     default null::varchar,
    token_type         varchar(255)     default null::varchar,
    id_token           varchar(2000)    default null::varchar,
    mac_algorithm      varchar(255)     default null::varchar,
    mac_key            varchar(255)     default null::varchar,
    code               varchar(255)     default null::varchar,
    oauth_token        varchar(255)     default null::varchar,
    oauth_token_secret varchar(255)     default null::varchar,
    create_dept        int8,
    create_by          int8,
    create_time        timestamp,
    update_by          int8,
    update_time        timestamp,
    del_flag           char             default '0'::bpchar,
    constraint "pk_sys_social" primary key (id)
);

COMMENT ON TABLE sys_social IS '社会化关系表';
COMMENT ON COLUMN sys_social.access_code IS '平台的授权信息，部分平台可能没有';
COMMENT ON COLUMN sys_social.access_token IS '用户的授权令牌';
COMMENT ON COLUMN sys_social.auth_id IS '平台+平台唯一id';
COMMENT ON COLUMN sys_social.avatar IS '头像地址';
COMMENT ON COLUMN sys_social.code IS '用户的授权code，部分平台可能没有';
COMMENT ON COLUMN sys_social.create_by IS '创建者';
COMMENT ON COLUMN sys_social.create_dept IS '创建部门';
COMMENT ON COLUMN sys_social.create_time IS '创建时间';
COMMENT ON COLUMN sys_social.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN sys_social.email IS '用户邮箱';
COMMENT ON COLUMN sys_social.expire_in IS '用户的授权令牌的有效期，部分平台可能没有';
COMMENT ON COLUMN sys_social.id IS '主键';
COMMENT ON COLUMN sys_social.id_token IS 'id token，部分平台可能没有';
COMMENT ON COLUMN sys_social.mac_algorithm IS '小米平台用户的附带属性，部分平台可能没有';
COMMENT ON COLUMN sys_social.mac_key IS '小米平台用户的附带属性，部分平台可能没有';
COMMENT ON COLUMN sys_social.nick_name IS '用户昵称';
COMMENT ON COLUMN sys_social.oauth_token IS 'Twitter平台用户的附带属性，部分平台可能没有';
COMMENT ON COLUMN sys_social.oauth_token_secret IS 'Twitter平台用户的附带属性，部分平台可能没有';
COMMENT ON COLUMN sys_social.open_id IS '平台编号唯一id';
COMMENT ON COLUMN sys_social.refresh_token IS '刷新令牌，部分平台可能没有';
COMMENT ON COLUMN sys_social.scope IS '授予的权限，部分平台可能没有';
COMMENT ON COLUMN sys_social.source IS '用户来源';
COMMENT ON COLUMN sys_social.token_type IS '个别平台的授权信息，部分平台可能没有';
COMMENT ON COLUMN sys_social.union_id IS '用户的 unionid';
COMMENT ON COLUMN sys_social.update_by IS '更新者';
COMMENT ON COLUMN sys_social.update_time IS '更新时间';
COMMENT ON COLUMN sys_social.user_id IS '用户ID';
COMMENT ON COLUMN sys_social.user_name IS '登录账号';

-- ======================================================================
-- 表: sys_dept
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_dept (
    dept_id     int8,
    parent_id   int8        default 0,
    ancestors   varchar(500)default ''::varchar,
    dept_name   varchar(30) default ''::varchar,
    dept_category varchar(100) default null::varchar,
    order_num   int4        default 0,
    leader      int8        default null,
    phone       varchar(11) default null::varchar,
    email       varchar(50) default null::varchar,
    status      char        default '0'::bpchar,
    del_flag    char        default '0'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    constraint "sys_dept_pk" primary key (dept_id)
);

COMMENT ON TABLE sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.ancestors IS '祖级列表';
COMMENT ON COLUMN sys_dept.create_by IS '创建者';
COMMENT ON COLUMN sys_dept.create_dept IS '创建部门';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN sys_dept.dept_category IS '部门类别编码';
COMMENT ON COLUMN sys_dept.dept_id IS '部门ID';
COMMENT ON COLUMN sys_dept.dept_name IS '部门名称';
COMMENT ON COLUMN sys_dept.email IS '邮箱';
COMMENT ON COLUMN sys_dept.leader IS '负责人';
COMMENT ON COLUMN sys_dept.order_num IS '显示顺序';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门ID';
COMMENT ON COLUMN sys_dept.phone IS '联系电话';
COMMENT ON COLUMN sys_dept.status IS '部门状态（0正常 1停用）';
COMMENT ON COLUMN sys_dept.update_by IS '更新者';
COMMENT ON COLUMN sys_dept.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_user
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_user (
    user_id     int8,
    dept_id     int8,
    user_name   varchar(100)  not null,
    nick_name   varchar(100)  not null,
    user_type   varchar(10)  default 'sys_user'::varchar,
    email       varchar(100)  default ''::varchar,
    phonenumber varchar(15)  default ''::varchar,
    sex         char         default '0'::bpchar,
    avatar      int8,
    password    varchar(100) default ''::varchar,
    status      char         default '0'::bpchar,
    del_flag    char         default '0'::bpchar,
    login_ip    varchar(128) default ''::varchar,
    login_date  timestamp,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    rate_limit_config TEXT DEFAULT NULL,
    constraint "sys_user_pk" primary key (user_id)
);

COMMENT ON TABLE sys_user IS '用户信息表';
COMMENT ON COLUMN sys_user.avatar IS '头像地址';
COMMENT ON COLUMN sys_user.create_by IS '创建者';
COMMENT ON COLUMN sys_user.create_dept IS '创建部门';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN sys_user.dept_id IS '部门ID';
COMMENT ON COLUMN sys_user.email IS '用户邮箱';
COMMENT ON COLUMN sys_user.login_date IS '最后登陆时间';
COMMENT ON COLUMN sys_user.login_ip IS '最后登陆IP';
COMMENT ON COLUMN sys_user.nick_name IS '用户昵称';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.phonenumber IS '手机号码';
COMMENT ON COLUMN sys_user.rate_limit_config IS '聊天限流配置(JSON)，结构：{"minute":{"requests":N,"tokens":N},"hour":{...},"day":{...}}，为空时使用系统默认配置';
COMMENT ON COLUMN sys_user.remark IS '备注';
COMMENT ON COLUMN sys_user.sex IS '用户性别（0男 1女 2未知）';
COMMENT ON COLUMN sys_user.status IS '帐号状态（0正常 1停用）';
COMMENT ON COLUMN sys_user.update_by IS '更新者';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.user_id IS '用户ID';
COMMENT ON COLUMN sys_user.user_name IS '用户账号';
COMMENT ON COLUMN sys_user.user_type IS '用户类型（sys_user系统用户）';

-- ======================================================================
-- 表: sys_post
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_post (
    post_id     int8,
    dept_id     int8,
    post_code   varchar(64) not null,
    post_category   varchar(100) default null,
    post_name   varchar(50) not null,
    post_sort   int4        not null,
    status      char        not null,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint "sys_post_pk" primary key (post_id)
);

COMMENT ON TABLE sys_post IS '岗位信息表';
COMMENT ON COLUMN sys_post.create_by IS '创建者';
COMMENT ON COLUMN sys_post.create_dept IS '创建部门';
COMMENT ON COLUMN sys_post.create_time IS '创建时间';
COMMENT ON COLUMN sys_post.dept_id IS '部门id';
COMMENT ON COLUMN sys_post.post_category IS '岗位类别编码';
COMMENT ON COLUMN sys_post.post_code IS '岗位编码';
COMMENT ON COLUMN sys_post.post_id IS '岗位ID';
COMMENT ON COLUMN sys_post.post_name IS '岗位名称';
COMMENT ON COLUMN sys_post.post_sort IS '显示顺序';
COMMENT ON COLUMN sys_post.remark IS '备注';
COMMENT ON COLUMN sys_post.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_post.update_by IS '更新者';
COMMENT ON COLUMN sys_post.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_role
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_role (
    role_id             int8,
    role_name           varchar(30)  not null,
    role_key            varchar(100) not null,
    role_sort           int4         not null,
    data_scope          char         default '1'::bpchar,
    menu_check_strictly bool         default true,
    dept_check_strictly bool         default true,
    status              char         not null,
    del_flag            char         default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    remark              varchar(500) default null::varchar,
    constraint "sys_role_pk" primary key (role_id)
);

COMMENT ON TABLE sys_role IS '角色信息表';
COMMENT ON COLUMN sys_role.create_by IS '创建者';
COMMENT ON COLUMN sys_role.create_dept IS '创建部门';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
COMMENT ON COLUMN sys_role.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN sys_role.dept_check_strictly IS '部门树选择项是否关联显示';
COMMENT ON COLUMN sys_role.menu_check_strictly IS '菜单树选择项是否关联显示';
COMMENT ON COLUMN sys_role.remark IS '备注';
COMMENT ON COLUMN sys_role.role_id IS '角色ID';
COMMENT ON COLUMN sys_role.role_key IS '角色权限字符串';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_sort IS '显示顺序';
COMMENT ON COLUMN sys_role.status IS '角色状态（0正常 1停用）';
COMMENT ON COLUMN sys_role.update_by IS '更新者';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_menu
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id     int8,
    menu_name   varchar(50) not null,
    parent_id   int8         default 0,
    order_num   int4         default 0,
    path        varchar(200) default ''::varchar,
    component   varchar(255) default null::varchar,
    query_param varchar(255) default null::varchar,
    is_frame    char         default '1'::bpchar,
    is_cache    char         default '0'::bpchar,
    menu_type   char         default ''::bpchar,
    visible     char         default '0'::bpchar,
    status      char         default '0'::bpchar,
    perms       varchar(100) default null::varchar,
    icon        varchar(100) default '#'::varchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default ''::varchar,
    constraint "sys_menu_pk" primary key (menu_id)
);

COMMENT ON TABLE sys_menu IS '菜单权限表';
COMMENT ON COLUMN sys_menu.component IS '组件路径';
COMMENT ON COLUMN sys_menu.create_by IS '创建者';
COMMENT ON COLUMN sys_menu.create_dept IS '创建部门';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.is_cache IS '是否缓存（0缓存 1不缓存）';
COMMENT ON COLUMN sys_menu.is_frame IS '是否为外链（0是 1否）';
COMMENT ON COLUMN sys_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN sys_menu.menu_type IS '菜单类型（M目录 C菜单 F按钮）';
COMMENT ON COLUMN sys_menu.order_num IS '显示顺序';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';
COMMENT ON COLUMN sys_menu.path IS '路由地址';
COMMENT ON COLUMN sys_menu.perms IS '权限标识';
COMMENT ON COLUMN sys_menu.query_param IS '路由参数';
COMMENT ON COLUMN sys_menu.remark IS '备注';
COMMENT ON COLUMN sys_menu.status IS '菜单状态（0正常 1停用）';
COMMENT ON COLUMN sys_menu.update_by IS '更新者';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.visible IS '显示状态（0显示 1隐藏）';

-- ======================================================================
-- 表: sys_user_role
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id int8 not null,
    role_id int8 not null,
    constraint sys_user_role_pk primary key (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户和角色关联表';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';

-- ======================================================================
-- 表: sys_role_menu
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id int8 not null,
    menu_id int8 not null,
    constraint sys_role_menu_pk primary key (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色和菜单关联表';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';

-- ======================================================================
-- 表: sys_role_dept
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id int8 not null,
    dept_id int8 not null,
    constraint sys_role_dept_pk primary key (role_id, dept_id)
);

COMMENT ON TABLE sys_role_dept IS '角色和部门关联表';
COMMENT ON COLUMN sys_role_dept.dept_id IS '部门ID';
COMMENT ON COLUMN sys_role_dept.role_id IS '角色ID';

-- ======================================================================
-- 表: sys_user_post
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_user_post (
    user_id int8 not null,
    post_id int8 not null,
    constraint sys_user_post_pk primary key (user_id, post_id)
);

COMMENT ON TABLE sys_user_post IS '用户与岗位关联表';
COMMENT ON COLUMN sys_user_post.post_id IS '岗位ID';
COMMENT ON COLUMN sys_user_post.user_id IS '用户ID';

-- ======================================================================
-- 表: sys_oper_log
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_oper_log (
    oper_id        int8,
    title          varchar(50)   default ''::varchar,
    business_type  int4          default 0,
    method         varchar(100)  default ''::varchar,
    request_method varchar(10)   default ''::varchar,
    operator_type  int4          default 0,
    oper_name      varchar(50)   default ''::varchar,
    dept_name      varchar(50)   default ''::varchar,
    oper_url       varchar(255)  default ''::varchar,
    oper_ip        varchar(128)  default ''::varchar,
    oper_location  varchar(255)  default ''::varchar,
    oper_param     text default ''::varchar,
    json_result    text default ''::varchar,
    status         int4          default 0,
    error_msg      text default ''::varchar,
    oper_time      timestamp,
    cost_time      int8          default 0,
    constraint sys_oper_log_pk primary key (oper_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_oper_log_bt ON sys_oper_log (business_type);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_s ON sys_oper_log (status);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_ot ON sys_oper_log (oper_time);

COMMENT ON TABLE sys_oper_log IS '操作日志记录';
COMMENT ON COLUMN sys_oper_log.business_type IS '业务类型（0其它 1新增 2修改 3删除）';
COMMENT ON COLUMN sys_oper_log.cost_time IS '消耗时间';
COMMENT ON COLUMN sys_oper_log.dept_name IS '部门名称';
COMMENT ON COLUMN sys_oper_log.error_msg IS '错误消息';
COMMENT ON COLUMN sys_oper_log.json_result IS '返回参数';
COMMENT ON COLUMN sys_oper_log.method IS '方法名称';
COMMENT ON COLUMN sys_oper_log.oper_id IS '日志主键';
COMMENT ON COLUMN sys_oper_log.oper_ip IS '主机地址';
COMMENT ON COLUMN sys_oper_log.oper_location IS '操作地点';
COMMENT ON COLUMN sys_oper_log.oper_name IS '操作人员';
COMMENT ON COLUMN sys_oper_log.oper_param IS '请求参数';
COMMENT ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT ON COLUMN sys_oper_log.oper_url IS '请求URL';
COMMENT ON COLUMN sys_oper_log.operator_type IS '操作类别（0其它 1后台用户 2手机端用户）';
COMMENT ON COLUMN sys_oper_log.request_method IS '请求方式';
COMMENT ON COLUMN sys_oper_log.status IS '操作状态（0正常 1异常）';
COMMENT ON COLUMN sys_oper_log.title IS '模块标题';

-- ======================================================================
-- 表: sys_dict_type
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
    dict_id     int8,
    dict_name   varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_type_pk primary key (dict_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_dict_type_index1 ON sys_dict_type (dict_type);

COMMENT ON TABLE sys_dict_type IS '字典类型表';
COMMENT ON COLUMN sys_dict_type.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_type.create_dept IS '创建部门';
COMMENT ON COLUMN sys_dict_type.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_type.dict_id IS '字典主键';
COMMENT ON COLUMN sys_dict_type.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict_type.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_type.remark IS '备注';
COMMENT ON COLUMN sys_dict_type.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_type.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_dict_data
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
    dict_code   int8,
    dict_sort   int4         default 0,
    dict_label  varchar(100) default ''::varchar,
    dict_value  varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    css_class   varchar(100) default null::varchar,
    list_class  varchar(100) default null::varchar,
    is_default  char         default 'N'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_data_pk primary key (dict_code)
);

COMMENT ON TABLE sys_dict_data IS '字典数据表';
COMMENT ON COLUMN sys_dict_data.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_data.create_dept IS '创建部门';
COMMENT ON COLUMN sys_dict_data.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_data.css_class IS '样式属性（其他样式扩展）';
COMMENT ON COLUMN sys_dict_data.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dict_data.dict_label IS '字典标签';
COMMENT ON COLUMN sys_dict_data.dict_sort IS '字典排序';
COMMENT ON COLUMN sys_dict_data.dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_data.dict_value IS '字典键值';
COMMENT ON COLUMN sys_dict_data.is_default IS '是否默认（Y是 N否）';
COMMENT ON COLUMN sys_dict_data.list_class IS '表格回显样式';
COMMENT ON COLUMN sys_dict_data.remark IS '备注';
COMMENT ON COLUMN sys_dict_data.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_data.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_config
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_config (
    config_id    int8,
    config_name  varchar(100) default ''::varchar,
    config_key   varchar(100) default ''::varchar,
    config_value varchar(500) default ''::varchar,
    config_type  char         default 'N'::bpchar,
    create_dept  int8,
    create_by    int8,
    create_time  timestamp,
    update_by    int8,
    update_time  timestamp,
    remark       varchar(500) default null::varchar,
    constraint sys_config_pk primary key (config_id)
);

COMMENT ON TABLE sys_config IS '参数配置表';
COMMENT ON COLUMN sys_config.config_id IS '参数主键';
COMMENT ON COLUMN sys_config.config_key IS '参数键名';
COMMENT ON COLUMN sys_config.config_name IS '参数名称';
COMMENT ON COLUMN sys_config.config_type IS '系统内置（Y是 N否）';
COMMENT ON COLUMN sys_config.config_value IS '参数键值';
COMMENT ON COLUMN sys_config.create_by IS '创建者';
COMMENT ON COLUMN sys_config.create_dept IS '创建部门';
COMMENT ON COLUMN sys_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_config.remark IS '备注';
COMMENT ON COLUMN sys_config.update_by IS '更新者';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_logininfor
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_logininfor (
    info_id        int8,
    user_name      varchar(50)  default ''::varchar,
    client_key     varchar(32)  default ''::varchar,
    device_type    varchar(32)  default ''::varchar,
    ipaddr         varchar(128) default ''::varchar,
    login_location varchar(255) default ''::varchar,
    browser        varchar(50)  default ''::varchar,
    os             varchar(50)  default ''::varchar,
    status         char         default '0'::bpchar,
    msg            varchar(255) default ''::varchar,
    login_time     timestamp,
    constraint sys_logininfor_pk primary key (info_id)
);

CREATE INDEX IF NOT EXISTS idx_sys_logininfor_s ON sys_logininfor (status);
CREATE INDEX IF NOT EXISTS idx_sys_logininfor_lt ON sys_logininfor (login_time);

COMMENT ON TABLE sys_logininfor IS '系统访问记录';
COMMENT ON COLUMN sys_logininfor.browser IS '浏览器类型';
COMMENT ON COLUMN sys_logininfor.client_key IS '客户端';
COMMENT ON COLUMN sys_logininfor.device_type IS '设备类型';
COMMENT ON COLUMN sys_logininfor.info_id IS '访问ID';
COMMENT ON COLUMN sys_logininfor.ipaddr IS '登录IP地址';
COMMENT ON COLUMN sys_logininfor.login_location IS '登录地点';
COMMENT ON COLUMN sys_logininfor.login_time IS '访问时间';
COMMENT ON COLUMN sys_logininfor.msg IS '提示消息';
COMMENT ON COLUMN sys_logininfor.os IS '操作系统';
COMMENT ON COLUMN sys_logininfor.status IS '登录状态（0成功 1失败）';
COMMENT ON COLUMN sys_logininfor.user_name IS '用户账号';

-- ======================================================================
-- 表: sys_notice
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_notice (
    notice_id      int8,
    notice_title   varchar(50)  not null,
    notice_type    char         not null,
    notice_content text,
    status         char         default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    remark         varchar(255) default null::varchar,
    constraint sys_notice_pk primary key (notice_id)
);

COMMENT ON TABLE sys_notice IS '通知公告表';
COMMENT ON COLUMN sys_notice.create_by IS '创建者';
COMMENT ON COLUMN sys_notice.create_dept IS '创建部门';
COMMENT ON COLUMN sys_notice.create_time IS '创建时间';
COMMENT ON COLUMN sys_notice.notice_content IS '公告内容';
COMMENT ON COLUMN sys_notice.notice_id IS '公告ID';
COMMENT ON COLUMN sys_notice.notice_title IS '公告标题';
COMMENT ON COLUMN sys_notice.notice_type IS '公告类型（1通知 2公告）';
COMMENT ON COLUMN sys_notice.remark IS '备注';
COMMENT ON COLUMN sys_notice.status IS '公告状态（0正常 1关闭）';
COMMENT ON COLUMN sys_notice.update_by IS '更新者';
COMMENT ON COLUMN sys_notice.update_time IS '更新时间';

-- ======================================================================
-- 表: gen_table
-- ======================================================================
CREATE TABLE IF NOT EXISTS gen_table (
    table_id          int8,
    data_name         varchar(200)  default ''::varchar,
    table_name        varchar(200)  default ''::varchar,
    table_comment     varchar(500)  default ''::varchar,
    sub_table_name    varchar(64)   default ''::varchar,
    sub_table_fk_name varchar(64)   default ''::varchar,
    class_name        varchar(100)  default ''::varchar,
    tpl_category      varchar(200)  default 'crud'::varchar,
    package_name      varchar(100)  default null::varchar,
    module_name       varchar(30)   default null::varchar,
    business_name     varchar(30)   default null::varchar,
    function_name     varchar(50)   default null::varchar,
    function_author   varchar(50)   default null::varchar,
    gen_type          char          default '0'::bpchar not null,
    gen_path          varchar(200)  default '/'::varchar,
    options           varchar(1000) default null::varchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    remark            varchar(500)  default null::varchar,
    constraint gen_table_pk primary key (table_id)
);

COMMENT ON TABLE gen_table IS '代码生成业务表';
COMMENT ON COLUMN gen_table.business_name IS '生成业务名';
COMMENT ON COLUMN gen_table.class_name IS '实体类名称';
COMMENT ON COLUMN gen_table.create_by IS '创建者';
COMMENT ON COLUMN gen_table.create_dept IS '创建部门';
COMMENT ON COLUMN gen_table.create_time IS '创建时间';
COMMENT ON COLUMN gen_table.data_name IS '数据源名称';
COMMENT ON COLUMN gen_table.function_author IS '生成功能作者';
COMMENT ON COLUMN gen_table.function_name IS '生成功能名';
COMMENT ON COLUMN gen_table.gen_path IS '生成路径（不填默认项目路径）';
COMMENT ON COLUMN gen_table.gen_type IS '生成代码方式（0zip压缩包 1自定义路径）';
COMMENT ON COLUMN gen_table.module_name IS '生成模块名';
COMMENT ON COLUMN gen_table.options IS '其它生成选项';
COMMENT ON COLUMN gen_table.package_name IS '生成包路径';
COMMENT ON COLUMN gen_table.remark IS '备注';
COMMENT ON COLUMN gen_table.sub_table_fk_name IS '子表关联的外键名';
COMMENT ON COLUMN gen_table.sub_table_name IS '关联子表的表名';
COMMENT ON COLUMN gen_table.table_comment IS '表描述';
COMMENT ON COLUMN gen_table.table_id IS '编号';
COMMENT ON COLUMN gen_table.table_name IS '表名称';
COMMENT ON COLUMN gen_table.tpl_category IS '使用的模板（CRUD单表操作 TREE树表操作）';
COMMENT ON COLUMN gen_table.update_by IS '更新者';
COMMENT ON COLUMN gen_table.update_time IS '更新时间';

-- ======================================================================
-- 表: gen_table_column
-- ======================================================================
CREATE TABLE IF NOT EXISTS gen_table_column (
    column_id      int8,
    table_id       int8,
    column_name    varchar(200) default null::varchar,
    column_comment varchar(500) default null::varchar,
    column_type    varchar(100) default null::varchar,
    java_type      varchar(500) default null::varchar,
    java_field     varchar(200) default null::varchar,
    is_pk          char         default null::bpchar,
    is_increment   char         default null::bpchar,
    is_required    char         default null::bpchar,
    is_insert      char         default null::bpchar,
    is_edit        char         default null::bpchar,
    is_list        char         default null::bpchar,
    is_query       char         default null::bpchar,
    query_type     varchar(200) default 'EQ'::varchar,
    html_type      varchar(200) default null::varchar,
    dict_type      varchar(200) default ''::varchar,
    sort           int4,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    constraint gen_table_column_pk primary key (column_id)
);

COMMENT ON TABLE gen_table_column IS '代码生成业务表字段';
COMMENT ON COLUMN gen_table_column.column_comment IS '列描述';
COMMENT ON COLUMN gen_table_column.column_id IS '编号';
COMMENT ON COLUMN gen_table_column.column_name IS '列名称';
COMMENT ON COLUMN gen_table_column.column_type IS '列类型';
COMMENT ON COLUMN gen_table_column.create_by IS '创建者';
COMMENT ON COLUMN gen_table_column.create_dept IS '创建部门';
COMMENT ON COLUMN gen_table_column.create_time IS '创建时间';
COMMENT ON COLUMN gen_table_column.dict_type IS '字典类型';
COMMENT ON COLUMN gen_table_column.html_type IS '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）';
COMMENT ON COLUMN gen_table_column.is_edit IS '是否编辑字段（1是）';
COMMENT ON COLUMN gen_table_column.is_increment IS '是否自增（1是）';
COMMENT ON COLUMN gen_table_column.is_insert IS '是否为插入字段（1是）';
COMMENT ON COLUMN gen_table_column.is_list IS '是否列表字段（1是）';
COMMENT ON COLUMN gen_table_column.is_pk IS '是否主键（1是）';
COMMENT ON COLUMN gen_table_column.is_query IS '是否查询字段（1是）';
COMMENT ON COLUMN gen_table_column.is_required IS '是否必填（1是）';
COMMENT ON COLUMN gen_table_column.java_field IS 'JAVA字段名';
COMMENT ON COLUMN gen_table_column.java_type IS 'JAVA类型';
COMMENT ON COLUMN gen_table_column.query_type IS '查询方式（等于、不等于、大于、小于、范围）';
COMMENT ON COLUMN gen_table_column.sort IS '排序';
COMMENT ON COLUMN gen_table_column.table_id IS '归属表编号';
COMMENT ON COLUMN gen_table_column.update_by IS '更新者';
COMMENT ON COLUMN gen_table_column.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_oss
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_oss (
    oss_id        int8,
    file_name     varchar(255) default ''::varchar not null,
    original_name varchar(255) default ''::varchar not null,
    file_suffix   varchar(10)  default ''::varchar not null,
    url           varchar(500) default ''::varchar not null,
    ext1          varchar(500) default ''::varchar,
    create_dept   int8,
    create_by     int8,
    create_time   timestamp,
    update_by     int8,
    update_time   timestamp,
    service       varchar(20)  default 'minio'::varchar,
    constraint sys_oss_pk primary key (oss_id)
);

COMMENT ON TABLE sys_oss IS 'OSS对象存储表';
COMMENT ON COLUMN sys_oss.create_by IS '上传人';
COMMENT ON COLUMN sys_oss.create_dept IS '创建部门';
COMMENT ON COLUMN sys_oss.create_time IS '创建时间';
COMMENT ON COLUMN sys_oss.ext1 IS '扩展字段';
COMMENT ON COLUMN sys_oss.file_name IS '文件名';
COMMENT ON COLUMN sys_oss.file_suffix IS '文件后缀名';
COMMENT ON COLUMN sys_oss.original_name IS '原名';
COMMENT ON COLUMN sys_oss.oss_id IS '对象存储主键';
COMMENT ON COLUMN sys_oss.service IS '服务商';
COMMENT ON COLUMN sys_oss.update_by IS '更新者';
COMMENT ON COLUMN sys_oss.update_time IS '更新时间';
COMMENT ON COLUMN sys_oss.url IS 'URL地址';

-- ======================================================================
-- 表: sys_oss_config
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_oss_config (
    oss_config_id int8,
    config_key    varchar(20)  default ''::varchar not null,
    access_key    varchar(255) default ''::varchar,
    secret_key    varchar(255) default ''::varchar,
    bucket_name   varchar(255) default ''::varchar,
    prefix        varchar(255) default ''::varchar,
    endpoint      varchar(255) default ''::varchar,
    domain        varchar(255) default ''::varchar,
    is_https      char         default 'N'::bpchar,
    region        varchar(255) default ''::varchar,
    access_policy char(1)      default '1'::bpchar not null,
    status        char         default '1'::bpchar,
    ext1          varchar(255) default ''::varchar,
    create_dept   int8,
    create_by     int8,
    create_time   timestamp,
    update_by     int8,
    update_time   timestamp,
    remark        varchar(500) default ''::varchar,
    constraint sys_oss_config_pk primary key (oss_config_id)
);

COMMENT ON TABLE sys_oss_config IS '对象存储配置表';
COMMENT ON COLUMN sys_oss_config.access_key IS 'accessKey';
COMMENT ON COLUMN sys_oss_config.access_policy IS '桶权限类型(0=private 1=public 2=custom)';
COMMENT ON COLUMN sys_oss_config.bucket_name IS '桶名称';
COMMENT ON COLUMN sys_oss_config.config_key IS '配置key';
COMMENT ON COLUMN sys_oss_config.create_by IS '创建者';
COMMENT ON COLUMN sys_oss_config.create_dept IS '创建部门';
COMMENT ON COLUMN sys_oss_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_oss_config.domain IS '自定义域名';
COMMENT ON COLUMN sys_oss_config.endpoint IS '访问站点';
COMMENT ON COLUMN sys_oss_config.ext1 IS '扩展字段';
COMMENT ON COLUMN sys_oss_config.is_https IS '是否https（Y=是,N=否）';
COMMENT ON COLUMN sys_oss_config.oss_config_id IS '主键';
COMMENT ON COLUMN sys_oss_config.prefix IS '前缀';
COMMENT ON COLUMN sys_oss_config.region IS '域';
COMMENT ON COLUMN sys_oss_config.remark IS '备注';
COMMENT ON COLUMN sys_oss_config.secret_key IS '秘钥';
COMMENT ON COLUMN sys_oss_config.status IS '是否默认（0=是,1=否）';
COMMENT ON COLUMN sys_oss_config.update_by IS '更新者';
COMMENT ON COLUMN sys_oss_config.update_time IS '更新时间';

-- ======================================================================
-- 表: sys_client
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_client (
    id                  int8,
    client_id           varchar(64)   default ''::varchar,
    client_key          varchar(32)   default ''::varchar,
    client_secret       varchar(255)  default ''::varchar,
    grant_type          varchar(255)  default ''::varchar,
    device_type         varchar(32)   default ''::varchar,
    active_timeout      int4          default 1800,
    timeout             int4          default 604800,
    status              char(1)       default '0'::bpchar,
    del_flag            char(1)       default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    constraint sys_client_pk primary key (id)
);

COMMENT ON TABLE sys_client IS '系统授权表';
COMMENT ON COLUMN sys_client.active_timeout IS 'token活跃超时时间';
COMMENT ON COLUMN sys_client.client_id IS '客户端id';
COMMENT ON COLUMN sys_client.client_key IS '客户端key';
COMMENT ON COLUMN sys_client.client_secret IS '客户端秘钥';
COMMENT ON COLUMN sys_client.create_by IS '创建者';
COMMENT ON COLUMN sys_client.create_dept IS '创建部门';
COMMENT ON COLUMN sys_client.create_time IS '创建时间';
COMMENT ON COLUMN sys_client.del_flag IS '删除标志（0代表存在 1代表删除）';
COMMENT ON COLUMN sys_client.device_type IS '设备类型';
COMMENT ON COLUMN sys_client.grant_type IS '授权类型';
COMMENT ON COLUMN sys_client.id IS '主键';
COMMENT ON COLUMN sys_client.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_client.timeout IS 'token固定超时';
COMMENT ON COLUMN sys_client.update_by IS '更新者';
COMMENT ON COLUMN sys_client.update_time IS '更新时间';

-- ======================================================================
-- 表: test_demo
-- ======================================================================
CREATE TABLE IF NOT EXISTS test_demo (
    id          int8,
    dept_id     int8,
    user_id     int8,
    order_num   int4            default 0,
    test_key    varchar(255),
    value       varchar(255),
    version     int4            default 0,
    create_dept int8,
    create_time timestamp,
    create_by   int8,
    update_time timestamp,
    update_by   int8,
    del_flag    int4            default 0
);

COMMENT ON TABLE test_demo IS '测试单表';
COMMENT ON COLUMN test_demo.create_by IS '创建人';
COMMENT ON COLUMN test_demo.create_dept IS '创建部门';
COMMENT ON COLUMN test_demo.create_time IS '创建时间';
COMMENT ON COLUMN test_demo.del_flag IS '删除标志';
COMMENT ON COLUMN test_demo.dept_id IS '部门id';
COMMENT ON COLUMN test_demo.id IS '主键';
COMMENT ON COLUMN test_demo.order_num IS '排序号';
COMMENT ON COLUMN test_demo.test_key IS 'key键';
COMMENT ON COLUMN test_demo.update_by IS '更新人';
COMMENT ON COLUMN test_demo.update_time IS '更新时间';
COMMENT ON COLUMN test_demo.user_id IS '用户id';
COMMENT ON COLUMN test_demo.value IS '值';
COMMENT ON COLUMN test_demo.version IS '版本';

-- ======================================================================
-- 表: test_tree
-- ======================================================================
CREATE TABLE IF NOT EXISTS test_tree (
    id          int8,
    parent_id   int8            default 0,
    dept_id     int8,
    user_id     int8,
    tree_name   varchar(255),
    version     int4            default 0,
    create_dept int8,
    create_time timestamp,
    create_by   int8,
    update_time timestamp,
    update_by   int8,
    del_flag    integer         default 0
);

COMMENT ON TABLE test_tree IS '测试树表';
COMMENT ON COLUMN test_tree.create_by IS '创建人';
COMMENT ON COLUMN test_tree.create_dept IS '创建部门';
COMMENT ON COLUMN test_tree.create_time IS '创建时间';
COMMENT ON COLUMN test_tree.del_flag IS '删除标志';
COMMENT ON COLUMN test_tree.dept_id IS '部门id';
COMMENT ON COLUMN test_tree.id IS '主键';
COMMENT ON COLUMN test_tree.parent_id IS '父id';
COMMENT ON COLUMN test_tree.tree_name IS '值';
COMMENT ON COLUMN test_tree.update_by IS '更新人';
COMMENT ON COLUMN test_tree.update_time IS '更新时间';
COMMENT ON COLUMN test_tree.user_id IS '用户id';
COMMENT ON COLUMN test_tree.version IS '版本';

-- ======================================================================
-- 表: sj_namespace
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_namespace (
    id          bigserial PRIMARY KEY,
    name        varchar(64)  NOT NULL,
    unique_id   varchar(64)  NOT NULL,
    description varchar(256) NOT NULL DEFAULT '',
    deleted     smallint     NOT NULL DEFAULT 0,
    create_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_namespace_01 ON sj_namespace (name);

COMMENT ON TABLE sj_namespace IS '命名空间';
COMMENT ON COLUMN sj_namespace.create_dt IS '创建时间';
COMMENT ON COLUMN sj_namespace.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_namespace.description IS '描述';
COMMENT ON COLUMN sj_namespace.id IS '主键';
COMMENT ON COLUMN sj_namespace.name IS '名称';
COMMENT ON COLUMN sj_namespace.unique_id IS '唯一id';
COMMENT ON COLUMN sj_namespace.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_group_config
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_group_config (
    id                bigserial PRIMARY KEY,
    namespace_id      varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        varchar(64)  NOT NULL DEFAULT '',
    description       varchar(256) NOT NULL DEFAULT '',
    token             varchar(64)  NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',
    group_status      smallint     NOT NULL DEFAULT 0,
    version           int          NOT NULL,
    group_partition   int          NOT NULL,
    id_generator_mode smallint     NOT NULL DEFAULT 1,
    init_scene        smallint     NOT NULL DEFAULT 0,
    create_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_group_config_01 ON sj_group_config (namespace_id, group_name);

COMMENT ON TABLE sj_group_config IS '组配置';
COMMENT ON COLUMN sj_group_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_group_config.description IS '组描述';
COMMENT ON COLUMN sj_group_config.group_name IS '组名称';
COMMENT ON COLUMN sj_group_config.group_partition IS '分区';
COMMENT ON COLUMN sj_group_config.group_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_group_config.id IS '主键';
COMMENT ON COLUMN sj_group_config.id_generator_mode IS '唯一id生成模式 默认号段模式';
COMMENT ON COLUMN sj_group_config.init_scene IS '是否初始化场景 0:否 1:是';
COMMENT ON COLUMN sj_group_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_group_config.token IS 'token';
COMMENT ON COLUMN sj_group_config.update_dt IS '修改时间';
COMMENT ON COLUMN sj_group_config.version IS '版本号';

-- ======================================================================
-- 表: sj_notify_config
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_notify_config (
    id                     bigserial PRIMARY KEY,
    namespace_id           varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             varchar(64)  NOT NULL,
    notify_name            varchar(64)  NOT NULL DEFAULT '',
    system_task_type       smallint     NOT NULL DEFAULT 3,
    notify_status          smallint     NOT NULL DEFAULT 0,
    recipient_ids          varchar(128) NOT NULL,
    notify_threshold       int          NOT NULL DEFAULT 0,
    notify_scene           smallint     NOT NULL DEFAULT 0,
    rate_limiter_status    smallint     NOT NULL DEFAULT 0,
    rate_limiter_threshold int          NOT NULL DEFAULT 0,
    description            varchar(256) NOT NULL DEFAULT '',
    create_dt              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_notify_config_01 ON sj_notify_config (namespace_id, group_name);

COMMENT ON TABLE sj_notify_config IS '通知配置';
COMMENT ON COLUMN sj_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_config.description IS '描述';
COMMENT ON COLUMN sj_notify_config.group_name IS '组名称';
COMMENT ON COLUMN sj_notify_config.id IS '主键';
COMMENT ON COLUMN sj_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_config.notify_name IS '通知名称';
COMMENT ON COLUMN sj_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN sj_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN sj_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN sj_notify_config.recipient_ids IS '接收人id列表';
COMMENT ON COLUMN sj_notify_config.system_task_type IS '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_notify_config.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_notify_recipient
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_notify_recipient (
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    recipient_name   varchar(64)  NOT NULL,
    notify_type      smallint     NOT NULL DEFAULT 0,
    notify_attribute varchar(512) NOT NULL,
    description      varchar(256) NOT NULL DEFAULT '',
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_notify_recipient_01 ON sj_notify_recipient (namespace_id);

COMMENT ON TABLE sj_notify_recipient IS '告警通知接收人';
COMMENT ON COLUMN sj_notify_recipient.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_recipient.description IS '描述';
COMMENT ON COLUMN sj_notify_recipient.id IS '主键';
COMMENT ON COLUMN sj_notify_recipient.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_recipient.notify_attribute IS '配置属性';
COMMENT ON COLUMN sj_notify_recipient.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook';
COMMENT ON COLUMN sj_notify_recipient.recipient_name IS '接收人名称';
COMMENT ON COLUMN sj_notify_recipient.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_retry_dead_letter
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry_dead_letter (
    id              bigserial PRIMARY KEY,
    namespace_id    varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name      varchar(64)  NOT NULL,
    group_id        bigint       NOT NULL,
    scene_name      varchar(64)  NOT NULL,
    scene_id        bigint       NOT NULL,
    idempotent_id   varchar(64)  NOT NULL,
    biz_no          varchar(64)  NOT NULL DEFAULT '',
    executor_name   varchar(512) NOT NULL DEFAULT '',
    serializer_name varchar(32)  NOT NULL DEFAULT 'jackson',
    args_str        text         NOT NULL,
    ext_attrs       text         NOT NULL,
    create_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_retry_dead_letter_01 ON sj_retry_dead_letter (namespace_id, group_name, scene_name);
CREATE INDEX IF NOT EXISTS idx_sj_retry_dead_letter_02 ON sj_retry_dead_letter (idempotent_id);
CREATE INDEX IF NOT EXISTS idx_sj_retry_dead_letter_03 ON sj_retry_dead_letter (biz_no);
CREATE INDEX IF NOT EXISTS idx_sj_retry_dead_letter_04 ON sj_retry_dead_letter (create_dt);

COMMENT ON TABLE sj_retry_dead_letter IS '死信队列表';
COMMENT ON COLUMN sj_retry_dead_letter.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_dead_letter.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_dead_letter.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_dead_letter.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_dead_letter.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_dead_letter.group_id IS '组Id';
COMMENT ON COLUMN sj_retry_dead_letter.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_dead_letter.id IS '主键';
COMMENT ON COLUMN sj_retry_dead_letter.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_dead_letter.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_dead_letter.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry_dead_letter.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_dead_letter.serializer_name IS '执行方法参数序列化器名称';

-- ======================================================================
-- 表: sj_retry
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry (
    id              bigserial PRIMARY KEY,
    namespace_id    varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name      varchar(64)  NOT NULL,
    group_id        bigint       NOT NULL,
    scene_name      varchar(64)  NOT NULL,
    scene_id        bigint       NOT NULL,
    idempotent_id   varchar(64)  NOT NULL,
    biz_no          varchar(64)  NOT NULL DEFAULT '',
    executor_name   varchar(512) NOT NULL DEFAULT '',
    args_str        text         NOT NULL,
    ext_attrs       text         NOT NULL,
    serializer_name varchar(32)  NOT NULL DEFAULT 'jackson',
    next_trigger_at bigint       NOT NULL,
    retry_count     int          NOT NULL DEFAULT 0,
    retry_status    smallint     NOT NULL DEFAULT 0,
    task_type       smallint     NOT NULL DEFAULT 1,
    bucket_index    int          NOT NULL DEFAULT 0,
    parent_id       bigint       NOT NULL DEFAULT 0,
    deleted         bigint       NOT NULL DEFAULT 0,
    create_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_retry_01 ON sj_retry (scene_id, task_type, idempotent_id, deleted);
CREATE INDEX IF NOT EXISTS idx_sj_retry_01 ON sj_retry (biz_no);
CREATE INDEX IF NOT EXISTS idx_sj_retry_02 ON sj_retry (idempotent_id);
CREATE INDEX IF NOT EXISTS idx_sj_retry_03 ON sj_retry (retry_status, bucket_index);
CREATE INDEX IF NOT EXISTS idx_sj_retry_04 ON sj_retry (parent_id);
CREATE INDEX IF NOT EXISTS idx_sj_retry_05 ON sj_retry (create_dt);

COMMENT ON TABLE sj_retry IS '重试信息表';
COMMENT ON COLUMN sj_retry.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_retry.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry.deleted IS '逻辑删除';
COMMENT ON COLUMN sj_retry.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry.group_id IS '组Id';
COMMENT ON COLUMN sj_retry.group_name IS '组名称';
COMMENT ON COLUMN sj_retry.id IS '主键';
COMMENT ON COLUMN sj_retry.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_retry.parent_id IS '父节点id';
COMMENT ON COLUMN sj_retry.retry_count IS '重试次数';
COMMENT ON COLUMN sj_retry.retry_status IS '重试状态 0、重试中 1、成功 2、最大重试次数';
COMMENT ON COLUMN sj_retry.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry.serializer_name IS '执行方法参数序列化器名称';
COMMENT ON COLUMN sj_retry.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_retry_task
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry_task (
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    scene_name       varchar(64)  NOT NULL,
    retry_id         bigint       NOT NULL,
    ext_attrs        text         NOT NULL,
    task_status      smallint     NOT NULL DEFAULT 1,
    task_type        smallint     NOT NULL DEFAULT 1,
    operation_reason smallint     NOT NULL DEFAULT 0,
    client_info      varchar(128) NULL     DEFAULT NULL,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_retry_task_01 ON sj_retry_task (namespace_id, group_name, scene_name);
CREATE INDEX IF NOT EXISTS idx_sj_retry_task_02 ON sj_retry_task (task_status);
CREATE INDEX IF NOT EXISTS idx_sj_retry_task_03 ON sj_retry_task (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_retry_task_04 ON sj_retry_task (retry_id);

COMMENT ON TABLE sj_retry_task IS '重试任务表';
COMMENT ON COLUMN sj_retry_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_retry_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_task.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task.id IS '主键';
COMMENT ON COLUMN sj_retry_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_retry_task.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_task.task_status IS '重试状态';
COMMENT ON COLUMN sj_retry_task.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_task.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_retry_task_log_message
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry_task_log_message (
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64) NOT NULL,
    retry_id      bigint      NOT NULL,
    retry_task_id bigint      NOT NULL,
    message       text        NOT NULL,
    log_num       int         NOT NULL DEFAULT 1,
    real_time     bigint      NOT NULL DEFAULT 0,
    create_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_retry_task_log_message_01 ON sj_retry_task_log_message (namespace_id, group_name, retry_task_id);
CREATE INDEX IF NOT EXISTS idx_sj_retry_task_log_message_02 ON sj_retry_task_log_message (create_dt);

COMMENT ON TABLE sj_retry_task_log_message IS '任务调度日志信息记录表';
COMMENT ON COLUMN sj_retry_task_log_message.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_log_message.id IS '主键';
COMMENT ON COLUMN sj_retry_task_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_retry_task_log_message.message IS '异常信息';
COMMENT ON COLUMN sj_retry_task_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_retry_task_log_message.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task_log_message.retry_task_id IS '重试任务Id';

-- ======================================================================
-- 表: sj_retry_scene_config
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry_scene_config (
    id                  bigserial PRIMARY KEY,
    namespace_id        varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    scene_name          varchar(64)  NOT NULL,
    group_name          varchar(64)  NOT NULL,
    scene_status        smallint     NOT NULL DEFAULT 0,
    max_retry_count     int          NOT NULL DEFAULT 5,
    back_off            smallint     NOT NULL DEFAULT 1,
    trigger_interval    varchar(16)  NOT NULL DEFAULT '',
    notify_ids          varchar(128) NOT NULL DEFAULT '',
    deadline_request    bigint       NOT NULL DEFAULT 60000,
    executor_timeout    int          NOT NULL DEFAULT 5,
    route_key           smallint     NOT NULL DEFAULT 4,
    block_strategy      smallint     NOT NULL DEFAULT 1,
    cb_status           smallint     NOT NULL DEFAULT 0,
    cb_trigger_type     smallint     NOT NULL DEFAULT 1,
    cb_max_count        int          NOT NULL DEFAULT 16,
    cb_trigger_interval varchar(16)  NOT NULL DEFAULT '',
    owner_id            bigint       NULL     DEFAULT NULL,
    labels              varchar(512) NULL     DEFAULT '',
    description         varchar(256) NOT NULL DEFAULT '',
    create_dt           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_retry_scene_config_01 ON sj_retry_scene_config (namespace_id, group_name, scene_name);

COMMENT ON TABLE sj_retry_scene_config IS '场景配置';
COMMENT ON COLUMN sj_retry_scene_config.back_off IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_retry_scene_config.cb_max_count IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.cb_status IS '回调状态 0、不开启 1、开启';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_interval IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_type IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_scene_config.deadline_request IS 'Deadline Request 调用链超时 单位毫秒';
COMMENT ON COLUMN sj_retry_scene_config.description IS '描述';
COMMENT ON COLUMN sj_retry_scene_config.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_retry_scene_config.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_scene_config.id IS '主键';
COMMENT ON COLUMN sj_retry_scene_config.labels IS '标签';
COMMENT ON COLUMN sj_retry_scene_config.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN sj_retry_scene_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_scene_config.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_retry_scene_config.owner_id IS '负责人id';
COMMENT ON COLUMN sj_retry_scene_config.route_key IS '路由策略';
COMMENT ON COLUMN sj_retry_scene_config.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_scene_config.scene_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_retry_scene_config.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_retry_scene_config.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_server_node
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_server_node (
    id           bigserial PRIMARY KEY,
    namespace_id varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   varchar(64)  NOT NULL,
    host_id      varchar(64)  NOT NULL,
    host_ip      varchar(64)  NOT NULL,
    host_port    int          NOT NULL,
    expire_at    timestamp    NOT NULL,
    node_type    smallint     NOT NULL,
    ext_attrs    varchar(256) NULL     DEFAULT '',
    labels       varchar(512) NULL     DEFAULT '',
    create_dt    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_server_node_01 ON sj_server_node (host_id, host_ip);
CREATE INDEX IF NOT EXISTS idx_sj_server_node_01 ON sj_server_node (namespace_id, group_name);
CREATE INDEX IF NOT EXISTS idx_sj_server_node_02 ON sj_server_node (expire_at, node_type);

COMMENT ON TABLE sj_server_node IS '服务器节点';
COMMENT ON COLUMN sj_server_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_server_node.expire_at IS '过期时间';
COMMENT ON COLUMN sj_server_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_server_node.group_name IS '组名称';
COMMENT ON COLUMN sj_server_node.host_id IS '主机id';
COMMENT ON COLUMN sj_server_node.host_ip IS '机器ip';
COMMENT ON COLUMN sj_server_node.host_port IS '机器端口';
COMMENT ON COLUMN sj_server_node.id IS '主键';
COMMENT ON COLUMN sj_server_node.labels IS '标签';
COMMENT ON COLUMN sj_server_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_server_node.node_type IS '节点类型 1、客户端 2、是服务端';
COMMENT ON COLUMN sj_server_node.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_distributed_lock
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_distributed_lock (
    name       varchar(64)  NOT NULL PRIMARY KEY,
    lock_until timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_at  timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  varchar(255) NOT NULL,
    create_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sj_distributed_lock IS '锁定表';
COMMENT ON COLUMN sj_distributed_lock.create_dt IS '创建时间';
COMMENT ON COLUMN sj_distributed_lock.lock_until IS '锁定时长';
COMMENT ON COLUMN sj_distributed_lock.locked_at IS '锁定时间';
COMMENT ON COLUMN sj_distributed_lock.locked_by IS '锁定者';
COMMENT ON COLUMN sj_distributed_lock.name IS '锁名称';
COMMENT ON COLUMN sj_distributed_lock.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_system_user_permission
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_system_user_permission (
    id             bigserial PRIMARY KEY,
    group_name     varchar(64) NOT NULL,
    namespace_id   varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    system_user_id bigint      NOT NULL,
    create_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_system_user_permission_01 ON sj_system_user_permission (namespace_id, group_name, system_user_id);

COMMENT ON TABLE sj_system_user_permission IS '系统用户权限表';
COMMENT ON COLUMN sj_system_user_permission.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user_permission.group_name IS '组名称';
COMMENT ON COLUMN sj_system_user_permission.id IS '主键';
COMMENT ON COLUMN sj_system_user_permission.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_system_user_permission.system_user_id IS '系统用户id';
COMMENT ON COLUMN sj_system_user_permission.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_job
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job (
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    job_name         varchar(64)  NOT NULL,
    args_str         text         NULL     DEFAULT NULL,
    args_type        smallint     NOT NULL DEFAULT 1,
    next_trigger_at  bigint       NOT NULL,
    job_status       smallint     NOT NULL DEFAULT 1,
    task_type        smallint     NOT NULL DEFAULT 1,
    route_key        smallint     NOT NULL DEFAULT 4,
    executor_type    smallint     NOT NULL DEFAULT 1,
    executor_info    varchar(255) NULL     DEFAULT NULL,
    trigger_type     smallint     NOT NULL,
    trigger_interval varchar(255) NOT NULL,
    block_strategy   smallint     NOT NULL DEFAULT 1,
    executor_timeout int          NOT NULL DEFAULT 0,
    max_retry_times  int          NOT NULL DEFAULT 0,
    parallel_num     int          NOT NULL DEFAULT 1,
    retry_interval   int          NOT NULL DEFAULT 0,
    bucket_index     int          NOT NULL DEFAULT 0,
    resident         smallint     NOT NULL DEFAULT 0,
    notify_ids       varchar(128) NOT NULL DEFAULT '',
    owner_id         bigint       NULL     DEFAULT NULL,
    labels           varchar(512) NULL     DEFAULT '',
    description      varchar(256) NOT NULL DEFAULT '',
    ext_attrs        varchar(256) NULL     DEFAULT '',
    deleted          smallint     NOT NULL DEFAULT 0,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_job_01 ON sj_job (namespace_id, group_name);
CREATE INDEX IF NOT EXISTS idx_sj_job_02 ON sj_job (job_status, bucket_index);
CREATE INDEX IF NOT EXISTS idx_sj_job_03 ON sj_job (create_dt);

COMMENT ON TABLE sj_job IS '任务信息';
COMMENT ON COLUMN sj_job.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复';
COMMENT ON COLUMN sj_job.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_job.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job.description IS '描述';
COMMENT ON COLUMN sj_job.executor_info IS '执行器名称';
COMMENT ON COLUMN sj_job.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_job.executor_type IS '执行器类型';
COMMENT ON COLUMN sj_job.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job.group_name IS '组名称';
COMMENT ON COLUMN sj_job.id IS '主键';
COMMENT ON COLUMN sj_job.job_name IS '名称';
COMMENT ON COLUMN sj_job.job_status IS '任务状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_job.labels IS '标签';
COMMENT ON COLUMN sj_job.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN sj_job.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_job.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_job.owner_id IS '负责人id';
COMMENT ON COLUMN sj_job.parallel_num IS '并行数';
COMMENT ON COLUMN sj_job.resident IS '是否是常驻任务';
COMMENT ON COLUMN sj_job.retry_interval IS '重试间隔 ( s)';
COMMENT ON COLUMN sj_job.route_key IS '路由策略';
COMMENT ON COLUMN sj_job.task_type IS '任务类型 1、集群 2、广播 3、切片';
COMMENT ON COLUMN sj_job.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_job.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_job.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_job_log_message
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job_log_message (
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64)  NOT NULL,
    job_id        bigint       NOT NULL,
    task_batch_id bigint       NOT NULL,
    task_id       bigint       NOT NULL,
    message       text         NOT NULL,
    log_num       int          NOT NULL DEFAULT 1,
    real_time     bigint       NOT NULL DEFAULT 0,
    ext_attrs     varchar(256) NULL     DEFAULT '',
    create_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_job_log_message_01 ON sj_job_log_message (task_batch_id, task_id);
CREATE INDEX IF NOT EXISTS idx_sj_job_log_message_02 ON sj_job_log_message (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_job_log_message_03 ON sj_job_log_message (namespace_id, group_name);

COMMENT ON TABLE sj_job_log_message IS '调度日志';
COMMENT ON COLUMN sj_job_log_message.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_log_message.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_job_log_message.id IS '主键';
COMMENT ON COLUMN sj_job_log_message.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_job_log_message.message IS '调度信息';
COMMENT ON COLUMN sj_job_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_job_log_message.task_batch_id IS '任务批次id';
COMMENT ON COLUMN sj_job_log_message.task_id IS '调度任务id';

-- ======================================================================
-- 表: sj_job_task
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job_task (
    id             bigserial PRIMARY KEY,
    namespace_id   varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     varchar(64)  NOT NULL,
    job_id         bigint       NOT NULL,
    task_batch_id  bigint       NOT NULL,
    parent_id      bigint       NOT NULL DEFAULT 0,
    task_status    smallint     NOT NULL DEFAULT 0,
    retry_count    int          NOT NULL DEFAULT 0,
    mr_stage       smallint     NULL     DEFAULT NULL,
    leaf           smallint     NOT NULL DEFAULT '1',
    task_name      varchar(255) NOT NULL DEFAULT '',
    client_info    varchar(128) NULL     DEFAULT NULL,
    wf_context     text         NULL     DEFAULT NULL,
    result_message text         NOT NULL,
    args_str       text         NULL     DEFAULT NULL,
    args_type      smallint     NOT NULL DEFAULT 1,
    ext_attrs      varchar(256) NULL     DEFAULT '',
    create_dt      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_job_task_01 ON sj_job_task (task_batch_id, task_status);
CREATE INDEX IF NOT EXISTS idx_sj_job_task_02 ON sj_job_task (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_job_task_03 ON sj_job_task (namespace_id, group_name);

COMMENT ON TABLE sj_job_task IS '任务实例';
COMMENT ON COLUMN sj_job_task.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job_task.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_job_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task.id IS '主键';
COMMENT ON COLUMN sj_job_task.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_task.leaf IS '叶子节点';
COMMENT ON COLUMN sj_job_task.mr_stage IS '动态分片所处阶段 1:map 2:reduce 3:mergeReduce';
COMMENT ON COLUMN sj_job_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task.parent_id IS '父执行器id';
COMMENT ON COLUMN sj_job_task.result_message IS '执行结果';
COMMENT ON COLUMN sj_job_task.retry_count IS '重试次数';
COMMENT ON COLUMN sj_job_task.task_batch_id IS '调度任务id';
COMMENT ON COLUMN sj_job_task.task_name IS '任务名称';
COMMENT ON COLUMN sj_job_task.task_status IS '执行的状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task.update_dt IS '修改时间';
COMMENT ON COLUMN sj_job_task.wf_context IS '工作流全局上下文';

-- ======================================================================
-- 表: sj_job_task_batch
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job_task_batch (
    id                      bigserial PRIMARY KEY,
    namespace_id            varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name              varchar(64)  NOT NULL,
    job_id                  bigint       NOT NULL,
    workflow_node_id        bigint       NOT NULL DEFAULT 0,
    parent_workflow_node_id bigint       NOT NULL DEFAULT 0,
    workflow_task_batch_id  bigint       NOT NULL DEFAULT 0,
    task_batch_status       smallint     NOT NULL DEFAULT 0,
    operation_reason        smallint     NOT NULL DEFAULT 0,
    execution_at            bigint       NOT NULL DEFAULT 0,
    system_task_type        smallint     NOT NULL DEFAULT 3,
    parent_id               varchar(64)  NOT NULL DEFAULT '',
    ext_attrs               varchar(256) NULL     DEFAULT '',
    deleted                 smallint     NOT NULL DEFAULT 0,
    create_dt               timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt               timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_job_task_batch_01 ON sj_job_task_batch (job_id, task_batch_status);
CREATE INDEX IF NOT EXISTS idx_sj_job_task_batch_02 ON sj_job_task_batch (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_job_task_batch_03 ON sj_job_task_batch (namespace_id, group_name);
CREATE INDEX IF NOT EXISTS idx_sj_job_task_batch_04 ON sj_job_task_batch (workflow_task_batch_id, workflow_node_id);

COMMENT ON TABLE sj_job_task_batch IS '任务批次';
COMMENT ON COLUMN sj_job_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_job_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task_batch.id IS '主键';
COMMENT ON COLUMN sj_job_task_batch.job_id IS '任务id';
COMMENT ON COLUMN sj_job_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_job_task_batch.parent_id IS '父节点';
COMMENT ON COLUMN sj_job_task_batch.parent_workflow_node_id IS '工作流任务父批次id';
COMMENT ON COLUMN sj_job_task_batch.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task_batch.update_dt IS '修改时间';
COMMENT ON COLUMN sj_job_task_batch.workflow_node_id IS '工作流节点id';
COMMENT ON COLUMN sj_job_task_batch.workflow_task_batch_id IS '工作流任务批次id';

-- ======================================================================
-- 表: sj_job_summary
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job_summary (
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL DEFAULT '',
    business_id      bigint       NOT NULL,
    system_task_type smallint     NOT NULL DEFAULT 3,
    trigger_at       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success_num      int          NOT NULL DEFAULT 0,
    fail_num         int          NOT NULL DEFAULT 0,
    fail_reason      varchar(512) NOT NULL DEFAULT '',
    stop_num         int          NOT NULL DEFAULT 0,
    stop_reason      varchar(512) NOT NULL DEFAULT '',
    cancel_num       int          NOT NULL DEFAULT 0,
    cancel_reason    varchar(512) NOT NULL DEFAULT '',
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_job_summary_01 ON sj_job_summary (trigger_at, system_task_type, business_id);
CREATE INDEX IF NOT EXISTS idx_sj_job_summary_01 ON sj_job_summary (namespace_id, group_name, business_id);

COMMENT ON TABLE sj_job_summary IS 'DashBoard_Job';
COMMENT ON COLUMN sj_job_summary.business_id IS '业务id  ( job_id或workflow_id)';
COMMENT ON COLUMN sj_job_summary.cancel_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.cancel_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_summary.fail_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_job_summary.id IS '主键';
COMMENT ON COLUMN sj_job_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_summary.stop_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.stop_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.success_num IS '执行成功-日志数量';
COMMENT ON COLUMN sj_job_summary.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_job_summary.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_retry_summary
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_retry_summary (
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64) NOT NULL DEFAULT '',
    scene_name    varchar(50) NOT NULL DEFAULT '',
    trigger_at    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    running_num   int         NOT NULL DEFAULT 0,
    finish_num    int         NOT NULL DEFAULT 0,
    max_count_num int         NOT NULL DEFAULT 0,
    suspend_num   int         NOT NULL DEFAULT 0,
    create_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sj_retry_summary_01 ON sj_retry_summary (namespace_id, group_name, scene_name, trigger_at);
CREATE INDEX IF NOT EXISTS idx_sj_retry_summary_01 ON sj_retry_summary (trigger_at);

COMMENT ON TABLE sj_retry_summary IS 'DashBoard_Retry';
COMMENT ON COLUMN sj_retry_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_summary.finish_num IS '重试完成-日志数量';
COMMENT ON COLUMN sj_retry_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_summary.id IS '主键';
COMMENT ON COLUMN sj_retry_summary.max_count_num IS '重试到达最大次数-日志数量';
COMMENT ON COLUMN sj_retry_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_summary.running_num IS '重试中-日志数量';
COMMENT ON COLUMN sj_retry_summary.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_summary.suspend_num IS '暂停重试-日志数量';
COMMENT ON COLUMN sj_retry_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_retry_summary.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_workflow
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_workflow (
    id               bigserial PRIMARY KEY,
    workflow_name    varchar(64)  NOT NULL,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    workflow_status  smallint     NOT NULL DEFAULT 1,
    trigger_type     smallint     NOT NULL,
    trigger_interval varchar(255) NOT NULL,
    next_trigger_at  bigint       NOT NULL,
    block_strategy   smallint     NOT NULL DEFAULT 1,
    executor_timeout int          NOT NULL DEFAULT 0,
    description      varchar(256) NOT NULL DEFAULT '',
    flow_info        text         NULL     DEFAULT NULL,
    wf_context       text         NULL     DEFAULT NULL,
    notify_ids       varchar(128) NOT NULL DEFAULT '',
    bucket_index     int          NOT NULL DEFAULT 0,
    version          int          NOT NULL,
    owner_id         bigint       NULL     DEFAULT NULL,
    ext_attrs        varchar(256) NULL     DEFAULT '',
    deleted          smallint     NOT NULL DEFAULT 0,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_workflow_01 ON sj_workflow (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_workflow_02 ON sj_workflow (namespace_id, group_name);

COMMENT ON TABLE sj_workflow IS '工作流';
COMMENT ON COLUMN sj_workflow.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_workflow.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_workflow.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow.description IS '描述';
COMMENT ON COLUMN sj_workflow.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_workflow.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow.id IS '主键';
COMMENT ON COLUMN sj_workflow.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_workflow.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_workflow.owner_id IS '负责人id';
COMMENT ON COLUMN sj_workflow.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_workflow.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_workflow.update_dt IS '修改时间';
COMMENT ON COLUMN sj_workflow.version IS '版本号';
COMMENT ON COLUMN sj_workflow.wf_context IS '上下文';
COMMENT ON COLUMN sj_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN sj_workflow.workflow_status IS '工作流状态 0、关闭、1、开启';

-- ======================================================================
-- 表: sj_workflow_node
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_workflow_node (
    id                   bigserial PRIMARY KEY,
    namespace_id         varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    node_name            varchar(64)  NOT NULL,
    group_name           varchar(64)  NOT NULL,
    job_id               bigint       NOT NULL,
    workflow_id          bigint       NOT NULL,
    node_type            smallint     NOT NULL DEFAULT 1,
    expression_type      smallint     NOT NULL DEFAULT 0,
    fail_strategy        smallint     NOT NULL DEFAULT 1,
    workflow_node_status smallint     NOT NULL DEFAULT 1,
    priority_level       int          NOT NULL DEFAULT 1,
    node_info            text         NULL     DEFAULT NULL,
    version              int          NOT NULL,
    ext_attrs            varchar(256) NULL     DEFAULT '',
    deleted              smallint     NOT NULL DEFAULT 0,
    create_dt            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt            timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_workflow_node_01 ON sj_workflow_node (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_workflow_node_02 ON sj_workflow_node (namespace_id, group_name);

COMMENT ON TABLE sj_workflow_node IS '工作流节点';
COMMENT ON COLUMN sj_workflow_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_node.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_node.expression_type IS '1、SpEl、2、Aviator 3、QL';
COMMENT ON COLUMN sj_workflow_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_node.fail_strategy IS '失败策略 1、跳过 2、阻塞';
COMMENT ON COLUMN sj_workflow_node.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_node.id IS '主键';
COMMENT ON COLUMN sj_workflow_node.job_id IS '任务信息id';
COMMENT ON COLUMN sj_workflow_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_node.node_info IS '节点信息 ';
COMMENT ON COLUMN sj_workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN sj_workflow_node.node_type IS '1、任务节点 2、条件节点';
COMMENT ON COLUMN sj_workflow_node.priority_level IS '优先级';
COMMENT ON COLUMN sj_workflow_node.update_dt IS '修改时间';
COMMENT ON COLUMN sj_workflow_node.version IS '版本号';
COMMENT ON COLUMN sj_workflow_node.workflow_id IS '工作流ID';
COMMENT ON COLUMN sj_workflow_node.workflow_node_status IS '工作流节点状态 0、关闭、1、开启';

-- ======================================================================
-- 表: sj_workflow_task_batch
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_workflow_task_batch (
    id                bigserial PRIMARY KEY,
    namespace_id      varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        varchar(64)  NOT NULL,
    workflow_id       bigint       NOT NULL,
    task_batch_status smallint     NOT NULL DEFAULT 0,
    operation_reason  smallint     NOT NULL DEFAULT 0,
    flow_info         text         NULL     DEFAULT NULL,
    wf_context        text         NULL     DEFAULT NULL,
    execution_at      bigint       NOT NULL DEFAULT 0,
    ext_attrs         varchar(256) NULL     DEFAULT '',
    version           int          NOT NULL DEFAULT 1,
    deleted           smallint     NOT NULL DEFAULT 0,
    create_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_workflow_task_batch_01 ON sj_workflow_task_batch (workflow_id, task_batch_status);
CREATE INDEX IF NOT EXISTS idx_sj_workflow_task_batch_02 ON sj_workflow_task_batch (create_dt);
CREATE INDEX IF NOT EXISTS idx_sj_workflow_task_batch_03 ON sj_workflow_task_batch (namespace_id, group_name);

COMMENT ON TABLE sj_workflow_task_batch IS '工作流批次';
COMMENT ON COLUMN sj_workflow_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_workflow_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_task_batch.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_task_batch.id IS '主键';
COMMENT ON COLUMN sj_workflow_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_workflow_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_workflow_task_batch.update_dt IS '修改时间';
COMMENT ON COLUMN sj_workflow_task_batch.version IS '版本号';
COMMENT ON COLUMN sj_workflow_task_batch.wf_context IS '全局上下文';
COMMENT ON COLUMN sj_workflow_task_batch.workflow_id IS '工作流任务id';

-- ======================================================================
-- 表: sj_job_executor
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_job_executor (
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64)  NOT NULL,
    executor_info varchar(256) NOT NULL,
    executor_type varchar(3)   NOT NULL,
    create_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sj_job_executor_01 ON sj_job_executor (namespace_id, group_name);
CREATE INDEX IF NOT EXISTS idx_sj_job_executor_02 ON sj_job_executor (create_dt);

COMMENT ON TABLE sj_job_executor IS '任务执行器信息';
COMMENT ON COLUMN sj_job_executor.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_executor.executor_info IS '任务执行器名称';
COMMENT ON COLUMN sj_job_executor.executor_type IS '1:java 2:python 3:go';
COMMENT ON COLUMN sj_job_executor.group_name IS '组名称';
COMMENT ON COLUMN sj_job_executor.id IS '主键';
COMMENT ON COLUMN sj_job_executor.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_executor.update_dt IS '修改时间';

-- ======================================================================
-- 表: sj_system_user
-- ======================================================================
CREATE TABLE IF NOT EXISTS sj_system_user (
    id        bigserial PRIMARY KEY,
    username  varchar(64)  NOT NULL,
    password  varchar(128) NOT NULL,
    role      smallint     NOT NULL DEFAULT 0,
    create_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sj_system_user IS '系统用户表';
COMMENT ON COLUMN sj_system_user.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user.id IS '主键';
COMMENT ON COLUMN sj_system_user.password IS '密码';
COMMENT ON COLUMN sj_system_user.role IS '角色：1-普通用户、2-管理员';
COMMENT ON COLUMN sj_system_user.update_dt IS '修改时间';
COMMENT ON COLUMN sj_system_user.username IS '账号';

-- ======================================================================
-- 表: km_model_provider
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_model_provider (
    provider_id     BIGINT          NOT NULL,
    provider_name   VARCHAR(64)     NOT NULL,
    provider_key    VARCHAR(64)     NOT NULL,
    provider_type   CHAR(1)         DEFAULT '1',
    default_endpoint VARCHAR(255)   DEFAULT '',
    site_url        VARCHAR(255)    DEFAULT '',
    icon_url        VARCHAR(500)    DEFAULT '',
    config_schema   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    sort            INTEGER         DEFAULT 0,
    models          JSONB           DEFAULT NULL,
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (provider_id),
    CONSTRAINT uk_provider_key UNIQUE (provider_key)
);

COMMENT ON TABLE km_model_provider IS '模型供应商表';
COMMENT ON COLUMN km_model_provider.config_schema IS '配置参数定义';
COMMENT ON COLUMN km_model_provider.default_endpoint IS '默认API地址';
COMMENT ON COLUMN km_model_provider.icon_url IS '图标URL';
COMMENT ON COLUMN km_model_provider.models IS '支持的模型标识(JSON)';
COMMENT ON COLUMN km_model_provider.provider_id IS '供应商ID';
COMMENT ON COLUMN km_model_provider.provider_key IS '供应商标识(openai/ollama)';
COMMENT ON COLUMN km_model_provider.provider_name IS '供应商名称';
COMMENT ON COLUMN km_model_provider.provider_type IS '供应商类型（1公用 2本地）';
COMMENT ON COLUMN km_model_provider.site_url IS '官网URL';
COMMENT ON COLUMN km_model_provider.sort IS '排序';
COMMENT ON COLUMN km_model_provider.status IS '状态（0正常 1停用）';

-- ======================================================================
-- 表: km_model
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_model (
    model_id        BIGINT          NOT NULL,
    provider_id     BIGINT          DEFAULT NULL,
    model_name      VARCHAR(64)     NOT NULL,
    model_type      CHAR(1)         NOT NULL,
    model_key       VARCHAR(100)    NOT NULL,
    api_key         VARCHAR(255)    DEFAULT '',
    api_base        VARCHAR(255)    DEFAULT '',
    config          JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    is_builtin      CHAR(1)         DEFAULT 'N',
    model_source    CHAR(1)         DEFAULT '1',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    is_default      SMALLINT        DEFAULT 0,
    remark          VARCHAR(500)    DEFAULT NULL,
    abilities       jsonb           DEFAULT NULL,
    PRIMARY KEY (model_id)
);

COMMENT ON COLUMN km_model.abilities IS '多模态能力标签 (JSON数组,如 ["vision", "audio"])';
COMMENT ON TABLE km_model IS 'AI模型配置表';
COMMENT ON COLUMN km_model.api_base IS 'API Base地址';
COMMENT ON COLUMN km_model.api_key IS 'API Key';
COMMENT ON COLUMN km_model.config IS '配置参数';
COMMENT ON COLUMN km_model.create_by IS '创建人';
COMMENT ON COLUMN km_model.create_dept IS '创建部门';
COMMENT ON COLUMN km_model.create_time IS '创建时间';
COMMENT ON COLUMN km_model.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_model.is_builtin IS '是否为系统内置模型(N-否 Y-是)';
COMMENT ON COLUMN km_model.is_default IS '是否为系统默认使用模型(0-否 1-是)';
COMMENT ON COLUMN km_model.model_key IS '模型标识';
COMMENT ON COLUMN km_model.model_name IS '模型名称';
COMMENT ON COLUMN km_model.model_source IS '模型来源(1公有 2本地)';
COMMENT ON COLUMN km_model.model_type IS '模型类型:1-语言模型 2-视觉模型 3-语音模型 4-混合模型';
COMMENT ON COLUMN km_model.remark IS '备注';
COMMENT ON COLUMN km_model.update_by IS '更新人';
COMMENT ON COLUMN km_model.update_time IS '更新时间';

-- ======================================================================
-- 表: km_knowledge_base
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT,
    permission_level VARCHAR(50) DEFAULT 'PRIVATE',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    embedding_model_id BIGINT
);

COMMENT ON TABLE km_knowledge_base IS '知识库主表';
COMMENT ON COLUMN km_knowledge_base.create_by IS '创建人';
COMMENT ON COLUMN km_knowledge_base.create_dept IS '创建部门';
COMMENT ON COLUMN km_knowledge_base.create_time IS '创建时间';
COMMENT ON COLUMN km_knowledge_base.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN km_knowledge_base.embedding_model_id IS '绑定的向量化模型ID';
COMMENT ON COLUMN km_knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN km_knowledge_base.owner_id IS '知识库所有者';
COMMENT ON COLUMN km_knowledge_base.permission_level IS '知识库权限(PRIVATE, TEAM, PUBLIC)';
COMMENT ON COLUMN km_knowledge_base.status IS '知识库状态(ACTIVE, INACTIVE)';
COMMENT ON COLUMN km_knowledge_base.update_by IS '更新人';
COMMENT ON COLUMN km_knowledge_base.update_time IS '更新时间';

-- ======================================================================
-- 表: km_dataset
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_dataset (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    config JSONB,
    is_system Boolean DEFAULT 'false',
    allowed_file_types VARCHAR(255),
    process_type VARCHAR(50) DEFAULT 'GENERIC_FILE',
    source_type VARCHAR(50) DEFAULT 'FILE',
    min_chunk_size INT DEFAULT 200,
    max_chunk_size INT DEFAULT 500,
    chunk_overlap INT DEFAULT 50,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    child_chunk_size INTEGER NULL,
    child_chunk_overlap INTEGER NULL
);

CREATE INDEX IF NOT EXISTS idx_dataset_kb_id ON km_dataset (kb_id);

COMMENT ON TABLE km_dataset IS '数据集表';
COMMENT ON COLUMN km_dataset.allowed_file_types IS '支持的文件格式(逗号分隔,*表示全部)';
COMMENT ON COLUMN km_dataset.child_chunk_overlap IS '子块重叠大小(字符数)，NULL 表示使用系统默认值';
COMMENT ON COLUMN km_dataset.child_chunk_size IS '子块大小(字符数)，NULL 表示使用系统默认值';
COMMENT ON COLUMN km_dataset.chunk_overlap IS '分块重叠大小';
COMMENT ON COLUMN km_dataset.config IS '配置参数';
COMMENT ON COLUMN km_dataset.create_by IS '创建人';
COMMENT ON COLUMN km_dataset.create_dept IS '创建部门';
COMMENT ON COLUMN km_dataset.create_time IS '创建时间';
COMMENT ON COLUMN km_dataset.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_dataset.is_system IS '是否为系统数据集';
COMMENT ON COLUMN km_dataset.kb_id IS '知识库ID';
COMMENT ON COLUMN km_dataset.max_chunk_size IS '最大分块大小';
COMMENT ON COLUMN km_dataset.min_chunk_size IS '最小分块大小';
COMMENT ON COLUMN km_dataset.name IS '数据集名称';
COMMENT ON COLUMN km_dataset.process_type IS '处理类型:GENERIC_FILE/QA_PAIR/ONLINE_DOC/WEB_LINK';
COMMENT ON COLUMN km_dataset.source_type IS '数据源类型:FILE/WEB/MANUAL';
COMMENT ON COLUMN km_dataset.update_by IS '更新人';
COMMENT ON COLUMN km_dataset.update_time IS '更新时间';

-- ======================================================================
-- 表: km_document
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_document (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    kb_id BIGINT,
    original_filename VARCHAR(512),
    file_path VARCHAR(1024),
    oss_id BIGINT,
    file_type VARCHAR(50),
    file_size BIGINT,
    error_msg TEXT,
    token_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    hash_code VARCHAR(128),
    store_type INTEGER DEFAULT 1,
    enabled INTEGER DEFAULT 1,
    embedding_status INTEGER DEFAULT 0,
    question_status INTEGER DEFAULT 0,
    status_meta JSONB,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    title VARCHAR(500),
    content TEXT,
    url VARCHAR(1000)
);

CREATE INDEX IF NOT EXISTS idx_document_dataset_id ON km_document (dataset_id);
CREATE INDEX IF NOT EXISTS idx_document_kb_id ON km_document (kb_id);

COMMENT ON TABLE km_document IS '文档表';
COMMENT ON COLUMN km_document.chunk_count IS '分块数量';
COMMENT ON COLUMN km_document.content IS '在线文档内容(富文本HTML)';
COMMENT ON COLUMN km_document.create_by IS '创建人';
COMMENT ON COLUMN km_document.create_dept IS '创建部门';
COMMENT ON COLUMN km_document.create_time IS '创建时间';
COMMENT ON COLUMN km_document.dataset_id IS '数据集ID';
COMMENT ON COLUMN km_document.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_document.embedding_status IS '向量生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document.enabled IS '启用状态(0-禁用, 1-启用)';
COMMENT ON COLUMN km_document.error_msg IS '错误信息';
COMMENT ON COLUMN km_document.file_path IS '文件路径';
COMMENT ON COLUMN km_document.file_size IS '文件大小';
COMMENT ON COLUMN km_document.file_type IS '文件类型-扩展名';
COMMENT ON COLUMN km_document.hash_code IS '文件哈希值';
COMMENT ON COLUMN km_document.kb_id IS '知识库ID';
COMMENT ON COLUMN km_document.original_filename IS '原始文件名';
COMMENT ON COLUMN km_document.oss_id IS 'OSS文件ID';
COMMENT ON COLUMN km_document.question_status IS '问答对生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document.status_meta IS '状态元数据';
COMMENT ON COLUMN km_document.store_type IS '存储类型(1-OSS, 2-本地文件)';
COMMENT ON COLUMN km_document.title IS '文档标题(用于向量化)';
COMMENT ON COLUMN km_document.token_count IS 'Token数量';
COMMENT ON COLUMN km_document.update_by IS '更新人';
COMMENT ON COLUMN km_document.update_time IS '更新时间';
COMMENT ON COLUMN km_document.url IS '网页链接URL';

-- ======================================================================
-- 表: km_document_chunk
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    kb_id BIGINT,
    title VARCHAR(500),
    content TEXT,
    metadata JSONB,
    parent_chain TEXT,
    enabled INTEGER DEFAULT 1,
    embedding_status INTEGER DEFAULT 0,
    question_status INTEGER DEFAULT 0,
    status_meta JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chunk_type SMALLINT NOT NULL DEFAULT 2,
    parent_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_chunk_document_id ON km_document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_kb_id ON km_document_chunk (kb_id);
CREATE INDEX IF NOT EXISTS idx_km_document_chunk_parent_id ON km_document_chunk (parent_id);

COMMENT ON TABLE km_document_chunk IS '文档分块表';
COMMENT ON COLUMN km_document_chunk.chunk_type IS '块类型: 0=PARENT(父块), 1=CHILD(子块), 2=STANDALONE(独立块)';
COMMENT ON COLUMN km_document_chunk.content IS '分块内容';
COMMENT ON COLUMN km_document_chunk.create_time IS '创建时间';
COMMENT ON COLUMN km_document_chunk.document_id IS '文档ID';
COMMENT ON COLUMN km_document_chunk.embedding_status IS '向量生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document_chunk.enabled IS '启用状态(0-禁用, 1-启用)';
COMMENT ON COLUMN km_document_chunk.kb_id IS '知识库ID';
COMMENT ON COLUMN km_document_chunk.metadata IS '元数据';
COMMENT ON COLUMN km_document_chunk.parent_chain IS '父级标题链路';
COMMENT ON COLUMN km_document_chunk.parent_id IS '父块ID，子块指向其所属父块，父块和独立块为 NULL';
COMMENT ON COLUMN km_document_chunk.question_status IS '问答对生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document_chunk.status_meta IS '状态元数据';
COMMENT ON COLUMN km_document_chunk.title IS '分块标题';

-- ======================================================================
-- 表: km_question
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_question (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    hit_num INT DEFAULT 0,
    source_type VARCHAR(20) DEFAULT 'IMPORT',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);

CREATE INDEX IF NOT EXISTS idx_question_kb_id ON km_question (kb_id);

COMMENT ON TABLE km_question IS '问题表';
COMMENT ON COLUMN km_question.content IS '问题内容';
COMMENT ON COLUMN km_question.create_by IS '创建人';
COMMENT ON COLUMN km_question.create_dept IS '创建部门';
COMMENT ON COLUMN km_question.create_time IS '创建时间';
COMMENT ON COLUMN km_question.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_question.hit_num IS '命中次数';
COMMENT ON COLUMN km_question.kb_id IS '知识库ID';
COMMENT ON COLUMN km_question.source_type IS '来源类型(IMPORT-导入, GENERATED-生成)';
COMMENT ON COLUMN km_question.update_by IS '更新人';
COMMENT ON COLUMN km_question.update_time IS '更新时间';

-- ======================================================================
-- 表: km_question_chunk_map
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_question_chunk_map (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    CONSTRAINT uk_question_chunk UNIQUE (question_id, chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_qcm_question_id ON km_question_chunk_map (question_id);
CREATE INDEX IF NOT EXISTS idx_qcm_chunk_id ON km_question_chunk_map (chunk_id);

COMMENT ON TABLE km_question_chunk_map IS '问题与分块关联表';
COMMENT ON COLUMN km_question_chunk_map.chunk_id IS '分块ID';
COMMENT ON COLUMN km_question_chunk_map.question_id IS '问题ID';

-- ======================================================================
-- 表: km_embedding
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_embedding (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_type SMALLINT NOT NULL,
    embedding vector(512),
    text_content TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_embedding_kb_id ON km_embedding (kb_id);
CREATE INDEX IF NOT EXISTS idx_embedding_source ON km_embedding (source_id, source_type);

COMMENT ON TABLE km_embedding IS '统一向量存储表';
COMMENT ON COLUMN km_embedding.create_time IS '创建时间';
COMMENT ON COLUMN km_embedding.embedding IS '向量';
COMMENT ON COLUMN km_embedding.kb_id IS '知识库ID';
COMMENT ON COLUMN km_embedding.source_id IS '来源ID';
COMMENT ON COLUMN km_embedding.source_type IS '来源类型(0=QUESTION, 1=CONTENT, 2=TITLE)';
COMMENT ON COLUMN km_embedding.text_content IS '文本内容';

-- ======================================================================
-- 表: km_temp_file
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_temp_file (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT,
    original_filename VARCHAR(500) NOT NULL,
    file_extension VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(1000) NOT NULL,
    oss_id BIGINT,
    store_type INT,
    hash_code VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_temp_file_dataset ON km_temp_file (dataset_id);
CREATE INDEX IF NOT EXISTS idx_temp_file_expire ON km_temp_file (expire_time);

COMMENT ON TABLE km_temp_file IS '临时文件表';
COMMENT ON COLUMN km_temp_file.create_time IS '创建时间';
COMMENT ON COLUMN km_temp_file.dataset_id IS '数据集ID';
COMMENT ON COLUMN km_temp_file.expire_time IS '过期时间';
COMMENT ON COLUMN km_temp_file.file_extension IS '文件扩展名';
COMMENT ON COLUMN km_temp_file.file_size IS '文件大小';
COMMENT ON COLUMN km_temp_file.original_filename IS '原始文件名';
COMMENT ON COLUMN km_temp_file.oss_id IS 'OSS文件ID';
COMMENT ON COLUMN km_temp_file.store_type IS '存储类型 (1-OSS, 2-本地)';
COMMENT ON COLUMN km_temp_file.hash_code IS '文件哈希';
COMMENT ON COLUMN km_temp_file.file_path IS '文件路径';

-- ======================================================================
-- 表: km_app
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_app (
    app_id          BIGINT          NOT NULL,
    app_name        VARCHAR(64)     NOT NULL,
    description     VARCHAR(500)    DEFAULT '',
    icon            VARCHAR(255)    DEFAULT '',
    app_type        CHAR(1)         DEFAULT '1',
    status          CHAR(1)         DEFAULT '0',
    prologue        VARCHAR(1000)   DEFAULT '',
    model_setting   JSONB           DEFAULT NULL,
    knowledge_setting JSONB         DEFAULT NULL,
    workflow_config JSONB           DEFAULT NULL,
    graph_data      JSONB           DEFAULT NULL,
    dsl_data        JSONB           DEFAULT NULL,
    parameters      JSONB           DEFAULT NULL,
    source_template_id BIGINT          DEFAULT NULL,
    source_template_scope CHAR(1)      DEFAULT NULL,
    model_id        BIGINT          DEFAULT NULL,
    enable_execution_detail CHAR(1) DEFAULT '0',
    public_access   CHAR(1)         DEFAULT '1',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (app_id)
);

COMMENT ON TABLE km_app IS '应用表';
COMMENT ON COLUMN km_app.app_id IS '应用ID';
COMMENT ON COLUMN km_app.app_name IS '应用名称';
COMMENT ON COLUMN km_app.app_type IS '应用类型（1固定模板 2自定义工作流）';
COMMENT ON COLUMN km_app.create_by IS '创建人';
COMMENT ON COLUMN km_app.create_dept IS '创建部门';
COMMENT ON COLUMN km_app.create_time IS '创建时间';
COMMENT ON COLUMN km_app.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_app.description IS '应用描述';
COMMENT ON COLUMN km_app.dsl_data IS 'DSL数据';
COMMENT ON COLUMN km_app.enable_execution_detail IS '是否启用执行详情（0禁用 1启用）';
COMMENT ON COLUMN km_app.graph_data IS '图数据';
COMMENT ON COLUMN km_app.icon IS '应用图标';
COMMENT ON COLUMN km_app.knowledge_setting IS '知识库配置';
COMMENT ON COLUMN km_app.model_id IS '模型ID';
COMMENT ON COLUMN km_app.model_setting IS '模型配置';
COMMENT ON COLUMN km_app.parameters IS '应用参数配置(全局/接口/会话)';
COMMENT ON COLUMN km_app.prologue IS '应用前置语-开场白';
COMMENT ON COLUMN km_app.public_access IS '公开访问（0关闭 1开启）';
COMMENT ON COLUMN km_app.remark IS '备注';
COMMENT ON COLUMN km_app.source_template_id IS '来源模版ID';
COMMENT ON COLUMN km_app.source_template_scope IS '来源模版类型(0系统/1自建)';
COMMENT ON COLUMN km_app.status IS '应用状态（0禁用 1启用）';
COMMENT ON COLUMN km_app.update_by IS '更新人';
COMMENT ON COLUMN km_app.update_time IS '更新时间';
COMMENT ON COLUMN km_app.workflow_config IS '工作流配置';

-- ======================================================================
-- 表: km_app_knowledge
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_app_knowledge (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    sort            INTEGER         DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_kb UNIQUE (app_id, knowledge_id)
);


-- ======================================================================
-- 表: km_app_version
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_app_version (
    version_id      BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    version         INTEGER         NOT NULL,
    app_snapshot    JSONB           NOT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (version_id)
);

COMMENT ON TABLE km_app_version IS '应用版本表';
COMMENT ON COLUMN km_app_version.app_id IS '应用ID';
COMMENT ON COLUMN km_app_version.app_snapshot IS '应用快照';
COMMENT ON COLUMN km_app_version.create_by IS '创建人';
COMMENT ON COLUMN km_app_version.create_time IS '创建时间';
COMMENT ON COLUMN km_app_version.remark IS '备注';
COMMENT ON COLUMN km_app_version.version IS '版本号';
COMMENT ON COLUMN km_app_version.version_id IS '版本ID';

-- ======================================================================
-- 表: km_app_access_stat
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_app_access_stat (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    access_count    BIGINT          DEFAULT 0,
    last_access_time TIMESTAMP      DEFAULT NULL,
    token_count int8 DEFAULT 0,
    like_count int8 DEFAULT 0,
    dislike_count int8 DEFAULT 0,
    question_count int8 DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_user UNIQUE (app_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_km_app_access_stat_app_id ON km_app_access_stat (app_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_km_app_access_stat_app_user ON km_app_access_stat (app_id, user_id);

COMMENT ON TABLE km_app_access_stat IS '应用访问统计表';
COMMENT ON COLUMN km_app_access_stat.access_count IS '访问次数';
COMMENT ON COLUMN km_app_access_stat.app_id IS '应用ID';
COMMENT ON COLUMN km_app_access_stat.dislike_count IS '点踩次数';
COMMENT ON COLUMN km_app_access_stat.id IS 'ID';
COMMENT ON COLUMN km_app_access_stat.last_access_time IS '最后访问时间';
COMMENT ON COLUMN km_app_access_stat.like_count IS '点赞次数';
COMMENT ON COLUMN km_app_access_stat.question_count IS '提问次数';
COMMENT ON COLUMN km_app_access_stat.token_count IS '消耗的 Token 总数';
COMMENT ON COLUMN km_app_access_stat.user_id IS '用户ID';

-- ======================================================================
-- 表: km_app_token
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_app_token (
    token_id        BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    token           VARCHAR(64)     NOT NULL,
    token_name      VARCHAR(100)    NOT NULL,
    allowed_origins VARCHAR(500)    DEFAULT '*',
    expires_at      TIMESTAMP       DEFAULT NULL,
    status          CHAR(1)         DEFAULT '1',
    remark          VARCHAR(500)    DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    PRIMARY KEY (token_id),
    CONSTRAINT uk_km_app_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_km_app_token_app ON km_app_token (app_id);

COMMENT ON TABLE km_app_token IS '应用Token表';
COMMENT ON COLUMN km_app_token.allowed_origins IS '允许的来源';
COMMENT ON COLUMN km_app_token.app_id IS '应用ID';
COMMENT ON COLUMN km_app_token.create_by IS '创建人';
COMMENT ON COLUMN km_app_token.create_dept IS '创建部门';
COMMENT ON COLUMN km_app_token.create_time IS '创建时间';
COMMENT ON COLUMN km_app_token.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_app_token.expires_at IS '过期时间';
COMMENT ON COLUMN km_app_token.remark IS '备注';
COMMENT ON COLUMN km_app_token.status IS '状态（0禁用 1启用）';
COMMENT ON COLUMN km_app_token.token IS 'Token';
COMMENT ON COLUMN km_app_token.token_id IS 'Token ID';
COMMENT ON COLUMN km_app_token.token_name IS 'Token名称';
COMMENT ON COLUMN km_app_token.update_by IS '更新人';
COMMENT ON COLUMN km_app_token.update_time IS '更新时间';

-- ======================================================================
-- 表: km_data_source
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_data_source (
    data_source_id      BIGINT          NOT NULL,
    data_source_name    VARCHAR(200)    NOT NULL,
    source_type         VARCHAR(20)     NOT NULL,
    ds_key              VARCHAR(100),
    driver_class_name   VARCHAR(200),
    jdbc_url            VARCHAR(500),
    username            VARCHAR(100),
    password            VARCHAR(500),
    db_type             VARCHAR(50),
    is_enabled          CHAR(1)         DEFAULT '1',
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (data_source_id),
    CONSTRAINT uk_km_ds_name UNIQUE (data_source_name)
);

COMMENT ON TABLE km_data_source IS '数据源表-生成sql查询节点依赖';
COMMENT ON COLUMN km_data_source.create_by IS '创建人';
COMMENT ON COLUMN km_data_source.create_dept IS '创建部门';
COMMENT ON COLUMN km_data_source.create_time IS '创建时间';
COMMENT ON COLUMN km_data_source.data_source_id IS '数据源ID';
COMMENT ON COLUMN km_data_source.data_source_name IS '数据源名称';
COMMENT ON COLUMN km_data_source.db_type IS '数据库类型';
COMMENT ON COLUMN km_data_source.driver_class_name IS '驱动类名';
COMMENT ON COLUMN km_data_source.ds_key IS '数据源Key';
COMMENT ON COLUMN km_data_source.is_enabled IS '是否启用（0禁用 1启用）';
COMMENT ON COLUMN km_data_source.jdbc_url IS 'JDBC URL';
COMMENT ON COLUMN km_data_source.password IS '密码';
COMMENT ON COLUMN km_data_source.remark IS '备注';
COMMENT ON COLUMN km_data_source.source_type IS '数据源类型';
COMMENT ON COLUMN km_data_source.update_by IS '更新人';
COMMENT ON COLUMN km_data_source.update_time IS '更新时间';
COMMENT ON COLUMN km_data_source.username IS '用户名';

-- ======================================================================
-- 表: km_database_meta
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_database_meta (
    meta_id             BIGINT          NOT NULL,
    data_source_id      BIGINT          NOT NULL,
    meta_source_type    VARCHAR(20)     NOT NULL,
    ddl_content         TEXT,
    table_name          VARCHAR(200)    NOT NULL,
    table_comment       VARCHAR(500),
    columns             JSONB           DEFAULT NULL,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (meta_id),
    CONSTRAINT uk_km_ds_table UNIQUE (data_source_id, table_name)
);

COMMENT ON TABLE km_database_meta IS '数据库元数据表-生成sql查询节点依赖';
COMMENT ON COLUMN km_database_meta.columns IS '列信息';
COMMENT ON COLUMN km_database_meta.create_by IS '创建人';
COMMENT ON COLUMN km_database_meta.create_dept IS '创建部门';
COMMENT ON COLUMN km_database_meta.create_time IS '创建时间';
COMMENT ON COLUMN km_database_meta.data_source_id IS '数据源ID';
COMMENT ON COLUMN km_database_meta.ddl_content IS 'DDL内容';
COMMENT ON COLUMN km_database_meta.meta_id IS '元数据ID';
COMMENT ON COLUMN km_database_meta.meta_source_type IS '元数据源类型';
COMMENT ON COLUMN km_database_meta.remark IS '备注';
COMMENT ON COLUMN km_database_meta.table_comment IS '表注释';
COMMENT ON COLUMN km_database_meta.table_name IS '表名';
COMMENT ON COLUMN km_database_meta.update_by IS '更新人';
COMMENT ON COLUMN km_database_meta.update_time IS '更新时间';

-- ======================================================================
-- 表: km_workflow_instance
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_workflow_instance (
    instance_id     BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    workflow_config JSONB           NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    current_node    VARCHAR(64),
    global_state    JSONB,
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    error_message   VARCHAR(1000),
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    PRIMARY KEY (instance_id)
);

COMMENT ON TABLE km_workflow_instance IS '工作流实例表';
COMMENT ON COLUMN km_workflow_instance.app_id IS '应用ID';
COMMENT ON COLUMN km_workflow_instance.create_by IS '创建人';
COMMENT ON COLUMN km_workflow_instance.create_dept IS '创建部门';
COMMENT ON COLUMN km_workflow_instance.create_time IS '创建时间';
COMMENT ON COLUMN km_workflow_instance.current_node IS '当前节点';
COMMENT ON COLUMN km_workflow_instance.end_time IS '结束时间';
COMMENT ON COLUMN km_workflow_instance.error_message IS '错误信息';
COMMENT ON COLUMN km_workflow_instance.global_state IS '全局状态';
COMMENT ON COLUMN km_workflow_instance.instance_id IS '实例ID';
COMMENT ON COLUMN km_workflow_instance.session_id IS '会话ID';
COMMENT ON COLUMN km_workflow_instance.start_time IS '开始时间';
COMMENT ON COLUMN km_workflow_instance.status IS '状态（0运行中 1已完成 2已取消 3异常）';
COMMENT ON COLUMN km_workflow_instance.update_by IS '更新人';
COMMENT ON COLUMN km_workflow_instance.update_time IS '更新时间';
COMMENT ON COLUMN km_workflow_instance.workflow_config IS '工作流配置';

-- ======================================================================
-- 表: km_node_execution
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_node_execution (
    execution_id    BIGINT          NOT NULL,
    instance_id     BIGINT          NOT NULL,
    node_id         VARCHAR(64)     NOT NULL,
    node_type       VARCHAR(64)     NOT NULL,
    node_name       VARCHAR(200),
    status          VARCHAR(20)     NOT NULL,
    input_params    JSONB,
    output_params   JSONB,
    input_tokens    INTEGER         DEFAULT 0,
    output_tokens   INTEGER         DEFAULT 0,
    total_tokens    INTEGER         DEFAULT 0,
    duration_ms     BIGINT,
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    error_message   VARCHAR(1000),
    retry_count     INTEGER         DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    PRIMARY KEY (execution_id)
);

COMMENT ON TABLE km_node_execution IS '节点执行表';
COMMENT ON COLUMN km_node_execution.create_by IS '创建人';
COMMENT ON COLUMN km_node_execution.create_time IS '创建时间';
COMMENT ON COLUMN km_node_execution.duration_ms IS '耗时（毫秒）';
COMMENT ON COLUMN km_node_execution.end_time IS '结束时间';
COMMENT ON COLUMN km_node_execution.error_message IS '错误信息';
COMMENT ON COLUMN km_node_execution.execution_id IS '执行ID';
COMMENT ON COLUMN km_node_execution.input_params IS '输入参数';
COMMENT ON COLUMN km_node_execution.input_tokens IS '输入Token数';
COMMENT ON COLUMN km_node_execution.instance_id IS '实例ID';
COMMENT ON COLUMN km_node_execution.node_id IS '节点ID';
COMMENT ON COLUMN km_node_execution.node_name IS '节点名称';
COMMENT ON COLUMN km_node_execution.node_type IS '节点类型';
COMMENT ON COLUMN km_node_execution.output_params IS '输出参数';
COMMENT ON COLUMN km_node_execution.output_tokens IS '输出Token数';
COMMENT ON COLUMN km_node_execution.retry_count IS '重试次数';
COMMENT ON COLUMN km_node_execution.start_time IS '开始时间';
COMMENT ON COLUMN km_node_execution.status IS '状态（0运行中 1已完成 2已取消 3异常）';
COMMENT ON COLUMN km_node_execution.total_tokens IS '总Token数';
COMMENT ON COLUMN km_node_execution.update_by IS '更新人';
COMMENT ON COLUMN km_node_execution.update_time IS '更新时间';

-- ======================================================================
-- 表: km_chat_session
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_chat_session (
    session_id      BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    title           VARCHAR(128)    DEFAULT '新会话',
    user_id         BIGINT          NOT NULL,
    user_type       VARCHAR(20)     DEFAULT 'system_user',
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    del_flag        CHAR(1)         DEFAULT '0',
    PRIMARY KEY (session_id)
);

COMMENT ON TABLE km_chat_session IS '聊天会话表';
COMMENT ON COLUMN km_chat_session.app_id IS '应用ID';
COMMENT ON COLUMN km_chat_session.create_by IS '创建人';
COMMENT ON COLUMN km_chat_session.create_dept IS '创建部门';
COMMENT ON COLUMN km_chat_session.create_time IS '创建时间';
COMMENT ON COLUMN km_chat_session.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_chat_session.remark IS '备注';
COMMENT ON COLUMN km_chat_session.session_id IS '会话ID';
COMMENT ON COLUMN km_chat_session.title IS '会话标题';
COMMENT ON COLUMN km_chat_session.update_by IS '更新人';
COMMENT ON COLUMN km_chat_session.update_time IS '更新时间';
COMMENT ON COLUMN km_chat_session.user_id IS '用户ID';
COMMENT ON COLUMN km_chat_session.user_type IS '用户类型 (anonymous_user/system_user/third_user)';

-- ======================================================================
-- 表: km_chat_message
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_chat_message (
    message_id      BIGINT          NOT NULL,
    instance_id     BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    content         TEXT,
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    remark          VARCHAR(500),
    feedback_status int2 DEFAULT 0,
    total_tokens int DEFAULT 0,
    PRIMARY KEY (message_id)
);

COMMENT ON TABLE km_chat_message IS '聊天消息表';
COMMENT ON COLUMN km_chat_message.content IS '消息内容';
COMMENT ON COLUMN km_chat_message.create_by IS '创建人';
COMMENT ON COLUMN km_chat_message.create_dept IS '创建部门';
COMMENT ON COLUMN km_chat_message.create_time IS '创建时间';
COMMENT ON COLUMN km_chat_message.feedback_status IS '用户反馈状态：0=无评价，1=赞同(Like)，-1=踩(Dislike)';
COMMENT ON COLUMN km_chat_message.instance_id IS '实例ID';
COMMENT ON COLUMN km_chat_message.message_id IS '消息ID';
COMMENT ON COLUMN km_chat_message.remark IS '备注';
COMMENT ON COLUMN km_chat_message.role IS '角色 (user/assistant/system)';
COMMENT ON COLUMN km_chat_message.session_id IS '会话ID';
COMMENT ON COLUMN km_chat_message.total_tokens IS '该条消息或会话周期内消耗的 Token 总数';
COMMENT ON COLUMN km_chat_message.update_by IS '更新人';
COMMENT ON COLUMN km_chat_message.update_time IS '更新时间';

-- ======================================================================
-- 表: km_node_definition
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_node_definition (
    node_def_id         BIGINT          NOT NULL,
    node_type           VARCHAR(100)    NOT NULL,
    node_label          VARCHAR(200)    NOT NULL,
    node_icon           VARCHAR(200),
    node_color          VARCHAR(50),
    category            VARCHAR(50)     NOT NULL,
    description         VARCHAR(500),
    is_system           CHAR(1)         DEFAULT '0',
    is_enabled          CHAR(1)         DEFAULT '1',
    allow_custom_input_params CHAR(1) DEFAULT '0',
    allow_custom_output_params CHAR(1) DEFAULT '0',
    input_params        TEXT,
    output_params       TEXT,
    version             INTEGER         DEFAULT 1,
    parent_version_id   BIGINT,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (node_def_id),
    CONSTRAINT uk_km_node_type_ver UNIQUE (node_type, version)
);

COMMENT ON TABLE km_node_definition IS '节点定义表';
COMMENT ON COLUMN km_node_definition.allow_custom_input_params IS '允许自定义输入参数(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.allow_custom_output_params IS '允许自定义输出参数(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.category IS '节点分类';
COMMENT ON COLUMN km_node_definition.create_by IS '创建人';
COMMENT ON COLUMN km_node_definition.create_dept IS '创建部门';
COMMENT ON COLUMN km_node_definition.create_time IS '创建时间';
COMMENT ON COLUMN km_node_definition.description IS '节点描述';
COMMENT ON COLUMN km_node_definition.input_params IS '输入参数';
COMMENT ON COLUMN km_node_definition.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.is_system IS '是否系统节点(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.node_color IS '节点颜色';
COMMENT ON COLUMN km_node_definition.node_def_id IS '节点定义ID';
COMMENT ON COLUMN km_node_definition.node_icon IS '节点图标';
COMMENT ON COLUMN km_node_definition.node_label IS '节点标签';
COMMENT ON COLUMN km_node_definition.node_type IS '节点类型';
COMMENT ON COLUMN km_node_definition.output_params IS '输出参数';
COMMENT ON COLUMN km_node_definition.parent_version_id IS '父版本ID';
COMMENT ON COLUMN km_node_definition.remark IS '备注';
COMMENT ON COLUMN km_node_definition.update_by IS '更新人';
COMMENT ON COLUMN km_node_definition.update_time IS '更新时间';
COMMENT ON COLUMN km_node_definition.version IS '版本';

-- ======================================================================
-- 表: km_workflow_template
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_workflow_template (
    template_id         BIGINT          NOT NULL,
    template_name       VARCHAR(200)    NOT NULL,
    template_code       VARCHAR(100)    NOT NULL,
    description         VARCHAR(500),
    icon                VARCHAR(200),
    category            VARCHAR(50),
    scope_type          CHAR(1)         NOT NULL,
    workflow_config     TEXT            NOT NULL,
    graph_data          TEXT,
    dsl_data            TEXT            DEFAULT NULL,
    version             INTEGER         DEFAULT 1,
    parent_version_id   BIGINT,
    is_published        CHAR(1)         DEFAULT '0',
    publish_time        TIMESTAMP,
    is_enabled          CHAR(1)         DEFAULT '1',
    use_count           INTEGER         DEFAULT 0,
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (template_id),
    CONSTRAINT uk_km_tpl_scope_code UNIQUE (scope_type, template_code, version)
);

COMMENT ON TABLE km_workflow_template IS '工作流模板表';
COMMENT ON COLUMN km_workflow_template.category IS '分类';
COMMENT ON COLUMN km_workflow_template.create_by IS '创建人';
COMMENT ON COLUMN km_workflow_template.create_dept IS '创建部门';
COMMENT ON COLUMN km_workflow_template.create_time IS '创建时间';
COMMENT ON COLUMN km_workflow_template.description IS '描述';
COMMENT ON COLUMN km_workflow_template.graph_data IS '图数据';
COMMENT ON COLUMN km_workflow_template.icon IS '图标';
COMMENT ON COLUMN km_workflow_template.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_workflow_template.is_published IS '是否发布(0-否 1-是)';
COMMENT ON COLUMN km_workflow_template.parent_version_id IS '父版本ID';
COMMENT ON COLUMN km_workflow_template.publish_time IS '发布时间';
COMMENT ON COLUMN km_workflow_template.remark IS '备注';
COMMENT ON COLUMN km_workflow_template.scope_type IS '范围类型(1-固定 2-自定义)';
COMMENT ON COLUMN km_workflow_template.template_code IS '模板编码';
COMMENT ON COLUMN km_workflow_template.template_id IS '模板ID';
COMMENT ON COLUMN km_workflow_template.template_name IS '模板名称';
COMMENT ON COLUMN km_workflow_template.update_by IS '更新人';
COMMENT ON COLUMN km_workflow_template.update_time IS '更新时间';
COMMENT ON COLUMN km_workflow_template.use_count IS '使用次数';
COMMENT ON COLUMN km_workflow_template.version IS '版本';
COMMENT ON COLUMN km_workflow_template.workflow_config IS '工作流配置';

-- ======================================================================
-- 表: km_node_connection_rule
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_node_connection_rule (
    rule_id             BIGINT          NOT NULL,
    source_node_type    VARCHAR(100)    NOT NULL,
    target_node_type    VARCHAR(100)    NOT NULL,
    rule_type           CHAR(1)         NOT NULL,
    priority            INTEGER         DEFAULT 0,
    is_enabled          CHAR(1)         DEFAULT '1',
    create_dept         BIGINT,
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    remark              VARCHAR(500),
    PRIMARY KEY (rule_id),
    CONSTRAINT uk_km_node_conn UNIQUE (source_node_type, target_node_type)
);

COMMENT ON TABLE km_node_connection_rule IS '节点连接规则表';
COMMENT ON COLUMN km_node_connection_rule.create_by IS '创建人';
COMMENT ON COLUMN km_node_connection_rule.create_dept IS '创建部门';
COMMENT ON COLUMN km_node_connection_rule.create_time IS '创建时间';
COMMENT ON COLUMN km_node_connection_rule.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_node_connection_rule.priority IS '优先级';
COMMENT ON COLUMN km_node_connection_rule.remark IS '备注';
COMMENT ON COLUMN km_node_connection_rule.rule_id IS '规则ID';
COMMENT ON COLUMN km_node_connection_rule.rule_type IS '规则类型:0-允许；1-禁止';
COMMENT ON COLUMN km_node_connection_rule.source_node_type IS '源节点类型';
COMMENT ON COLUMN km_node_connection_rule.target_node_type IS '目标节点类型';
COMMENT ON COLUMN km_node_connection_rule.update_by IS '更新人';
COMMENT ON COLUMN km_node_connection_rule.update_time IS '更新时间';

-- ======================================================================
-- 表: km_mcp_server
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_mcp_server (
    server_id       BIGINT          NOT NULL,
    server_name     VARCHAR(64)     NOT NULL,
    description     VARCHAR(128)    DEFAULT '',
    transport_type  VARCHAR(20)     NOT NULL DEFAULT 'sse',
    server_config   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (server_id)
);

COMMENT ON TABLE km_mcp_server IS 'MCP Server 配置管理';
COMMENT ON COLUMN km_mcp_server.description IS '描述';
COMMENT ON COLUMN km_mcp_server.server_config IS 'MCP Server 配置(JSON)，如 url, transport 等';
COMMENT ON COLUMN km_mcp_server.server_id IS 'MCP Server ID';
COMMENT ON COLUMN km_mcp_server.server_name IS 'MCP Server 名称';
COMMENT ON COLUMN km_mcp_server.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN km_mcp_server.transport_type IS '传输类型（sse/streamable_http）';

-- ======================================================================
-- 表: km_builtin_tool
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_builtin_tool (
    tool_id         BIGINT          NOT NULL,
    tool_name       VARCHAR(64)     NOT NULL,
    spec            VARCHAR(128)    DEFAULT '',
    init_params     JSONB           DEFAULT NULL,
    input_schema    JSONB           DEFAULT NULL,
    output_schema   JSONB           DEFAULT NULL,
    python_code     TEXT            DEFAULT '',
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (tool_id)
);

COMMENT ON TABLE km_builtin_tool IS '内置 Python 工具管理';
COMMENT ON COLUMN km_builtin_tool.init_params IS '启动参数定义 schema (JSON Array)';
COMMENT ON COLUMN km_builtin_tool.input_schema IS '输入参数 JSON Schema（供 LLM 解析字段）';
COMMENT ON COLUMN km_builtin_tool.output_schema IS '输出参数 JSON Schema（供 LLM 解析字段）';
COMMENT ON COLUMN km_builtin_tool.python_code IS 'Python 脚本内容';
COMMENT ON COLUMN km_builtin_tool.spec IS '工具描述（提供给 LLM 的说明）';
COMMENT ON COLUMN km_builtin_tool.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN km_builtin_tool.tool_id IS '工具 ID';
COMMENT ON COLUMN km_builtin_tool.tool_name IS '工具名称（英文标识，作为 LLM Tool function name）';

-- ======================================================================
-- 表: km_skill
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_skill (
    skill_id        BIGINT          NOT NULL,
    skill_name      VARCHAR(64)     NOT NULL,
    spec            VARCHAR(500)    DEFAULT '',
    tool_bindings   JSONB           DEFAULT NULL,
    input_schema    JSONB           DEFAULT NULL,
    output_schema   JSONB           DEFAULT NULL,
    status          CHAR(1)         DEFAULT '0',
    create_dept     BIGINT          DEFAULT NULL,
    create_by       BIGINT          DEFAULT NULL,
    create_time     TIMESTAMP       DEFAULT NULL,
    update_by       BIGINT          DEFAULT NULL,
    update_time     TIMESTAMP       DEFAULT NULL,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500)    DEFAULT NULL,
    PRIMARY KEY (skill_id)
);

COMMENT ON TABLE km_skill IS '技能配置管理';
COMMENT ON COLUMN km_skill.input_schema IS '技能入参 JSON Schema';
COMMENT ON COLUMN km_skill.output_schema IS '技能出参 JSON Schema';
COMMENT ON COLUMN km_skill.skill_id IS '技能ID';
COMMENT ON COLUMN km_skill.skill_name IS '技能名称（英文标识，作为 LLM 函数名）';
COMMENT ON COLUMN km_skill.spec IS '技能说明（提供给大模型参考用）';
COMMENT ON COLUMN km_skill.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN km_skill.tool_bindings IS '绑定的工具配置集合 JSON Array [{type:"builtin",id:1}, ...]';

-- ======================================================================
-- 第三部分: 初始化数据
-- ======================================================================

-- km_model
INSERT INTO km_model (model_id, provider_id, model_name, model_type, model_key, is_builtin, model_source, status, is_default, create_time)
VALUES
    (30, 11, 'bge-reranker-v2-m3 (内置)', '3', 'bge-reranker-v2-m3', 'Y', '2', '0', 1, CURRENT_TIMESTAMP),
    (31, 11, 'bge-small-zh (内置)', '2', 'bge-small-zh', 'Y', '2', '0', 1, CURRENT_TIMESTAMP),
    (31, 1, 'text-embedding-3-small', '2', 'text-embedding-3-small', 'N', '1', '0', 0, CURRENT_TIMESTAMP),
    (40, 3, 'nomic-embed-text', '2', 'nomic-embed-text', 'N', '2', '0', 0, CURRENT_TIMESTAMP),
    (50, 7, 'text-embedding-v2', '2', 'text-embedding-v2', 'N', '1', '0', 0, CURRENT_TIMESTAMP),
    (60, 8, 'embedding-2', '2', 'embedding-2', 'N', '1', '0', 0, CURRENT_TIMESTAMP),
    (32, 12, 'BAAI/bge-reranker-v2-m3', '3', 'BAAI/bge-reranker-v2-m3', 'N', '1', '0', 0, CURRENT_TIMESTAMP)
ON CONFLICT (model_id) DO NOTHING;

-- km_model_provider
INSERT INTO km_model_provider (provider_id, provider_name, provider_key, provider_type, default_endpoint, site_url, icon_url, config_schema, status, sort, models, create_time)
VALUES
    (1, 'OpenAI', 'openai', '1', 'https://api.openai.com/v1', 'https://openai.com', '/model-provider-icon/openai.png', NULL, '0', 1, '[{"modelKey": "gpt-4o", "modelType": "1"}, {"modelKey": "gpt-4o-mini", "modelType": "1"}, {"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-3.5-turbo", "modelType": "1"}, {"modelKey": "text-embedding-3-small", "modelType": "2"}, {"modelKey": "text-embedding-3-large", "modelType": "2"}, {"modelKey": "text-embedding-ada-002", "modelType": "2"}]', CURRENT_TIMESTAMP),
    (2, 'Gemini', 'gemini', '1', 'https://generativelanguage.googleapis.com', 'https://ai.google.dev', '/model-provider-icon/gemini.svg', NULL, '0', 2, '[{"modelKey": "gemini-3-flash-preview", "modelType": "1"}, {"modelKey": "gemini-3-pro-preview", "modelType": "1"}, {"modelKey": "gemini-2.5-flash", "modelType": "1"}, {"modelKey": "text-embedding-004", "modelType": "2"}]', CURRENT_TIMESTAMP),
    (3, 'Ollama', 'ollama', '2', 'http://localhost:11434', 'https://ollama.com', '/model-provider-icon/ollama.png', NULL, '0', 3, '[{"modelKey": "llama3", "modelType": "1"}, {"modelKey": "llama2", "modelType": "1"}, {"modelKey": "mistral", "modelType": "1"}, {"modelKey": "mixtral", "modelType": "1"}, {"modelKey": "phi3", "modelType": "1"}, {"modelKey": "qwen2", "modelType": "1"}, {"modelKey": "gemma2", "modelType": "1"}, {"modelKey": "nomic-embed-text", "modelType": "2"}, {"modelKey": "mxbai-embed-large", "modelType": "2"}]', CURRENT_TIMESTAMP),
    (4, 'DeepSeek', 'deepseek', '1', 'https://api.deepseek.com', 'https://www.deepseek.com', '/model-provider-icon/deepseek.png', NULL, '0', 4, '[{"modelKey": "deepseek-chat", "modelType": "1"}, {"modelKey": "deepseek-coder", "modelType": "1"}]', CURRENT_TIMESTAMP),
    (5, 'vLLM', 'vllm', '2', 'http://localhost:8000/v1', 'https://docs.vllm.ai', '/model-provider-icon/vllm.ico', NULL, '0', 5, '[]', CURRENT_TIMESTAMP),
    (6, 'Azure OpenAI', 'azure', '1', 'https://{resource}.openai.azure.com', 'https://azure.microsoft.com/products/ai-services/openai-service', '/model-provider-icon/azure.png', NULL, '0', 6, '[{"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-4-turbo", "modelType": "1"}, {"modelKey": "gpt-35-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
    (7, '阿里云百炼', 'bailian', '1', 'https://dashscope.aliyuncs.com/api/v1', 'https://www.aliyun.com/product/bailian', '/model-provider-icon/bailian.jpeg', NULL, '0', 7, '[{"modelKey": "qwen-max", "modelType": "1"}, {"modelKey": "qwen-plus", "modelType": "1"}, {"modelKey": "qwen-turbo", "modelType": "1"}, {"modelKey": "text-embedding-v1", "modelType": "2"}, {"modelKey": "text-embedding-v2", "modelType": "2"}]', CURRENT_TIMESTAMP),
    (8, '智谱AI', 'zhipu', '1', 'https://open.bigmodel.cn/api/paas/v4', 'https://open.bigmodel.cn', '/model-provider-icon/zhipu.png', NULL, '0', 8, '[{"modelKey": "glm-4", "modelType": "1"}, {"modelKey": "glm-4-flash", "modelType": "1"}, {"modelKey": "glm-3-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
    (9, '火山引擎(豆包)', 'doubao', '1', 'https://ark.cn-beijing.volces.com/api/v3', 'https://www.volcengine.com/product/doubao', '/model-provider-icon/doubao.png', NULL, '0', 9, '[{"modelKey": "doubao-pro-32k", "modelType": "1"}, {"modelKey": "doubao-lite-32k", "modelType": "1"}]', CURRENT_TIMESTAMP),
    (10, 'Moonshot', 'moonshot', '1', 'https://api.moonshot.cn/v1', 'https://www.moonshot.cn', '/model-provider-icon/moonshot.ico', NULL, '0', 10, '[{"modelKey": "moonshot-v1-8k", "modelType": "1"}, {"modelKey": "moonshot-v1-32k", "modelType": "1"}, {"modelKey": "moonshot-v1-128k", "modelType": "1"}]', CURRENT_TIMESTAMP),
    (11, 'Local (内置)', 'local', '2', '', '', '/model-provider-icon/logo.png', NULL, '0', 11, '[{"modelKey": "bge-reranker-v2-m3", "modelType": "3"},{"modelKey": "bge-small-zh", "modelType": "2"}]', CURRENT_TIMESTAMP),
    (12, 'SiliconFlow (硅基流动)', 'siliconflow', '1', 'https://api.siliconflow.cn/v1/', 'https://siliconflow.cn/', '/model-provider-icon/logo-siliconflow.svg', NULL, '0', 12, '[{"modelKey": "deepseek-ai/DeepSeek-V3", "modelType": "1"},{"modelKey": "deepseek-ai/DeepSeek-R1", "modelType": "1"},{"modelKey": "Qwen/Qwen2.5-72B-Instruct", "modelType": "1"},{"modelKey": "Qwen/Qwen3-Reranker-0.6B", "modelType": "3"},{"modelKey": "BAAI/bge-reranker-v2-m3", "modelType": "3"},{"modelKey": "BAAI/bge-m3", "modelType": "2"}]', CURRENT_TIMESTAMP)
ON CONFLICT (provider_id) DO NOTHING;

-- km_node_connection_rule
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time)
VALUES
    (1, 'START', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (2, 'START', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (3, 'START', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (4, 'START', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (5, 'LLM_CHAT', 'END', '0', 10, CURRENT_TIMESTAMP),
    (6, 'LLM_CHAT', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (7, 'LLM_CHAT', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (8, 'LLM_CHAT', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (9, 'INTENT_CLASSIFIER', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (10, 'INTENT_CLASSIFIER', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (11, 'INTENT_CLASSIFIER', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (12, 'INTENT_CLASSIFIER', 'END', '0', 10, CURRENT_TIMESTAMP),
    (13, 'CONDITION', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (14, 'CONDITION', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (15, 'CONDITION', 'END', '0', 10, CURRENT_TIMESTAMP),
    (16, 'FIXED_RESPONSE', 'END', '0', 10, CURRENT_TIMESTAMP),
    (17, 'START', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (18, 'LLM_CHAT', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (19, 'CONDITION', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (20, 'INTENT_CLASSIFIER', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (21, 'DB_QUERY', 'END', '0', 10, CURRENT_TIMESTAMP),
    (22, 'DB_QUERY', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (23, 'DB_QUERY', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (24, 'DB_QUERY', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (25, 'DB_QUERY', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (26, 'START', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (27, 'LLM_CHAT', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (28, 'CONDITION', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (29, 'INTENT_CLASSIFIER', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (30, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (31, 'SQL_GENERATE', 'END', '0', 10, CURRENT_TIMESTAMP),
    (32, 'SQL_GENERATE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (33, 'SQL_GENERATE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (34, 'SQL_GENERATE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (35, 'START', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (37, 'LLM_CHAT', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (38, 'CONDITION', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (39, 'INTENT_CLASSIFIER', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (40, 'SQL_EXECUTE', 'END', '0', 10, CURRENT_TIMESTAMP),
    (41, 'SQL_EXECUTE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (42, 'SQL_EXECUTE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (43, 'SQL_EXECUTE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (44, 'SQL_EXECUTE', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (50, 'START', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (51, 'LLM_CHAT', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (52, 'CONDITION', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (53, 'INTENT_CLASSIFIER', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (54, 'KNOWLEDGE_RETRIEVAL', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (55, 'KNOWLEDGE_RETRIEVAL', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (56, 'KNOWLEDGE_RETRIEVAL', 'END', '0', 10, CURRENT_TIMESTAMP),
    (100, 'START', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (101, 'LLM_CHAT', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (102, 'INTENT_CLASSIFIER', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (103, 'CONDITION', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (104, 'DB_QUERY', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (105, 'SQL_GENERATE', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (106, 'SQL_EXECUTE', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (107, 'KNOWLEDGE_RETRIEVAL', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (108, 'TOOL', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (109, 'TOOL', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (110, 'TOOL', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (111, 'TOOL', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (112, 'TOOL', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (113, 'TOOL', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (114, 'TOOL', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (115, 'TOOL', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (116, 'TOOL', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (117, 'TOOL', 'END', '0', 10, CURRENT_TIMESTAMP),
    (120, 'START', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (121, 'LLM_CHAT', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (122, 'INTENT_CLASSIFIER', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (123, 'CONDITION', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (124, 'DB_QUERY', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (125, 'SQL_GENERATE', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (126, 'SQL_EXECUTE', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (127, 'KNOWLEDGE_RETRIEVAL', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (128, 'TOOL', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (129, 'SKILL', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (130, 'SKILL', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (131, 'SKILL', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (132, 'SKILL', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (133, 'SKILL', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (134, 'SKILL', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (135, 'SKILL', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (136, 'SKILL', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (137, 'SKILL', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (138, 'SKILL', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (139, 'SKILL', 'END', '0', 10, CURRENT_TIMESTAMP),
    (140, 'START', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (141, 'LLM_CHAT', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (142, 'INTENT_CLASSIFIER', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (143, 'CONDITION', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (144, 'DB_QUERY', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (145, 'SQL_GENERATE', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (146, 'SQL_EXECUTE', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (147, 'KNOWLEDGE_RETRIEVAL', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (148, 'TOOL', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (149, 'SKILL', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (150, 'LOOP', 'LOOP', '0', 10, CURRENT_TIMESTAMP),
    (151, 'LOOP', 'TOOL', '0', 10, CURRENT_TIMESTAMP),
    (152, 'LOOP', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP),
    (153, 'LOOP', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
    (154, 'LOOP', 'CONDITION', '0', 10, CURRENT_TIMESTAMP),
    (155, 'LOOP', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
    (156, 'LOOP', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
    (157, 'LOOP', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
    (158, 'LOOP', 'KNOWLEDGE_RETRIEVAL', '0', 10, CURRENT_TIMESTAMP),
    (159, 'LOOP', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
    (160, 'LOOP', 'END', '0', 10, CURRENT_TIMESTAMP),
    (161, 'LOOP', 'SKILL', '0', 10, CURRENT_TIMESTAMP),
    (162, 'FIXED_RESPONSE', 'LOOP', '0', 10, CURRENT_TIMESTAMP)
ON CONFLICT (rule_id) DO NOTHING;

-- km_node_definition
INSERT INTO km_node_definition (node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, allow_custom_input_params, allow_custom_output_params, input_params, output_params, "version", parent_version_id, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, 'APP_INFO', '基础信息', 'mdi-information', '#64748BFF', 'basic', '应用的基础信息配置', '1', '1', '0', '0', '[]', '[]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (2, 'START', '开始', 'mdi-play-circle', '#64748BFF', 'basic', '工作流的入口节点', '1', '1', '0', '0', '[]', '[{"key": "userInput", "type": "string", "label": "用户输入", "required": true, "description": "用户的输入内容"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (3, 'END', '结束', 'mdi-stop-circle', '#64748BFF', 'basic', '工作流的结束节点，可以把各节点的输出参数引用进来，组合成最终回复消息作为工作流最终输出', '1', '1', '1', '0', '[{"key": "finalResponse", "type": "string", "label": "最终回复", "required": true, "description": "返回给用户的最终回复内容"}]', '[]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (4, 'LLM_CHAT', 'LLM 对话', 'mdi-robot', '#A855F7FF', 'ai', '调用大语言模型进行对话', '0', '1', '1', '1', '[{"key": "userInput", "type": "string", "label": "输入消息", "required": true, "description": "传递给 LLM 的输入消息"}, {"key": "chatContext", "type": "string", "label": "上下文", "required": false, "description": "比如可以传递知识库的检索结果", "defaultValue": ""}, {"key": "retrievedDocs", "type": "array", "label": "知识检索结果记录", "required": false, "description": "知识检索结果记录列表", "defaultValue": ""}]', '[{"key": "response", "type": "string", "label": "AI 回复", "required": true, "description": "LLM 生成的回复内容"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (5, 'INTENT_CLASSIFIER', '意图分类', 'mdi-sitemap', '#A855F7FF', 'ai', '识别用户输入的意图并分类', '0', '1', '0', '0', '[{"key": "instruction", "type": "string", "label": "文本指令", "required": true, "description": "需要分类的指令"}]', '[{"key": "intent", "type": "string", "label": "匹配的意图", "required": true, "description": "识别出的意图名称"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (6, 'CONDITION', '条件判断', 'mdi-source-branch', '#27560BFF', 'logic', '根据条件表达式进行分支判断', '0', '1', '0', '0', '[{"key": "matchedBranch", "type": "string", "label": "匹配的分支", "required": false, "description": "用于条件判断的值"}]', '[{"key": "matchedBranch", "type": "string", "label": "匹配的分支", "required": true, "description": "满足条件的分支名称"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (7, 'FIXED_RESPONSE', '指定回复', 'mdi-message-text', '#0F1A3ACF', 'action', '返回预设的固定文本内容', '0', '1', '1', '0', '[]', '[{"key": "response", "type": "string", "label": "回复内容", "required": true, "description": "固定的回复文本"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (8, 'DB_QUERY', '数据库对话', 'mdi-database-search', '#A855F7FF', 'ai', '结合LLM智能分析用户问题，生成SQL查询并返回自然语言回答', '0', '1', '0', '0', '[{"key": "userQuery", "type": "string", "label": "用户问题", "required": true, "description": "用户提出的业务问题"}]', '[{"key": "generatedSql", "type": "string", "label": "生成的SQL", "required": true, "description": "LLM生成的SQL语句"}, {"key": "queryResult", "type": "object", "label": "查询结果", "required": true, "description": "SQL执行结果(JSON)"}, {"key": "response", "type": "string", "label": "AI回复", "required": true, "description": "基于查询结果生成的自然语言回答"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (9, 'SQL_GENERATE', 'SQL生成', 'mdi-database-cog', '#8b5cf6', 'ai', '使用LLM分析用户问题，结合数据库元数据生成SQL语句', '0', '1', '0', '0', '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"description":"用户提出的业务问题"}]', '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"description":"LLM生成的SQL语句"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (10, 'SQL_EXECUTE', 'SQL执行', 'mdi-database-arrow-right', '#F59E0BFF', 'database', '执行SQL语句并返回查询结果', '0', '1', '0', '0', '[{"key": "sql", "type": "string", "label": "SQL语句", "required": true, "description": "待执行的SQL语句"}]', '[{"key": "queryResult", "type": "object", "label": "查询结果", "required": true, "description": "SQL执行结果(JSON)"}, {"key": "rowCount", "type": "number", "label": "返回行数", "required": true, "description": "查询返回的行数"}, {"key": "strResult", "type": "string", "label": "查询结果", "required": true, "description": "", "defaultValue": ""}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (11, 'KNOWLEDGE_RETRIEVAL', '知识检索', 'mdi-book-search', '#F59E0BFF', 'database', '从知识库中检索相关文档片段，用于RAG对话', '0', '1', '0', '0', '[{"key": "query", "type": "string", "label": "查询文本", "required": true, "description": "用于检索的查询文本"}]', '[{"key": "context", "type": "string", "label": "检索上下文", "required": true, "description": "拼接后的上下文文本"}, {"key": "docCount", "type": "number", "label": "文档数量", "required": true, "description": "检索到的文档数量"}, {"key": "retrievedDocs", "type": "array", "label": "检索结果", "required": true, "description": "检索到的文档片段列表"}]', 1, NULL, 103, 1, NOW(), 1, NOW(), NULL),
    (12, 'TOOL', '工具节点', 'mdi-tools', '#0d9488', 'action', '执行系统内置工具或MCP服务集成工具', '1', '1', '1', '1', '[]', '[]', 1, NULL, 103, 1, NOW(), 1, NOW(), 'MCP/Built-in Tool Support'),
    (13, 'SKILL', '技能节点', 'mdi-brain', '#ef4444', 'action', '统一执行由多个工具编排而成的技能', '1', '1', '1', '1', '[]', '[]', 1, NULL, 103, 1, NOW(), 1, NOW(), 'Skill-based execution'),
    (14, 'LOOP', '循环节点', 'mdi-sync', '#27560BFF', 'logic', '用于执行一个循环流程，直到满足条件就跳出或结束', '0', '1', '0', '0', '[]', '[]', 1, NULL, 103, 1, NOW(), 1, NOW(), 'Iteration support')
ON CONFLICT (node_def_id) DO NOTHING;

-- km_builtin_tool
INSERT INTO km_builtin_tool (tool_id, tool_name, spec, init_params, input_schema, output_schema, python_code, status, create_time, update_time) 
VALUES 
    (1, 'date_formatter', '将日期格式化为中文显示格式（例如：2024年03月20日）', '[]', '{"type": "object", "properties": {"date": {"type": "string", "description": "ISO 格式日期 (如: 2024-03-20)，不传则用当前日期"}}}', '{"type": "object", "properties": {"formatted_date": {"type": "string", "description": "格式化后的中文日期"}}}', E'import json\nimport sys\nfrom datetime import datetime\n\ndef main():\n    try:\n        if len(sys.argv) < 2:\n            input_data = {}\n        else:\n            with open(sys.argv[1], "r", encoding="utf-8") as f:\n                input_data = json.load(f)\n        \n        date_str = input_data.get("date")\n        if date_str:\n            dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))\n        else:\n            dt = datetime.now()\n            \n        formatted_date = dt.strftime("%Y年%m月%d日")\n        print(json.dumps({"formatted_date": formatted_date}, ensure_ascii=False))\n    except Exception as e:\n        print(json.dumps({"error": str(e)}, ensure_ascii=False))\n        sys.exit(1)\n\nif __name__ == "__main__":\n    main()', '0', NOW(), NOW()),
    (2, 'builtin_increment', '加1的核心逻辑', '[{"name": "init_number", "type": "number", "required": false, "description": "初始数值", "displayName": "初始数值", "defaultValue": "0"}]'::jsonb, '{"type": "object", "properties": {"number": {"type": "number", "description": "初始数值"}}}'::jsonb, '{"type": "object", "required": ["new_number"], "properties": {"new_number": {"type": "number", "description": "增加 1 后的结果值"}}}'::jsonb, E'import json\nimport sys\ndef main(params):\n    number = params.get("number")\n    if number is None: number = params.get("init_number", 0)\n    return {"new_number": int(number) + 1}\nwith open(sys.argv[1], ''r'', encoding=''utf-8'') as f:\n    args = json.load(f)\nprint(json.dumps(main(args)))', '0', NOW(), NOW())
ON CONFLICT (tool_id) DO UPDATE SET python_code = EXCLUDED.python_code;

-- km_skill
INSERT INTO km_skill (skill_id, skill_name, spec, tool_bindings, input_schema, output_schema, status, create_dept, create_by, create_time, update_by, update_time, del_flag, remark)
VALUES
    (2, 'integer_increment_skill', '通用的整数自增技能', '[{"id": 2, "type": "builtin"}]'::jsonb, '{"type": "object", "required": ["number"], "properties": {"number": {"type": "number"}}}'::jsonb, '{"type": "object", "required": ["result"], "properties": {"result": {"type": "number"}}}'::jsonb, '0', 103, 1, '2026-03-22 16:57:16', 1, '2026-03-22 16:57:16', '0', NULL),
    (1, 'date_formatter_skill', '调用内置日期格式化工具', '[{"type":"builtin", "id":1}]'::jsonb, '{"type": "object", "properties": {"date": {"type": "string"}}}'::jsonb, '{"type": "object", "properties": {"formatted_date": {"type": "string"}}}'::jsonb, '0', 103, 1, NOW(), 1, NOW(), '0', 'Date tool skill')
ON CONFLICT (skill_id) DO NOTHING;

-- sj_job
INSERT INTO sj_job (id, namespace_id, group_name, job_name, args_str, args_type, next_trigger_at, job_status, task_type, route_key, executor_type, executor_info, trigger_type, trigger_interval, block_strategy, executor_timeout, max_retry_times, parallel_num, retry_interval, bucket_index, resident, notify_ids, owner_id, labels, description, ext_attrs, deleted, create_dt, update_dt)
VALUES
    (1, 'dev', 'ruoyi_group', 'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', 1, '', '', '', 0, now(), now())
ON CONFLICT (id) DO NOTHING;

-- sj_system_user
INSERT INTO sj_system_user (username, password, role)
VALUES
    ('admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2)
ON CONFLICT (id) DO NOTHING;

-- sys_client
INSERT INTO sys_client (id, client_id, client_key, client_secret, grant_type, device_type, active_timeout, timeout, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
VALUES
    (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, 0, 0, 103, 1, now(), 1, now()),
    (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, 0, 0, 103, 1, now(), 1, now())
ON CONFLICT (id) DO NOTHING;

-- sys_config
INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, NOW(), NULL, NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),
    (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, NOW(), NULL, NULL, '初始化密码 123456'),
    (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, NOW(), NULL, NULL, '深色主题theme-dark，浅色主题theme-light'),
    (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, NOW(), NULL, NULL, '是否开启注册用户功能（true开启，false关闭）'),
    (11, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, NOW(), NULL, NULL, 'true:开启, false:关闭'),
    (12, '公共演示环境标志', 'sys.demo.enabled', 'true', 'Y', 103, 1, NOW(), NULL, NULL, 'true:开启, false:关闭'),
    (13, '聊天对话默认限流配置', 'chat.rate.limit.default', '{"minute":{"requests":10,"tokens":10000},"hour":{"requests":100,"tokens":100000},"day":{"requests":1000,"tokens":1000000}}', 'Y', 1, 1, NOW(), NULL, NULL, '嵌入第三方对话窗口的默认频率与Token限制，用户可在限流管理页面为特定用户配置覆盖值')
ON CONFLICT (config_id) DO NOTHING;

-- sys_dept
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, dept_category, order_num, leader, phone, email, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
VALUES
    (100, 0, '0', '科亿信息技术', null,0, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (101, 100, '0,100', '深圳总公司', null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (102, 100, '0,100', '长沙分公司', null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (103, 101, '0,100,101', '研发部门', null,1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (104, 101, '0,100,101', '市场部门', null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (105, 101, '0,100,101', '测试部门', null,3, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (106, 101, '0,100,101', '财务部门', null,4, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (107, 101, '0,100,101', '运维部门', null,5, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (108, 102, '0,100,102', '市场部门', null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null),
    (109, 102, '0,100,102', '财务部门', null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null)
ON CONFLICT (dept_id) DO NOTHING;

-- sys_dict_data
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', 103, 1, now(), null, null, '性别男'),
    (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', 103, 1, now(), null, null, '性别女'),
    (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', 103, 1, now(), null, null, '性别未知'),
    (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 103, 1, now(), null, null, '显示菜单'),
    (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 103, 1, now(), null, null, '隐藏菜单'),
    (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 103, 1, now(), null, null, '正常状态'),
    (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 103, 1, now(), null, null, '停用状态'),
    (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 103, 1, now(), null, null, '系统默认是'),
    (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 103, 1, now(), null, null, '系统默认否'),
    (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 103, 1, now(), null, null, '通知'),
    (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 103, 1, now(), null, null, '公告'),
    (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 103, 1, now(), null, null, '正常状态'),
    (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 103, 1, now(), null, null, '关闭状态'),
    (29, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '其他操作'),
    (18, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '新增操作'),
    (19, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '修改操作'),
    (20, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '删除操作'),
    (21, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 103, 1, now(), null, null, '授权操作'),
    (22, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '导出操作'),
    (23, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '导入操作'),
    (24, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '强退操作'),
    (25, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '生成操作'),
    (26, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '清空操作'),
    (27, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 103, 1, now(), null, null, '正常状态'),
    (28, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 103, 1, now(), null, null, '停用状态'),
    (30, 0, '密码认证', 'password', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '密码认证'),
    (31, 0, '短信认证', 'sms', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '短信认证'),
    (32, 0, '邮件认证', 'email', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '邮件认证'),
    (33, 0, '小程序认证', 'xcx', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '小程序认证'),
    (34, 0, '三方登录认证', 'social', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '三方登录认证'),
    (35, 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, 'PC'),
    (36, 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, '安卓'),
    (37, 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, 'iOS'),
    (38, 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, '小程序'),
    (39, 1, '知识问答', 'knowledge_qa', 'km_workflow_template_category', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '知识问答类型的工作流模板'),
    (40, 2, '智能客服', 'customer_service', 'km_workflow_template_category', '', 'success', 'N', 103, 1, now(), NULL, NULL, '智能客服类型的工作流模板'),
    (41, 3, '营销', 'marketing', 'km_workflow_template_category', '', 'warning', 'N', 103, 1, now(), NULL, NULL, '营销类型的工作流模板')
ON CONFLICT (dict_code) DO NOTHING;

-- sys_dict_type
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, '用户性别', 'sys_user_sex', 103, 1, now(), null, null, '用户性别列表'),
    (2, '菜单状态', 'sys_show_hide', 103, 1, now(), null, null, '菜单状态列表'),
    (3, '系统开关', 'sys_normal_disable', 103, 1, now(), null, null, '系统开关列表'),
    (6, '系统是否', 'sys_yes_no', 103, 1, now(), null, null, '系统是否列表'),
    (7, '通知类型', 'sys_notice_type', 103, 1, now(), null, null, '通知类型列表'),
    (8, '通知状态', 'sys_notice_status', 103, 1, now(), null, null, '通知状态列表'),
    (9, '操作类型', 'sys_oper_type', 103, 1, now(), null, null, '操作类型列表'),
    (10, '系统状态', 'sys_common_status', 103, 1, now(), null, null, '登录状态列表'),
    (11, '授权类型', 'sys_grant_type', 103, 1, now(), null, null, '认证授权类型'),
    (12, '设备类型', 'sys_device_type', 103, 1, now(), null, null, '客户端设备类型'),
    (13, '工作流模板分类', 'km_workflow_template_category', 103, 1, now(), NULL, NULL, '工作流模板分类列表')
ON CONFLICT (dict_id) DO NOTHING;

-- sys_menu - 完整的菜单INSERT语句（已重组AI管理模块）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    -- ======================================================================
    -- 系统管理模块 (保持原有结构)
    -- ======================================================================
    ('1', '系统管理', '0', '2', 'system', null, '', '1', '0', 'M', '0', '0', '', 'system', 103, 1, now(), null, null, '系统管理目录'),
    ('2', '系统监控', '0', '3', 'monitor', null, '', '1', '0', 'M', '0', '0', '', 'monitor', 103, 1, now(), null, null, '系统监控目录'),
    ('3', '系统工具', '0', '4', 'tool', null, '', '1', '0', 'M', '0', '0', '', 'tool', 103, 1, now(), null, null, '系统工具目录'),
    ('5', '测试菜单', '0', '5', 'demo', null, '', '1', '0', 'M', '0', '1', null, 'star', 103, 1, now(), null, null, '测试菜单'),
    
    -- 系统管理-二级菜单
    ('100', '用户管理', '1', '1', 'user', 'system/user/index', '', '1', '0', 'C', '0', '0', 'system:user:list', 'user', 103, 1, now(), null, null, '用户管理菜单'),
    ('101', '角色管理', '1', '2', 'role', 'system/role/index', '', '1', '0', 'C', '0', '0', 'system:role:list', 'peoples', 103, 1, now(), null, null, '角色管理菜单'),
    ('102', '菜单管理', '1', '3', 'menu', 'system/menu/index', '', '1', '0', 'C', '0', '0', 'system:menu:list', 'tree-table', 103, 1, now(), null, null, '菜单管理菜单'),
    ('103', '部门管理', '1', '4', 'dept', 'system/dept/index', '', '1', '0', 'C', '0', '0', 'system:dept:list', 'tree', 103, 1, now(), null, null, '部门管理菜单'),
    ('104', '岗位管理', '1', '5', 'post', 'system/post/index', '', '1', '0', 'C', '0', '0', 'system:post:list', 'post', 103, 1, now(), null, null, '岗位管理菜单'),
    ('105', '字典管理', '1', '6', 'dict', 'system/dict/index', '', '1', '0', 'C', '0', '0', 'system:dict:list', 'dict', 103, 1, now(), null, null, '字典管理菜单'),
    ('106', '参数设置', '1', '7', 'config', 'system/config/index', '', '1', '0', 'C', '0', '0', 'system:config:list', 'edit', 103, 1, now(), null, null, '参数设置菜单'),
    ('107', '通知公告', '1', '8', 'notice', 'system/notice/index', '', '1', '0', 'C', '0', '0', 'system:notice:list', 'message', 103, 1, now(), null, null, '通知公告菜单'),
    ('108', '日志管理', '1', '9', 'log', '', '', '1', '0', 'M', '0', '0', '', 'log', 103, 1, now(), null, null, '日志管理菜单'),
    ('118', '文件管理', '1', '10', 'oss', 'system/oss/index', '', '1', '0', 'C', '0', '0', 'system:oss:list', 'upload', 103, 1, now(), null, null, '文件管理菜单'),
    ('123', '客户端管理', '1', '11', 'client', 'system/client/index', '', '1', '0', 'C', '0', '0', 'system:client:list', 'international', 103, 1, now(), null, null, '客户端管理菜单'),
    ('133', '文件配置管理', '1', '10', 'oss-config/index', 'system/oss-config/index', '', '1', '1', 'C', '1', '0', 'system:ossConfig:list', '#', 103, 1, now(), null, null, '/system/oss'),
    
    -- 系统监控-二级菜单
    ('109', '在线用户', '2', '1', 'online', 'monitor/online/index', '', '1', '0', 'C', '0', '0', 'monitor:online:list', 'online', 103, 1, now(), null, null, '在线用户菜单'),
    ('113', '缓存监控', '2', '5', 'cache', 'monitor/cache/index', '', '1', '0', 'C', '0', '0', 'monitor:cache:list', 'redis', 103, 1, now(), null, null, '缓存监控菜单'),
    
    -- 日志管理-三级菜单
    ('500', '操作日志', '108', '1', 'operlog', 'monitor/operlog/index', '', '1', '0', 'C', '0', '0', 'monitor:operlog:list', 'form', 103, 1, now(), null, null, '操作日志菜单'),
    ('501', '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '1', '0', 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 103, 1, now(), null, null, '登录日志菜单'),
    
    -- 系统工具-二级菜单
    ('115', '代码生成', '3', '2', 'gen', 'tool/gen/index', '', '1', '0', 'C', '0', '0', 'tool:gen:list', 'code', 103, 1, now(), null, null, '代码生成菜单'),
    
    -- ======================================================================
    -- 系统管理模块 - 按钮权限
    -- ======================================================================
    -- 用户管理按钮
    ('1001', '用户查询', '100', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:user:query', '#', 103, 1, now(), null, null, ''),
    ('1002', '用户新增', '100', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:user:add', '#', 103, 1, now(), null, null, ''),
    ('1003', '用户修改', '100', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:user:edit', '#', 103, 1, now(), null, null, ''),
    ('1004', '用户删除', '100', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:user:remove', '#', 103, 1, now(), null, null, ''),
    ('1005', '用户导出', '100', '5', '', '', '', '1', '0', 'F', '0', '0', 'system:user:export', '#', 103, 1, now(), null, null, ''),
    ('1006', '用户导入', '100', '6', '', '', '', '1', '0', 'F', '0', '0', 'system:user:import', '#', 103, 1, now(), null, null, ''),
    ('1007', '重置密码', '100', '7', '', '', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, now(), null, null, ''),
    
    -- 角色管理按钮
    ('1008', '角色查询', '101', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:role:query', '#', 103, 1, now(), null, null, ''),
    ('1009', '角色新增', '101', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:role:add', '#', 103, 1, now(), null, null, ''),
    ('1010', '角色修改', '101', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:role:edit', '#', 103, 1, now(), null, null, ''),
    ('1011', '角色删除', '101', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:role:remove', '#', 103, 1, now(), null, null, ''),
    ('1012', '角色导出', '101', '5', '', '', '', '1', '0', 'F', '0', '0', 'system:role:export', '#', 103, 1, now(), null, null, ''),
    
    -- 菜单管理按钮
    ('1013', '菜单查询', '102', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:query', '#', 103, 1, now(), null, null, ''),
    ('1014', '菜单新增', '102', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:add', '#', 103, 1, now(), null, null, ''),
    ('1015', '菜单修改', '102', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:edit', '#', 103, 1, now(), null, null, ''),
    ('1016', '菜单删除', '102', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 部门管理按钮
    ('1017', '部门查询', '103', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:query', '#', 103, 1, now(), null, null, ''),
    ('1018', '部门新增', '103', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:add', '#', 103, 1, now(), null, null, ''),
    ('1019', '部门修改', '103', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:edit', '#', 103, 1, now(), null, null, ''),
    ('1020', '部门删除', '103', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 岗位管理按钮
    ('1021', '岗位查询', '104', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:post:query', '#', 103, 1, now(), null, null, ''),
    ('1022', '岗位新增', '104', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:post:add', '#', 103, 1, now(), null, null, ''),
    ('1023', '岗位修改', '104', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:post:edit', '#', 103, 1, now(), null, null, ''),
    ('1024', '岗位删除', '104', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:post:remove', '#', 103, 1, now(), null, null, ''),
    ('1025', '岗位导出', '104', '5', '', '', '', '1', '0', 'F', '0', '0', 'system:post:export', '#', 103, 1, now(), null, null, ''),
    
    -- 字典管理按钮
    ('1026', '字典查询', '105', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:query', '#', 103, 1, now(), null, null, ''),
    ('1027', '字典新增', '105', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:add', '#', 103, 1, now(), null, null, ''),
    ('1028', '字典修改', '105', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:edit', '#', 103, 1, now(), null, null, ''),
    ('1029', '字典删除', '105', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:remove', '#', 103, 1, now(), null, null, ''),
    ('1030', '字典导出', '105', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:export', '#', 103, 1, now(), null, null, ''),
    
    -- 参数设置按钮
    ('1031', '参数查询', '106', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:query', '#', 103, 1, now(), null, null, ''),
    ('1032', '参数新增', '106', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:add', '#', 103, 1, now(), null, null, ''),
    ('1033', '参数修改', '106', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:edit', '#', 103, 1, now(), null, null, ''),
    ('1034', '参数删除', '106', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:remove', '#', 103, 1, now(), null, null, ''),
    ('1035', '参数导出', '106', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:export', '#', 103, 1, now(), null, null, ''),
    
    -- 通知公告按钮
    ('1036', '公告查询', '107', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:query', '#', 103, 1, now(), null, null, ''),
    ('1037', '公告新增', '107', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:add', '#', 103, 1, now(), null, null, ''),
    ('1038', '公告修改', '107', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:edit', '#', 103, 1, now(), null, null, ''),
    ('1039', '公告删除', '107', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 操作日志按钮
    ('1040', '操作查询', '500', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, now(), null, null, ''),
    ('1041', '操作删除', '500', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1, now(), null, null, ''),
    ('1042', '日志导出', '500', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1, now(), null, null, ''),
    
    -- 登录日志按钮
    ('1043', '登录查询', '501', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, now(), null, null, ''),
    ('1044', '登录删除', '501', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1, now(), null, null, ''),
    ('1045', '日志导出', '501', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1, now(), null, null, ''),
    ('1050', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1, now(), null, null, ''),
    
    -- 在线用户按钮
    ('1046', '在线查询', '109', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:query', '#', 103, 1, now(), null, null, ''),
    ('1047', '批量强退', '109', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, now(), null, null, ''),
    ('1048', '单条强退', '109', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, now(), null, null, ''),
    
    -- 代码生成按钮
    ('1055', '生成查询', '115', '1', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:query', '#', 103, 1, now(), null, null, ''),
    ('1056', '生成修改', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:edit', '#', 103, 1, now(), null, null, ''),
    ('1057', '生成删除', '115', '3', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:remove', '#', 103, 1, now(), null, null, ''),
    ('1058', '导入代码', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:import', '#', 103, 1, now(), null, null, ''),
    ('1059', '预览代码', '115', '4', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:preview', '#', 103, 1, now(), null, null, ''),
    ('1060', '生成代码', '115', '5', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:code', '#', 103, 1, now(), null, null, ''),
    
    -- 文件管理按钮
    ('1600', '文件查询', '118', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:query', '#', 103, 1, now(), null, null, ''),
    ('1601', '文件上传', '118', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:upload', '#', 103, 1, now(), null, null, ''),
    ('1602', '文件下载', '118', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:download', '#', 103, 1, now(), null, null, ''),
    ('1603', '文件删除', '118', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:remove', '#', 103, 1, now(), null, null, ''),
    ('1620', '配置列表', '118', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:list', '#', 103, 1, now(), null, null, ''),
    ('1621', '配置添加', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:add', '#', 103, 1, now(), null, null, ''),
    ('1622', '配置编辑', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:edit', '#', 103, 1, now(), null, null, ''),
    ('1623', '配置删除', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 客户端管理按钮
    ('1061', '客户端管理查询', '123', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:client:query', '#', 103, 1, now(), null, null, ''),
    ('1062', '客户端管理新增', '123', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:client:add', '#', 103, 1, now(), null, null, ''),
    ('1063', '客户端管理修改', '123', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:client:edit', '#', 103, 1, now(), null, null, ''),
    ('1064', '客户端管理删除', '123', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:client:remove', '#', 103, 1, now(), null, null, ''),
    ('1065', '客户端管理导出', '123', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:client:export', '#', 103, 1, now(), null, null, ''),
    
    -- ======================================================================
    -- AI管理模块 - 重新组织的层次结构
    -- ======================================================================
    
    -- 一级菜单：AI管理
    ('2000', 'AI管理', '0', '1', 'ai', null, '', '1', '0', 'M', '0', '0', '', 'robot', 103, 1, now(), null, null, 'AI模块根菜单'),
    
    -- ======================================================================
    -- 二级菜单：大模型管理
    -- ======================================================================
    ('2100', '大模型', '2000', '1', 'model', null, '', '1', '0', 'M', '0', '0', '', 'mdi-robot-outline', 103, 1, now(), null, null, '大模型管理目录'),
    
    -- 三级菜单：大模型下的具体功能
    ('2101', '模型管理', '2100', '1', 'model-manager', 'ai/model-manager/index', '', '1', '0', 'C', '0', '0', 'ai:model:list', 'mdi-robot', 103, 1, now(), null, null, '模型配置管理'),
    ('2102', 'MCP服务', '2100', '2', 'mcp-manager', 'ai/mcp-manager/index', '', '1', '0', 'C', '0', '0', 'ai:mcpServer:list', 'mdi-cloud-braces', 103, 1, now(), null, null, 'MCP Server管理'),
    ('2103', '工具管理', '2100', '3', 'tool-manager', 'ai/tool-manager/index', '', '1', '0', 'C', '0', '0', 'ai:builtinTool:list', 'mdi-hammer-wrench', 103, 1, now(), null, null, '内置Python工具管理'),
    ('2104', '技能管理', '2100', '4', 'skill-manager', 'ai/skill-manager/index', '', '1', '0', 'C', '0', '0', 'ai:skill:list', 'mdi-brain', 103, 1, now(), null, null, '技能抽象管理'),
    
    -- 大模型-模型管理按钮权限
    ('2111', '模型查询', '2101', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:model:query', '#', 103, 1, now(), null, null, ''),
    ('2112', '模型新增', '2101', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:model:add', '#', 103, 1, now(), null, null, ''),
    ('2113', '模型修改', '2101', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:model:edit', '#', 103, 1, now(), null, null, ''),
    ('2114', '模型删除', '2101', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:model:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 大模型-MCP服务按钮权限
    ('2121', 'MCP查询', '2102', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:mcpServer:query', '#', 103, 1, now(), null, null, ''),
    ('2122', 'MCP新增', '2102', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:mcpServer:add', '#', 103, 1, now(), null, null, ''),
    ('2123', 'MCP修改', '2102', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:mcpServer:edit', '#', 103, 1, now(), null, null, ''),
    ('2124', 'MCP删除', '2102', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:mcpServer:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 大模型-工具管理按钮权限
    ('2131', '工具查询', '2103', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:builtinTool:query', '#', 103, 1, now(), null, null, ''),
    ('2132', '工具新增', '2103', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:builtinTool:add', '#', 103, 1, now(), null, null, ''),
    ('2133', '工具修改', '2103', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:builtinTool:edit', '#', 103, 1, now(), null, null, ''),
    ('2134', '工具删除', '2103', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:builtinTool:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 大模型-技能管理按钮权限
    ('2141', '技能查询', '2104', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:skill:query', '#', 103, 1, now(), null, null, ''),
    ('2142', '技能新增', '2104', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:skill:add', '#', 103, 1, now(), null, null, ''),
    ('2143', '技能修改', '2104', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:skill:edit', '#', 103, 1, now(), null, null, ''),
    ('2144', '技能删除', '2104', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:skill:remove', '#', 103, 1, now(), null, null, ''),
    
    -- ======================================================================
    -- 二级菜单：知识库管理
    -- ======================================================================
    ('2200', '知识库', '2000', '2', 'knowledge', null, '', '1', '0', 'M', '0', '0', '', 'mdi-database', 103, 1, now(), null, null, '知识库管理目录'),
    
    -- 三级菜单：知识库下的具体功能
    ('2201', '知识库管理', '2200', '1', 'knowledge-manager', 'ai/knowledge-manager/index', '', '1', '0', 'C', '0', '0', 'ai:knowledge:list', 'mdi-database-outline', 103, 1, now(), null, null, '知识库列表管理'),
    ('2202', '知识库详情', '2200', '2', 'knowledge-detail', 'ai/knowledge-detail/index', '', '1', '1', 'C', '1', '0', 'ai:knowledge:view', 'mdi-database-search', 103, 1, now(), null, null, '知识库详情页面（隐藏）'),
    ('2203', '分块管理', '2200', '3', 'chunk-manager', 'ai/chunk-manager/index', '', '1', '1', 'C', '1', '0', 'ai:chunkManager:list', 'mdi-file-document-multiple', 103, 1, now(), null, null, '文档分块管理（隐藏）'),
    
    -- 知识库-知识库管理按钮权限
    ('2211', '知识库查询', '2201', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:knowledge:query', '#', 103, 1, now(), null, null, ''),
    ('2212', '知识库新增', '2201', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:knowledge:add', '#', 103, 1, now(), null, null, ''),
    ('2213', '知识库修改', '2201', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:knowledge:edit', '#', 103, 1, now(), null, null, ''),
    ('2214', '知识库删除', '2201', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:knowledge:remove', '#', 103, 1, now(), null, null, ''),
    ('2215', '知识库导出', '2201', '5', '', '', '', '1', '0', 'F', '0', '0', 'ai:knowledge:export', '#', 103, 1, now(), null, null, ''),
    
    -- ======================================================================
    -- 二级菜单：工作流管理
    -- ======================================================================
    ('2300', '工作流', '2000', '3', 'workflow', null, '', '1', '0', 'M', '0', '0', '', 'mdi-workflow', 103, 1, now(), null, null, '工作流管理目录'),
    
    -- 三级菜单：工作流下的具体功能
    ('2301', '工作流模板', '2300', '1', 'workflow-template', 'ai/workflow-template/index', '', '1', '0', 'C', '0', '0', 'ai:workflowTemplate:list', 'mdi-file-tree', 103, 1, now(), null, null, '工作流模板管理'),
    ('2302', '节点定义', '2300', '2', 'node-definition', 'ai/node-definition/index', '', '1', '1', 'C', '0', '0', 'ai:nodeDefinition:list', 'mdi-sitemap', 103, 1, now(), null, null, '工作流节点定义'),
    ('2303', '工作流编排', '2300', '3', 'workflow-editor', 'ai/workflow/index', '', '1', '1', 'C', '1', '0', 'ai:app:workflow', 'mdi-graph', 103, 1, now(), null, null, '工作流编排页面（隐藏）'),
    ('2304', '模板编排', '2300', '4', 'template-editor', 'ai/template-editor/index', '', '1', '1', 'C', '1', '0', 'ai:templateEditor:view', 'mdi-file-edit', 103, 1, now(), null, null, '模板工作流编排（隐藏）'),
    
    -- 工作流-工作流模板按钮权限
    ('2311', '模板查询', '2301', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:query', '#', 103, 1, now(), null, null, ''),
    ('2312', '模板新增', '2301', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:add', '#', 103, 1, now(), null, null, ''),
    ('2313', '模板修改', '2301', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:edit', '#', 103, 1, now(), null, null, ''),
    ('2314', '模板删除', '2301', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 工作流-节点定义按钮权限
    ('2321', '节点查询', '2302', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:node:query', '#', 103, 1, now(), null, null, ''),
    ('2322', '节点新增', '2302', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:node:add', '#', 103, 1, now(), null, null, ''),
    ('2323', '节点修改', '2302', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:node:edit', '#', 103, 1, now(), null, null, ''),
    ('2324', '节点删除', '2302', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:node:remove', '#', 103, 1, now(), null, null, ''),
    
    -- 工作流-工作流编排按钮权限
    ('2331', '工作流查询', '2303', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:list', '#', 103, 1, now(), null, null, ''),
    ('2332', '工作流保存', '2303', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:workflow:add,ai:workflow:edit', '#', 103, 1, now(), null, null, ''),
    
    -- ======================================================================
    -- 二级菜单：应用管理
    -- ======================================================================
    ('2400', '应用管理', '2000', '4', 'app', null, '', '1', '0', 'M', '0', '0', '', 'mdi-application', 103, 1, now(), null, null, '应用管理目录'),
    
    -- 三级菜单：应用管理下的具体功能
    ('2401', '应用列表', '2400', '1', 'app-manager', 'ai/app-manager/index', '', '1', '0', 'C', '0', '0', 'ai:app:list', 'mdi-apps', 103, 1, now(), null, null, 'AI应用列表管理'),
    ('2402', '应用详情', '2400', '2', 'app-detail', 'ai/app-detail/index', '', '1', '1', 'C', '1', '0', 'ai:appDetail:view', 'mdi-application-outline', 103, 1, now(), null, null, '应用详情页面（隐藏）'),
    ('2403', 'AI对话', '2400', '3', 'chat', 'ai/chat/index', '', '1', '1', 'C', '1', '0', 'ai:chat:view', 'mdi-chat', 103, 1, now(), null, null, 'AI聊天对话页面（隐藏）'),
    ('2404', '数据源管理', '2400', '4', 'datasource-manager', 'ai/datasource-manager/index', '', '1', '1', 'C', '0', '0', 'ai:datasourceManager:list', 'mdi-database-plus', 103, 1, now(), null, null, '数据源管理'),
    ('2405', '限流配置', '2400', '5', 'rateLimit', 'ai/rateLimit/index', '', '1', '0', 'C', '0', '0', 'ai:rateLimit:list', 'mdi-timer-sand', 103, 1, now(), null, null, 'AI对话频率与Token限制管理'),

    -- 应用管理-应用列表按钮权限
    ('2411', '应用查询', '2401', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:app:query', '#', 103, 1, now(), null, null, ''),
    ('2412', '应用新增', '2401', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:app:add', '#', 103, 1, now(), null, null, ''),
    ('2413', '应用修改', '2401', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:app:edit', '#', 103, 1, now(), null, null, ''),
    ('2414', '应用删除', '2401', '4', '', '', '', '1', '0', 'F', '0', '0', 'ai:app:remove', '#', 103, 1, now(), null, null, ''),
    ('2415', '应用导出', '2401', '5', '', '', '', '1', '0', 'F', '0', '0', 'ai:app:export', '#', 103, 1, now(), null, null, ''),
    
    -- 应用管理-AI对话按钮权限
    ('2421', '发送消息', '2403', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:chat:send', '#', 103, 1, now(), null, null, ''),
    ('2422', '查看历史', '2403', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:chat:history', '#', 103, 1, now(), null, null, ''),
    ('2423', '清空对话', '2403', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:chat:clear', '#', 103, 1, now(), null, null, '')

    -- 大模型-限流配置按钮权限
    ('2431', '限流查询', '2405', '1', '', '', '', '1', '0', 'F', '0', '0', 'ai:rateLimit:query', '#', 103, 1, now(), null, null, ''),
    ('2432', '限流修改', '2405', '2', '', '', '', '1', '0', 'F', '0', '0', 'ai:rateLimit:edit', '#', 103, 1, now(), null, null, ''),
    ('2433', '限流删除', '2405', '3', '', '', '', '1', '0', 'F', '0', '0', 'ai:rateLimit:remove', '#', 103, 1, now(), null, null, ''),

ON CONFLICT (menu_id) DO NOTHING;


-- sys_post
INSERT INTO sys_post (post_id, dept_id, post_code, post_category, post_name, post_sort, status, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (1, 103, 'ceo', null, '董事长', 1, '0', 103, 1, now(), null, null, ''),
    (2, 100, 'se', null, '项目经理', 2, '0', 103, 1, now(), null, null, ''),
    (3, 100, 'hr', null, '人力资源', 3, '0', 103, 1, now(), null, null, ''),
    (4, 100, 'user', null, '普通员工', 4, '0', 103, 1, now(), null, null, '')
ON CONFLICT (post_id) DO NOTHING;

-- sys_role
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    ('1', '超级管理员', 'superadmin', 1, '1', 't', 't', '0', '0', 103, 1, now(), null, null, '超级管理员'),
    ('3', '本部门及以下', 'test1', 3, '4', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, ''),
    ('4', '仅本人', 'test2', 4, '5', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, '')
ON CONFLICT (role_id) DO NOTHING;

-- sys_role_menu
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
    ('3', '1'),
    ('3', '5'),
    ('3', '100'),
    ('3', '101'),
    ('3', '102'),
    ('3', '103'),
    ('3', '104'),
    ('3', '105'),
    ('3', '106'),
    ('3', '107'),
    ('3', '108'),
    ('3', '118'),
    ('3', '123'),
    ('3', '500'),
    ('3', '501'),
    ('3', '1001'),
    ('3', '1002'),
    ('3', '1003'),
    ('3', '1004'),
    ('3', '1005'),
    ('3', '1006'),
    ('3', '1007'),
    ('3', '1008'),
    ('3', '1009'),
    ('3', '1010'),
    ('3', '1011'),
    ('3', '1012'),
    ('3', '1013'),
    ('3', '1014'),
    ('3', '1015'),
    ('3', '1016'),
    ('3', '1017'),
    ('3', '1018'),
    ('3', '1019'),
    ('3', '1020'),
    ('3', '1021'),
    ('3', '1022'),
    ('3', '1023'),
    ('3', '1024'),
    ('3', '1025'),
    ('3', '1026'),
    ('3', '1027'),
    ('3', '1028'),
    ('3', '1029'),
    ('3', '1030'),
    ('3', '1031'),
    ('3', '1032'),
    ('3', '1033'),
    ('3', '1034'),
    ('3', '1035'),
    ('3', '1036'),
    ('3', '1037'),
    ('3', '1038'),
    ('3', '1039'),
    ('3', '1040'),
    ('3', '1041'),
    ('3', '1042'),
    ('3', '1043'),
    ('3', '1044'),
    ('3', '1045'),
    ('3', '1050'),
    ('3', '1061'),
    ('3', '1062'),
    ('3', '1063'),
    ('3', '1064'),
    ('3', '1065'),
    ('3', '1500'),
    ('3', '1501'),
    ('3', '1502'),
    ('3', '1503'),
    ('3', '1504'),
    ('3', '1505'),
    ('3', '1506'),
    ('3', '1507'),
    ('3', '1508'),
    ('3', '1509'),
    ('3', '1510'),
    ('3', '1511'),
    ('3', '1600'),
    ('3', '1601'),
    ('3', '1602'),
    ('3', '1603'),
    ('3', '1620'),
    ('3', '1621'),
    ('3', '1622'),
    ('3', '1623'),
    ('3', '11616'),
    ('3', '11618'),
    ('3', '11619'),
    ('3', '11622'),
    ('3', '11623'),
    ('3', '11629'),
    ('3', '11632'),
    ('3', '11633'),
    ('3', '11638'),
    ('3', '11639'),
    ('3', '11640'),
    ('3', '11641'),
    ('3', '11642'),
    ('3', '11643'),
    ('4', '5'),
    ('4', '1500'),
    ('4', '1501'),
    ('4', '1502'),
    ('4', '1503'),
    ('4', '1504'),
    ('4', '1505'),
    ('4', '1506'),
    ('4', '1507'),
    ('4', '1508'),
    ('4', '1509'),
    ('4', '1510'),
    ('4', '1511')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- sys_user
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark, rate_limit_config) 
VALUES
    (1, 103, 'admin', 'keyi', 'sys_user', 'hnliuwx@gmail.com', '13888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, '管理员', NULL),
    (3, 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), 3, NOW(), NULL, NULL),
    (4, 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), 4, NOW(), NULL, NULL)
ON CONFLICT (user_id) DO NOTHING;

-- sys_user_role
INSERT INTO sys_user_role (user_id, role_id)
VALUES
    ('1', '1'),
    ('3', '3'),
    ('4', '4')
ON CONFLICT (user_id, role_id) DO NOTHING;


INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    15, 'FILE_STORAGE', '文件存储', 'mdi-file-upload-outline', '#10B981', 
    'action', '上传图片或语音附件，并返回OSS ID供下游使用', '0', '1', 
    '1', '0', 
    '[]', 
    '[{"key": "ossIds", "type": "array", "label": "文件ID集合", "required": true, "description": "上传到系统的OSS ID列表"}]', 
    1, NOW(), NOW()
) ON CONFLICT (node_def_id) DO NOTHING;

INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    16, 'AUDIO_ASR', '语音转录', 'mdi-microphone', '#3B82F6', 
    'ai', '调用ASR模型将音频文件转录为文字', '0', '1', 
    '0', '0', 
    '[{"key": "ossId", "type": "string", "label": "音频附件ID", "required": true, "description": "上游传入的音频OSS ID"}]', 
    '[{"key": "transcription", "type": "string", "label": "转录文本", "required": true, "description": "语音转录后的文字内容"}]', 
    1, NOW(), NOW()
) ON CONFLICT (node_def_id) DO NOTHING;

INSERT INTO km_node_definition (
    node_def_id, node_type, node_label, node_icon, node_color, 
    category, description, is_system, is_enabled, 
    allow_custom_input_params, allow_custom_output_params, 
    input_params, output_params, version, create_time, update_time
) VALUES (
    17, 'IMAGE_OCR', '图像识别', 'mdi-text-recognition', '#F59E0B', 
    'ai', '调用视觉大模型或OCR服务识别图像中的内容', '0', '1', 
    '0', '0', 
    '[{"key": "ossId", "type": "string", "label": "图片附件ID", "required": true, "description": "上游传入的图片OSS ID"}]', 
    '[{"key": "text", "type": "string", "label": "识别结果", "required": true, "description": "图像识别出来的文本"}]', 
    1, NOW(), NOW()
) ON CONFLICT (node_def_id) DO NOTHING;

-- 插入 km_node_connection_rule 默认连接规则
-- START -> FILE_STORAGE, AUDIO_ASR, IMAGE_OCR
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, is_enabled, create_time) VALUES
(200, 'START', 'FILE_STORAGE', '0', 10, '1', NOW()),
(201, 'START', 'AUDIO_ASR', '0', 10, '1', NOW()),
(202, 'START', 'IMAGE_OCR', '0', 10, '1', NOW()),

-- FILE_STORAGE -> LLM_CHAT, AUDIO_ASR, IMAGE_OCR, CONDITION, END
(203, 'FILE_STORAGE', 'LLM_CHAT', '0', 10, '1', NOW()),
(204, 'FILE_STORAGE', 'AUDIO_ASR', '0', 10, '1', NOW()),
(205, 'FILE_STORAGE', 'IMAGE_OCR', '0', 10, '1', NOW()),
(206, 'FILE_STORAGE', 'CONDITION', '0', 10, '1', NOW()),
(207, 'FILE_STORAGE', 'END', '0', 10, '1', NOW()),

-- AUDIO_ASR -> LLM_CHAT, CONDITION, END
(208, 'AUDIO_ASR', 'LLM_CHAT', '0', 10, '1', NOW()),
(209, 'AUDIO_ASR', 'CONDITION', '0', 10, '1', NOW()),
(210, 'AUDIO_ASR', 'END', '0', 10, '1', NOW()),

-- IMAGE_OCR -> LLM_CHAT, CONDITION, END
(211, 'IMAGE_OCR', 'LLM_CHAT', '0', 10, '1', NOW()),
(212, 'IMAGE_OCR', 'CONDITION', '0', 10, '1', NOW()),
(213, 'IMAGE_OCR', 'END', '0', 10, '1', NOW())
ON CONFLICT (rule_id) DO NOTHING;
