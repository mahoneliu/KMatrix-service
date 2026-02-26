-- ======================================================================
-- KMatrix 数据库初始化脚本
-- PostgreSQL 17+
-- 生成时间: 2026-02-09 16:15:39
-- ======================================================================


-- ======================================================================
-- 第一部分: 扩展定义
-- ======================================================================
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_jieba; 


CREATE FUNCTION cast_varchar_to_timestamp(character varying) RETURNS timestamp with time zone
    LANGUAGE sql STRICT
    AS $_$
select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');
$_$;
-- ======================================================================
-- 第二部分: 表结构定义
-- ======================================================================

-- ----------------------------
-- 第三方平台授权表
-- ----------------------------

create table sys_social
(
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

comment on table   sys_social                   is '社会化关系表';
comment on column  sys_social.id                is '主键';
comment on column  sys_social.user_id           is '用户ID';
comment on column  sys_social.auth_id           is '平台+平台唯一id';
comment on column  sys_social.source            is '用户来源';
comment on column  sys_social.open_id           is '平台编号唯一id';
comment on column  sys_social.user_name         is '登录账号';
comment on column  sys_social.nick_name         is '用户昵称';
comment on column  sys_social.email             is '用户邮箱';
comment on column  sys_social.avatar            is '头像地址';
comment on column  sys_social.access_token      is '用户的授权令牌';
comment on column  sys_social.expire_in         is '用户的授权令牌的有效期，部分平台可能没有';
comment on column  sys_social.refresh_token     is '刷新令牌，部分平台可能没有';
comment on column  sys_social.access_code       is '平台的授权信息，部分平台可能没有';
comment on column  sys_social.union_id          is '用户的 unionid';
comment on column  sys_social.scope             is '授予的权限，部分平台可能没有';
comment on column  sys_social.token_type        is '个别平台的授权信息，部分平台可能没有';
comment on column  sys_social.id_token          is 'id token，部分平台可能没有';
comment on column  sys_social.mac_algorithm     is '小米平台用户的附带属性，部分平台可能没有';
comment on column  sys_social.mac_key           is '小米平台用户的附带属性，部分平台可能没有';
comment on column  sys_social.code              is '用户的授权code，部分平台可能没有';
comment on column  sys_social.oauth_token       is 'Twitter平台用户的附带属性，部分平台可能没有';
comment on column  sys_social.oauth_token_secret is 'Twitter平台用户的附带属性，部分平台可能没有';
comment on column  sys_social.create_dept       is '创建部门';
comment on column  sys_social.create_by         is '创建者';
comment on column  sys_social.create_time       is '创建时间';
comment on column  sys_social.update_by         is '更新者';
comment on column  sys_social.update_time       is '更新时间';
comment on column  sys_social.del_flag          is '删除标志（0代表存在 1代表删除）';



-- ----------------------------
-- 1、部门表
-- ----------------------------

create table if not exists sys_dept
(
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

comment on table sys_dept               is '部门表';
comment on column sys_dept.dept_id      is '部门ID';
comment on column sys_dept.parent_id    is '父部门ID';
comment on column sys_dept.ancestors    is '祖级列表';
comment on column sys_dept.dept_name    is '部门名称';
comment on column sys_dept.dept_category    is '部门类别编码';
comment on column sys_dept.order_num    is '显示顺序';
comment on column sys_dept.leader       is '负责人';
comment on column sys_dept.phone        is '联系电话';
comment on column sys_dept.email        is '邮箱';
comment on column sys_dept.status       is '部门状态（0正常 1停用）';
comment on column sys_dept.del_flag     is '删除标志（0代表存在 1代表删除）';
comment on column sys_dept.create_dept  is '创建部门';
comment on column sys_dept.create_by    is '创建者';
comment on column sys_dept.create_time  is '创建时间';
comment on column sys_dept.update_by    is '更新者';
comment on column sys_dept.update_time  is '更新时间';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------

create table if not exists sys_user
(
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
    constraint "sys_user_pk" primary key (user_id)
);

comment on table sys_user               is '用户信息表';
comment on column sys_user.user_id      is '用户ID';
comment on column sys_user.dept_id      is '部门ID';
comment on column sys_user.user_name    is '用户账号';
comment on column sys_user.nick_name    is '用户昵称';
comment on column sys_user.user_type    is '用户类型（sys_user系统用户）';
comment on column sys_user.email        is '用户邮箱';
comment on column sys_user.phonenumber  is '手机号码';
comment on column sys_user.sex          is '用户性别（0男 1女 2未知）';
comment on column sys_user.avatar       is '头像地址';
comment on column sys_user.password     is '密码';
comment on column sys_user.status       is '帐号状态（0正常 1停用）';
comment on column sys_user.del_flag     is '删除标志（0代表存在 1代表删除）';
comment on column sys_user.login_ip     is '最后登陆IP';
comment on column sys_user.login_date   is '最后登陆时间';
comment on column sys_user.create_dept  is '创建部门';
comment on column sys_user.create_by    is '创建者';
comment on column sys_user.create_time  is '创建时间';
comment on column sys_user.update_by    is '更新者';
comment on column sys_user.update_time  is '更新时间';
comment on column sys_user.remark       is '备注';

-- ----------------------------

-- 初始化-用户信息表数据
-- ----------------------------

create table if not exists sys_post
(
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

comment on table sys_post               is '岗位信息表';
comment on column sys_post.post_id      is '岗位ID';
comment on column sys_post.dept_id      is '部门id';
comment on column sys_post.post_code    is '岗位编码';
comment on column sys_post.post_category is '岗位类别编码';
comment on column sys_post.post_name    is '岗位名称';
comment on column sys_post.post_sort    is '显示顺序';
comment on column sys_post.status       is '状态（0正常 1停用）';
comment on column sys_post.create_dept  is '创建部门';
comment on column sys_post.create_by    is '创建者';
comment on column sys_post.create_time  is '创建时间';
comment on column sys_post.update_by    is '更新者';
comment on column sys_post.update_time  is '更新时间';
comment on column sys_post.remark       is '备注';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------

create table if not exists sys_role
(
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

comment on table sys_role                       is '角色信息表';
comment on column sys_role.role_id              is '角色ID';
comment on column sys_role.role_name            is '角色名称';
comment on column sys_role.role_key             is '角色权限字符串';
comment on column sys_role.role_sort            is '显示顺序';
comment on column sys_role.data_scope           is '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
comment on column sys_role.menu_check_strictly  is '菜单树选择项是否关联显示';
comment on column sys_role.dept_check_strictly  is '部门树选择项是否关联显示';
comment on column sys_role.status               is '角色状态（0正常 1停用）';
comment on column sys_role.del_flag             is '删除标志（0代表存在 1代表删除）';
comment on column sys_role.create_dept          is '创建部门';
comment on column sys_role.create_by            is '创建者';
comment on column sys_role.create_time          is '创建时间';
comment on column sys_role.update_by            is '更新者';
comment on column sys_role.update_time          is '更新时间';
comment on column sys_role.remark               is '备注';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------

create table if not exists sys_menu
(
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

comment on table sys_menu               is '菜单权限表';
comment on column sys_menu.menu_id      is '菜单ID';
comment on column sys_menu.menu_name    is '菜单名称';
comment on column sys_menu.parent_id    is '父菜单ID';
comment on column sys_menu.order_num    is '显示顺序';
comment on column sys_menu.path         is '路由地址';
comment on column sys_menu.component    is '组件路径';
comment on column sys_menu.query_param  is '路由参数';
comment on column sys_menu.is_frame     is '是否为外链（0是 1否）';
comment on column sys_menu.is_cache     is '是否缓存（0缓存 1不缓存）';
comment on column sys_menu.menu_type    is '菜单类型（M目录 C菜单 F按钮）';
comment on column sys_menu.visible      is '显示状态（0显示 1隐藏）';
comment on column sys_menu.status       is '菜单状态（0正常 1停用）';
comment on column sys_menu.perms        is '权限标识';
comment on column sys_menu.icon         is '菜单图标';
comment on column sys_menu.create_dept  is '创建部门';
comment on column sys_menu.create_by    is '创建者';
comment on column sys_menu.create_time  is '创建时间';
comment on column sys_menu.update_by    is '更新者';
comment on column sys_menu.update_time  is '更新时间';
comment on column sys_menu.remark       is '备注';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单

create table if not exists sys_user_role
(
    user_id int8 not null,
    role_id int8 not null,
    constraint sys_user_role_pk primary key (user_id, role_id)
);

comment on table sys_user_role              is '用户和角色关联表';
comment on column sys_user_role.user_id     is '用户ID';
comment on column sys_user_role.role_id     is '角色ID';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------

create table if not exists sys_role_menu
(
    role_id int8 not null,
    menu_id int8 not null,
    constraint sys_role_menu_pk primary key (role_id, menu_id)
);

comment on table sys_role_menu              is '角色和菜单关联表';
comment on column sys_role_menu.role_id     is '角色ID';
comment on column sys_role_menu.menu_id     is '菜单ID';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------

create table if not exists sys_role_dept
(
    role_id int8 not null,
    dept_id int8 not null,
    constraint sys_role_dept_pk primary key (role_id, dept_id)
);

comment on table sys_role_dept              is '角色和部门关联表';
comment on column sys_role_dept.role_id     is '角色ID';
comment on column sys_role_dept.dept_id     is '部门ID';


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------

create table if not exists sys_user_post
(
    user_id int8 not null,
    post_id int8 not null,
    constraint sys_user_post_pk primary key (user_id, post_id)
);

comment on table sys_user_post              is '用户与岗位关联表';
comment on column sys_user_post.user_id     is '用户ID';
comment on column sys_user_post.post_id     is '岗位ID';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------

create table if not exists sys_oper_log
(
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

create index idx_sys_oper_log_bt ON sys_oper_log (business_type);
create index idx_sys_oper_log_s ON sys_oper_log (status);
create index idx_sys_oper_log_ot ON sys_oper_log (oper_time);

comment on table sys_oper_log                   is '操作日志记录';
comment on column sys_oper_log.oper_id          is '日志主键';
comment on column sys_oper_log.title            is '模块标题';
comment on column sys_oper_log.business_type    is '业务类型（0其它 1新增 2修改 3删除）';
comment on column sys_oper_log.method           is '方法名称';
comment on column sys_oper_log.request_method   is '请求方式';
comment on column sys_oper_log.operator_type    is '操作类别（0其它 1后台用户 2手机端用户）';
comment on column sys_oper_log.oper_name        is '操作人员';
comment on column sys_oper_log.dept_name        is '部门名称';
comment on column sys_oper_log.oper_url         is '请求URL';
comment on column sys_oper_log.oper_ip          is '主机地址';
comment on column sys_oper_log.oper_location    is '操作地点';
comment on column sys_oper_log.oper_param       is '请求参数';
comment on column sys_oper_log.json_result      is '返回参数';
comment on column sys_oper_log.status           is '操作状态（0正常 1异常）';
comment on column sys_oper_log.error_msg        is '错误消息';
comment on column sys_oper_log.oper_time        is '操作时间';
comment on column sys_oper_log.cost_time        is '消耗时间';

-- ----------------------------
-- 11、字典类型表
-- ----------------------------

create table if not exists sys_dict_type
(
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

create unique index sys_dict_type_index1 ON sys_dict_type (dict_type);

comment on table sys_dict_type                  is '字典类型表';
comment on column sys_dict_type.dict_id         is '字典主键';
comment on column sys_dict_type.dict_name       is '字典名称';
comment on column sys_dict_type.dict_type       is '字典类型';
comment on column sys_dict_type.create_dept     is '创建部门';
comment on column sys_dict_type.create_by       is '创建者';
comment on column sys_dict_type.create_time     is '创建时间';
comment on column sys_dict_type.update_by       is '更新者';
comment on column sys_dict_type.update_time     is '更新时间';
comment on column sys_dict_type.remark          is '备注';


create table if not exists sys_dict_data
(
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

comment on table sys_dict_data                  is '字典数据表';
comment on column sys_dict_data.dict_code       is '字典编码';
comment on column sys_dict_data.dict_sort       is '字典排序';
comment on column sys_dict_data.dict_label      is '字典标签';
comment on column sys_dict_data.dict_value      is '字典键值';
comment on column sys_dict_data.dict_type       is '字典类型';
comment on column sys_dict_data.css_class       is '样式属性（其他样式扩展）';
comment on column sys_dict_data.list_class      is '表格回显样式';
comment on column sys_dict_data.is_default      is '是否默认（Y是 N否）';
comment on column sys_dict_data.create_dept     is '创建部门';
comment on column sys_dict_data.create_by       is '创建者';
comment on column sys_dict_data.create_time     is '创建时间';
comment on column sys_dict_data.update_by       is '更新者';
comment on column sys_dict_data.update_time     is '更新时间';
comment on column sys_dict_data.remark          is '备注';


create table if not exists sys_config
(
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

comment on table sys_config                 is '参数配置表';
comment on column sys_config.config_id      is '参数主键';
comment on column sys_config.config_name    is '参数名称';
comment on column sys_config.config_key     is '参数键名';
comment on column sys_config.config_value   is '参数键值';
comment on column sys_config.config_type    is '系统内置（Y是 N否）';
comment on column sys_config.create_dept    is '创建部门';
comment on column sys_config.create_by      is '创建者';
comment on column sys_config.create_time    is '创建时间';
comment on column sys_config.update_by      is '更新者';
comment on column sys_config.update_time    is '更新时间';
comment on column sys_config.remark         is '备注';


create table if not exists sys_logininfor
(
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

create index idx_sys_logininfor_s ON sys_logininfor (status);
create index idx_sys_logininfor_lt ON sys_logininfor (login_time);

comment on table sys_logininfor                 is '系统访问记录';
comment on column sys_logininfor.info_id        is '访问ID';
comment on column sys_logininfor.user_name      is '用户账号';
comment on column sys_logininfor.client_key     is '客户端';
comment on column sys_logininfor.device_type    is '设备类型';
comment on column sys_logininfor.ipaddr         is '登录IP地址';
comment on column sys_logininfor.login_location is '登录地点';
comment on column sys_logininfor.browser        is '浏览器类型';
comment on column sys_logininfor.os             is '操作系统';
comment on column sys_logininfor.status         is '登录状态（0成功 1失败）';
comment on column sys_logininfor.msg            is '提示消息';
comment on column sys_logininfor.login_time     is '访问时间';

-- ----------------------------
-- 17、通知公告表
-- ----------------------------

create table if not exists sys_notice
(
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

comment on table sys_notice                 is '通知公告表';
comment on column sys_notice.notice_id      is '公告ID';
comment on column sys_notice.notice_title   is '公告标题';
comment on column sys_notice.notice_type    is '公告类型（1通知 2公告）';
comment on column sys_notice.notice_content is '公告内容';
comment on column sys_notice.status         is '公告状态（0正常 1关闭）';
comment on column sys_notice.create_dept    is '创建部门';
comment on column sys_notice.create_by      is '创建者';
comment on column sys_notice.create_time    is '创建时间';
comment on column sys_notice.update_by      is '更新者';
comment on column sys_notice.update_time    is '更新时间';
comment on column sys_notice.remark         is '备注';

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------

create table if not exists gen_table
(
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

comment on table gen_table is '代码生成业务表';
comment on column gen_table.table_id is '编号';
comment on column gen_table.data_name is '数据源名称';
comment on column gen_table.table_name is '表名称';
comment on column gen_table.table_comment is '表描述';
comment on column gen_table.sub_table_name is '关联子表的表名';
comment on column gen_table.sub_table_fk_name is '子表关联的外键名';
comment on column gen_table.class_name is '实体类名称';
comment on column gen_table.tpl_category is '使用的模板（CRUD单表操作 TREE树表操作）';
comment on column gen_table.package_name is '生成包路径';
comment on column gen_table.module_name is '生成模块名';
comment on column gen_table.business_name is '生成业务名';
comment on column gen_table.function_name is '生成功能名';
comment on column gen_table.function_author is '生成功能作者';
comment on column gen_table.gen_type is '生成代码方式（0zip压缩包 1自定义路径）';
comment on column gen_table.gen_path is '生成路径（不填默认项目路径）';
comment on column gen_table.options is '其它生成选项';
comment on column gen_table.create_dept is '创建部门';
comment on column gen_table.create_by is '创建者';
comment on column gen_table.create_time is '创建时间';
comment on column gen_table.update_by is '更新者';
comment on column gen_table.update_time is '更新时间';
comment on column gen_table.remark is '备注';

-- ----------------------------
-- 19、代码生成业务表字段
-- ----------------------------

create table if not exists gen_table_column
(
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

comment on table gen_table_column is '代码生成业务表字段';
comment on column gen_table_column.column_id is '编号';
comment on column gen_table_column.table_id is '归属表编号';
comment on column gen_table_column.column_name is '列名称';
comment on column gen_table_column.column_comment is '列描述';
comment on column gen_table_column.column_type is '列类型';
comment on column gen_table_column.java_type is 'JAVA类型';
comment on column gen_table_column.java_field is 'JAVA字段名';
comment on column gen_table_column.is_pk is '是否主键（1是）';
comment on column gen_table_column.is_increment is '是否自增（1是）';
comment on column gen_table_column.is_required is '是否必填（1是）';
comment on column gen_table_column.is_insert is '是否为插入字段（1是）';
comment on column gen_table_column.is_edit is '是否编辑字段（1是）';
comment on column gen_table_column.is_list is '是否列表字段（1是）';
comment on column gen_table_column.is_query is '是否查询字段（1是）';
comment on column gen_table_column.query_type is '查询方式（等于、不等于、大于、小于、范围）';
comment on column gen_table_column.html_type is '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）';
comment on column gen_table_column.dict_type is '字典类型';
comment on column gen_table_column.sort is '排序';
comment on column gen_table_column.create_dept is '创建部门';
comment on column gen_table_column.create_by is '创建者';
comment on column gen_table_column.create_time is '创建时间';
comment on column gen_table_column.update_by is '更新者';
comment on column gen_table_column.update_time is '更新时间';

-- ----------------------------
-- OSS对象存储表
-- ----------------------------

create table if not exists sys_oss
(
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

comment on table sys_oss                    is 'OSS对象存储表';
comment on column sys_oss.oss_id            is '对象存储主键';
comment on column sys_oss.file_name         is '文件名';
comment on column sys_oss.original_name     is '原名';
comment on column sys_oss.file_suffix       is '文件后缀名';
comment on column sys_oss.url               is 'URL地址';
comment on column sys_oss.ext1              is '扩展字段';
comment on column sys_oss.create_by         is '上传人';
comment on column sys_oss.create_dept       is '创建部门';
comment on column sys_oss.create_time       is '创建时间';
comment on column sys_oss.update_by         is '更新者';
comment on column sys_oss.update_time       is '更新时间';
comment on column sys_oss.service           is '服务商';

-- ----------------------------
-- OSS对象存储动态配置表
-- ----------------------------

create table if not exists sys_oss_config
(
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

comment on table sys_oss_config                 is '对象存储配置表';
comment on column sys_oss_config.oss_config_id  is '主键';
comment on column sys_oss_config.config_key     is '配置key';
comment on column sys_oss_config.access_key     is 'accessKey';
comment on column sys_oss_config.secret_key     is '秘钥';
comment on column sys_oss_config.bucket_name    is '桶名称';
comment on column sys_oss_config.prefix         is '前缀';
comment on column sys_oss_config.endpoint       is '访问站点';
comment on column sys_oss_config.domain         is '自定义域名';
comment on column sys_oss_config.is_https       is '是否https（Y=是,N=否）';
comment on column sys_oss_config.region         is '域';
comment on column sys_oss_config.access_policy  is '桶权限类型(0=private 1=public 2=custom)';
comment on column sys_oss_config.status         is '是否默认（0=是,1=否）';
comment on column sys_oss_config.ext1           is '扩展字段';
comment on column sys_oss_config.create_dept    is '创建部门';
comment on column sys_oss_config.create_by      is '创建者';
comment on column sys_oss_config.create_time    is '创建时间';
comment on column sys_oss_config.update_by      is '更新者';
comment on column sys_oss_config.update_time    is '更新时间';
comment on column sys_oss_config.remark         is '备注';


create table sys_client (
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

comment on table sys_client                         is '系统授权表';
comment on column sys_client.id                     is '主键';
comment on column sys_client.client_id              is '客户端id';
comment on column sys_client.client_key             is '客户端key';
comment on column sys_client.client_secret          is '客户端秘钥';
comment on column sys_client.grant_type             is '授权类型';
comment on column sys_client.device_type            is '设备类型';
comment on column sys_client.active_timeout         is 'token活跃超时时间';
comment on column sys_client.timeout                is 'token固定超时';
comment on column sys_client.status                 is '状态（0正常 1停用）';
comment on column sys_client.del_flag               is '删除标志（0代表存在 1代表删除）';
comment on column sys_client.create_dept            is '创建部门';
comment on column sys_client.create_by              is '创建者';
comment on column sys_client.create_time            is '创建时间';
comment on column sys_client.update_by              is '更新者';
comment on column sys_client.update_time            is '更新时间';


create table if not exists test_demo
(
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

comment on table test_demo is '测试单表';
comment on column test_demo.id is '主键';
comment on column test_demo.dept_id is '部门id';
comment on column test_demo.user_id is '用户id';
comment on column test_demo.order_num is '排序号';
comment on column test_demo.test_key is 'key键';
comment on column test_demo.value is '值';
comment on column test_demo.version is '版本';
comment on column test_demo.create_dept  is '创建部门';
comment on column test_demo.create_time is '创建时间';
comment on column test_demo.create_by is '创建人';
comment on column test_demo.update_time is '更新时间';
comment on column test_demo.update_by is '更新人';
comment on column test_demo.del_flag is '删除标志';


create table if not exists test_tree
(
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

comment on table test_tree is '测试树表';
comment on column test_tree.id is '主键';
comment on column test_tree.parent_id is '父id';
comment on column test_tree.dept_id is '部门id';
comment on column test_tree.user_id is '用户id';
comment on column test_tree.tree_name is '值';
comment on column test_tree.version is '版本';
comment on column test_tree.create_dept  is '创建部门';
comment on column test_tree.create_time is '创建时间';
comment on column test_tree.create_by is '创建人';
comment on column test_tree.update_time is '更新时间';
comment on column test_tree.update_by is '更新人';
comment on column test_tree.del_flag is '删除标志';

-- sj_namespace

CREATE TABLE sj_namespace
(
    id          bigserial PRIMARY KEY,
    name        varchar(64)  NOT NULL,
    unique_id   varchar(64)  NOT NULL,
    description varchar(256) NOT NULL DEFAULT '',
    deleted     smallint     NOT NULL DEFAULT 0,
    create_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_namespace_01 ON sj_namespace (name);

COMMENT ON COLUMN sj_namespace.id IS '主键';
COMMENT ON COLUMN sj_namespace.name IS '名称';
COMMENT ON COLUMN sj_namespace.unique_id IS '唯一id';
COMMENT ON COLUMN sj_namespace.description IS '描述';
COMMENT ON COLUMN sj_namespace.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_namespace.create_dt IS '创建时间';
COMMENT ON COLUMN sj_namespace.update_dt IS '修改时间';
COMMENT ON TABLE sj_namespace IS '命名空间';


CREATE TABLE sj_group_config
(
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

CREATE UNIQUE INDEX uk_sj_group_config_01 ON sj_group_config (namespace_id, group_name);

COMMENT ON COLUMN sj_group_config.id IS '主键';
COMMENT ON COLUMN sj_group_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_group_config.group_name IS '组名称';
COMMENT ON COLUMN sj_group_config.description IS '组描述';
COMMENT ON COLUMN sj_group_config.token IS 'token';
COMMENT ON COLUMN sj_group_config.group_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_group_config.version IS '版本号';
COMMENT ON COLUMN sj_group_config.group_partition IS '分区';
COMMENT ON COLUMN sj_group_config.id_generator_mode IS '唯一id生成模式 默认号段模式';
COMMENT ON COLUMN sj_group_config.init_scene IS '是否初始化场景 0:否 1:是';
COMMENT ON COLUMN sj_group_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_group_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_group_config IS '组配置';


CREATE TABLE sj_notify_config
(
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

CREATE INDEX idx_sj_notify_config_01 ON sj_notify_config (namespace_id, group_name);

COMMENT ON COLUMN sj_notify_config.id IS '主键';
COMMENT ON COLUMN sj_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_config.group_name IS '组名称';
COMMENT ON COLUMN sj_notify_config.notify_name IS '通知名称';
COMMENT ON COLUMN sj_notify_config.system_task_type IS '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.recipient_ids IS '接收人id列表';
COMMENT ON COLUMN sj_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN sj_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN sj_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN sj_notify_config.description IS '描述';
COMMENT ON COLUMN sj_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_config IS '通知配置';

-- sj_notify_recipient

CREATE TABLE sj_notify_recipient
(
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    recipient_name   varchar(64)  NOT NULL,
    notify_type      smallint     NOT NULL DEFAULT 0,
    notify_attribute varchar(512) NOT NULL,
    description      varchar(256) NOT NULL DEFAULT '',
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_notify_recipient_01 ON sj_notify_recipient (namespace_id);

COMMENT ON COLUMN sj_notify_recipient.id IS '主键';
COMMENT ON COLUMN sj_notify_recipient.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_recipient.recipient_name IS '接收人名称';
COMMENT ON COLUMN sj_notify_recipient.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook';
COMMENT ON COLUMN sj_notify_recipient.notify_attribute IS '配置属性';
COMMENT ON COLUMN sj_notify_recipient.description IS '描述';
COMMENT ON COLUMN sj_notify_recipient.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_recipient.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_recipient IS '告警通知接收人';

-- sj_retry_dead_letter

CREATE TABLE sj_retry_dead_letter
(
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

CREATE INDEX idx_sj_retry_dead_letter_01 ON sj_retry_dead_letter (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_dead_letter_02 ON sj_retry_dead_letter (idempotent_id);
CREATE INDEX idx_sj_retry_dead_letter_03 ON sj_retry_dead_letter (biz_no);
CREATE INDEX idx_sj_retry_dead_letter_04 ON sj_retry_dead_letter (create_dt);

COMMENT ON COLUMN sj_retry_dead_letter.id IS '主键';
COMMENT ON COLUMN sj_retry_dead_letter.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_dead_letter.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_dead_letter.group_id IS '组Id';
COMMENT ON COLUMN sj_retry_dead_letter.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_dead_letter.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry_dead_letter.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_dead_letter.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_dead_letter.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_dead_letter.serializer_name IS '执行方法参数序列化器名称';
COMMENT ON COLUMN sj_retry_dead_letter.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_dead_letter.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_dead_letter.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_dead_letter IS '死信队列表';

-- sj_retry

CREATE TABLE sj_retry
(
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

CREATE UNIQUE INDEX uk_sj_retry_01 ON sj_retry (scene_id, task_type, idempotent_id, deleted);

CREATE INDEX idx_sj_retry_01 ON sj_retry (biz_no);
CREATE INDEX idx_sj_retry_02 ON sj_retry (idempotent_id);
CREATE INDEX idx_sj_retry_03 ON sj_retry (retry_status, bucket_index);
CREATE INDEX idx_sj_retry_04 ON sj_retry (parent_id);
CREATE INDEX idx_sj_retry_05 ON sj_retry (create_dt);

COMMENT ON COLUMN sj_retry.id IS '主键';
COMMENT ON COLUMN sj_retry.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry.group_name IS '组名称';
COMMENT ON COLUMN sj_retry.group_id IS '组Id';
COMMENT ON COLUMN sj_retry.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry.serializer_name IS '执行方法参数序列化器名称';
COMMENT ON COLUMN sj_retry.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_retry.retry_count IS '重试次数';
COMMENT ON COLUMN sj_retry.retry_status IS '重试状态 0、重试中 1、成功 2、最大重试次数';
COMMENT ON COLUMN sj_retry.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_retry.parent_id IS '父节点id';
COMMENT ON COLUMN sj_retry.deleted IS '逻辑删除';
COMMENT ON COLUMN sj_retry.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry IS '重试信息表';

-- sj_retry_task

CREATE TABLE sj_retry_task
(
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

CREATE INDEX idx_sj_retry_task_01 ON sj_retry_task (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_task_02 ON sj_retry_task (task_status);
CREATE INDEX idx_sj_retry_task_03 ON sj_retry_task (create_dt);
CREATE INDEX idx_sj_retry_task_04 ON sj_retry_task (retry_id);

COMMENT ON COLUMN sj_retry_task.id IS '主键';
COMMENT ON COLUMN sj_retry_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_task.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_task.task_status IS '重试状态';
COMMENT ON COLUMN sj_retry_task.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_task.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_retry_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_retry_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_task IS '重试任务表';

-- sj_retry_task_log_message

CREATE TABLE sj_retry_task_log_message
(
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

CREATE INDEX idx_sj_retry_task_log_message_01 ON sj_retry_task_log_message (namespace_id, group_name, retry_task_id);
CREATE INDEX idx_sj_retry_task_log_message_02 ON sj_retry_task_log_message (create_dt);

COMMENT ON COLUMN sj_retry_task_log_message.id IS '主键';
COMMENT ON COLUMN sj_retry_task_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_log_message.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task_log_message.retry_task_id IS '重试任务Id';
COMMENT ON COLUMN sj_retry_task_log_message.message IS '异常信息';
COMMENT ON COLUMN sj_retry_task_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_retry_task_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_retry_task_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_task_log_message IS '任务调度日志信息记录表';

-- sj_retry_scene_config

CREATE TABLE sj_retry_scene_config
(
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

CREATE UNIQUE INDEX uk_sj_retry_scene_config_01 ON sj_retry_scene_config (namespace_id, group_name, scene_name);

COMMENT ON COLUMN sj_retry_scene_config.id IS '主键';
COMMENT ON COLUMN sj_retry_scene_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_scene_config.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_scene_config.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_scene_config.scene_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_retry_scene_config.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN sj_retry_scene_config.back_off IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_retry_scene_config.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_retry_scene_config.deadline_request IS 'Deadline Request 调用链超时 单位毫秒';
COMMENT ON COLUMN sj_retry_scene_config.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_retry_scene_config.route_key IS '路由策略';
COMMENT ON COLUMN sj_retry_scene_config.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_retry_scene_config.cb_status IS '回调状态 0、不开启 1、开启';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_type IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.cb_max_count IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_interval IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.owner_id IS '负责人id';
COMMENT ON COLUMN sj_retry_scene_config.labels IS '标签';
COMMENT ON COLUMN sj_retry_scene_config.description IS '描述';
COMMENT ON COLUMN sj_retry_scene_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_scene_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_scene_config IS '场景配置';

-- sj_server_node

CREATE TABLE sj_server_node
(
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

CREATE UNIQUE INDEX uk_sj_server_node_01 ON sj_server_node (host_id, host_ip);

CREATE INDEX idx_sj_server_node_01 ON sj_server_node (namespace_id, group_name);
CREATE INDEX idx_sj_server_node_02 ON sj_server_node (expire_at, node_type);

COMMENT ON COLUMN sj_server_node.id IS '主键';
COMMENT ON COLUMN sj_server_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_server_node.group_name IS '组名称';
COMMENT ON COLUMN sj_server_node.host_id IS '主机id';
COMMENT ON COLUMN sj_server_node.host_ip IS '机器ip';
COMMENT ON COLUMN sj_server_node.host_port IS '机器端口';
COMMENT ON COLUMN sj_server_node.expire_at IS '过期时间';
COMMENT ON COLUMN sj_server_node.node_type IS '节点类型 1、客户端 2、是服务端';
COMMENT ON COLUMN sj_server_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_server_node.labels IS '标签';
COMMENT ON COLUMN sj_server_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_server_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_server_node IS '服务器节点';

-- sj_distributed_lock

CREATE TABLE sj_distributed_lock
(
    name       varchar(64)  NOT NULL PRIMARY KEY,
    lock_until timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_at  timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  varchar(255) NOT NULL,
    create_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sj_distributed_lock.name IS '锁名称';
COMMENT ON COLUMN sj_distributed_lock.lock_until IS '锁定时长';
COMMENT ON COLUMN sj_distributed_lock.locked_at IS '锁定时间';
COMMENT ON COLUMN sj_distributed_lock.locked_by IS '锁定者';
COMMENT ON COLUMN sj_distributed_lock.create_dt IS '创建时间';
COMMENT ON COLUMN sj_distributed_lock.update_dt IS '修改时间';
COMMENT ON TABLE sj_distributed_lock IS '锁定表';


-- sj_system_user_permission

CREATE TABLE sj_system_user_permission
(
    id             bigserial PRIMARY KEY,
    group_name     varchar(64) NOT NULL,
    namespace_id   varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    system_user_id bigint      NOT NULL,
    create_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_system_user_permission_01 ON sj_system_user_permission (namespace_id, group_name, system_user_id);

COMMENT ON COLUMN sj_system_user_permission.id IS '主键';
COMMENT ON COLUMN sj_system_user_permission.group_name IS '组名称';
COMMENT ON COLUMN sj_system_user_permission.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_system_user_permission.system_user_id IS '系统用户id';
COMMENT ON COLUMN sj_system_user_permission.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user_permission.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user_permission IS '系统用户权限表';

-- sj_job

CREATE TABLE sj_job
(
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

CREATE INDEX idx_sj_job_01 ON sj_job (namespace_id, group_name);
CREATE INDEX idx_sj_job_02 ON sj_job (job_status, bucket_index);
CREATE INDEX idx_sj_job_03 ON sj_job (create_dt);

COMMENT ON COLUMN sj_job.id IS '主键';
COMMENT ON COLUMN sj_job.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job.group_name IS '组名称';
COMMENT ON COLUMN sj_job.job_name IS '名称';
COMMENT ON COLUMN sj_job.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_job.job_status IS '任务状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_job.task_type IS '任务类型 1、集群 2、广播 3、切片';
COMMENT ON COLUMN sj_job.route_key IS '路由策略';
COMMENT ON COLUMN sj_job.executor_type IS '执行器类型';
COMMENT ON COLUMN sj_job.executor_info IS '执行器名称';
COMMENT ON COLUMN sj_job.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_job.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_job.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复';
COMMENT ON COLUMN sj_job.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_job.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN sj_job.parallel_num IS '并行数';
COMMENT ON COLUMN sj_job.retry_interval IS '重试间隔 ( s)';
COMMENT ON COLUMN sj_job.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_job.resident IS '是否是常驻任务';
COMMENT ON COLUMN sj_job.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_job.owner_id IS '负责人id';
COMMENT ON COLUMN sj_job.labels IS '标签';
COMMENT ON COLUMN sj_job.description IS '描述';
COMMENT ON COLUMN sj_job.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job.update_dt IS '修改时间';
COMMENT ON TABLE sj_job IS '任务信息';


CREATE TABLE sj_job_log_message
(
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

CREATE INDEX idx_sj_job_log_message_01 ON sj_job_log_message (task_batch_id, task_id);
CREATE INDEX idx_sj_job_log_message_02 ON sj_job_log_message (create_dt);
CREATE INDEX idx_sj_job_log_message_03 ON sj_job_log_message (namespace_id, group_name);

COMMENT ON COLUMN sj_job_log_message.id IS '主键';
COMMENT ON COLUMN sj_job_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_job_log_message.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_log_message.task_batch_id IS '任务批次id';
COMMENT ON COLUMN sj_job_log_message.task_id IS '调度任务id';
COMMENT ON COLUMN sj_job_log_message.message IS '调度信息';
COMMENT ON COLUMN sj_job_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_job_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_job_log_message.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_job_log_message IS '调度日志';

-- sj_job_task

CREATE TABLE sj_job_task
(
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

CREATE INDEX idx_sj_job_task_01 ON sj_job_task (task_batch_id, task_status);
CREATE INDEX idx_sj_job_task_02 ON sj_job_task (create_dt);
CREATE INDEX idx_sj_job_task_03 ON sj_job_task (namespace_id, group_name);

COMMENT ON COLUMN sj_job_task.id IS '主键';
COMMENT ON COLUMN sj_job_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_task.task_batch_id IS '调度任务id';
COMMENT ON COLUMN sj_job_task.parent_id IS '父执行器id';
COMMENT ON COLUMN sj_job_task.task_status IS '执行的状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task.retry_count IS '重试次数';
COMMENT ON COLUMN sj_job_task.mr_stage IS '动态分片所处阶段 1:map 2:reduce 3:mergeReduce';
COMMENT ON COLUMN sj_job_task.leaf IS '叶子节点';
COMMENT ON COLUMN sj_job_task.task_name IS '任务名称';
COMMENT ON COLUMN sj_job_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_job_task.wf_context IS '工作流全局上下文';
COMMENT ON COLUMN sj_job_task.result_message IS '执行结果';
COMMENT ON COLUMN sj_job_task.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job_task.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task IS '任务实例';

-- sj_job_task_batch

CREATE TABLE sj_job_task_batch
(
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

CREATE INDEX idx_sj_job_task_batch_01 ON sj_job_task_batch (job_id, task_batch_status);
CREATE INDEX idx_sj_job_task_batch_02 ON sj_job_task_batch (create_dt);
CREATE INDEX idx_sj_job_task_batch_03 ON sj_job_task_batch (namespace_id, group_name);
CREATE INDEX idx_sj_job_task_batch_04 ON sj_job_task_batch (workflow_task_batch_id, workflow_node_id);

COMMENT ON COLUMN sj_job_task_batch.id IS '主键';
COMMENT ON COLUMN sj_job_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task_batch.job_id IS '任务id';
COMMENT ON COLUMN sj_job_task_batch.workflow_node_id IS '工作流节点id';
COMMENT ON COLUMN sj_job_task_batch.parent_workflow_node_id IS '工作流任务父批次id';
COMMENT ON COLUMN sj_job_task_batch.workflow_task_batch_id IS '工作流任务批次id';
COMMENT ON COLUMN sj_job_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_job_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_job_task_batch.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_task_batch.parent_id IS '父节点';
COMMENT ON COLUMN sj_job_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task_batch IS '任务批次';

-- sj_job_summary

CREATE TABLE sj_job_summary
(
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

CREATE UNIQUE INDEX uk_sj_job_summary_01 ON sj_job_summary (trigger_at, system_task_type, business_id);

CREATE INDEX idx_sj_job_summary_01 ON sj_job_summary (namespace_id, group_name, business_id);

COMMENT ON COLUMN sj_job_summary.id IS '主键';
COMMENT ON COLUMN sj_job_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_job_summary.business_id IS '业务id  ( job_id或workflow_id)';
COMMENT ON COLUMN sj_job_summary.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_job_summary.success_num IS '执行成功-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.stop_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.stop_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.cancel_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.cancel_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_summary IS 'DashBoard_Job';

-- sj_retry_summary

CREATE TABLE sj_retry_summary
(
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

CREATE UNIQUE INDEX uk_sj_retry_summary_01 ON sj_retry_summary (namespace_id, group_name, scene_name, trigger_at);

CREATE INDEX idx_sj_retry_summary_01 ON sj_retry_summary (trigger_at);

COMMENT ON COLUMN sj_retry_summary.id IS '主键';
COMMENT ON COLUMN sj_retry_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_summary.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_retry_summary.running_num IS '重试中-日志数量';
COMMENT ON COLUMN sj_retry_summary.finish_num IS '重试完成-日志数量';
COMMENT ON COLUMN sj_retry_summary.max_count_num IS '重试到达最大次数-日志数量';
COMMENT ON COLUMN sj_retry_summary.suspend_num IS '暂停重试-日志数量';
COMMENT ON COLUMN sj_retry_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_summary IS 'DashBoard_Retry';

-- sj_workflow

CREATE TABLE sj_workflow
(
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

CREATE INDEX idx_sj_workflow_01 ON sj_workflow (create_dt);
CREATE INDEX idx_sj_workflow_02 ON sj_workflow (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow.id IS '主键';
COMMENT ON COLUMN sj_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN sj_workflow.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow.workflow_status IS '工作流状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_workflow.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_workflow.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_workflow.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_workflow.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_workflow.description IS '描述';
COMMENT ON COLUMN sj_workflow.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow.wf_context IS '上下文';
COMMENT ON COLUMN sj_workflow.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_workflow.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_workflow.version IS '版本号';
COMMENT ON COLUMN sj_workflow.owner_id IS '负责人id';
COMMENT ON COLUMN sj_workflow.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow IS '工作流';

-- sj_workflow_node

CREATE TABLE sj_workflow_node
(
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

CREATE INDEX idx_sj_workflow_node_01 ON sj_workflow_node (create_dt);
CREATE INDEX idx_sj_workflow_node_02 ON sj_workflow_node (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_node.id IS '主键';
COMMENT ON COLUMN sj_workflow_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN sj_workflow_node.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_node.job_id IS '任务信息id';
COMMENT ON COLUMN sj_workflow_node.workflow_id IS '工作流ID';
COMMENT ON COLUMN sj_workflow_node.node_type IS '1、任务节点 2、条件节点';
COMMENT ON COLUMN sj_workflow_node.expression_type IS '1、SpEl、2、Aviator 3、QL';
COMMENT ON COLUMN sj_workflow_node.fail_strategy IS '失败策略 1、跳过 2、阻塞';
COMMENT ON COLUMN sj_workflow_node.workflow_node_status IS '工作流节点状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow_node.priority_level IS '优先级';
COMMENT ON COLUMN sj_workflow_node.node_info IS '节点信息 ';
COMMENT ON COLUMN sj_workflow_node.version IS '版本号';
COMMENT ON COLUMN sj_workflow_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_node.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_node IS '工作流节点';

-- sj_workflow_task_batch

CREATE TABLE sj_workflow_task_batch
(
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

CREATE INDEX idx_sj_workflow_task_batch_01 ON sj_workflow_task_batch (workflow_id, task_batch_status);
CREATE INDEX idx_sj_workflow_task_batch_02 ON sj_workflow_task_batch (create_dt);
CREATE INDEX idx_sj_workflow_task_batch_03 ON sj_workflow_task_batch (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_task_batch.id IS '主键';
COMMENT ON COLUMN sj_workflow_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_task_batch.workflow_id IS '工作流任务id';
COMMENT ON COLUMN sj_workflow_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_workflow_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_workflow_task_batch.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow_task_batch.wf_context IS '全局上下文';
COMMENT ON COLUMN sj_workflow_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_workflow_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_task_batch.version IS '版本号';
COMMENT ON COLUMN sj_workflow_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_task_batch IS '工作流批次';

-- sj_job_executor

CREATE TABLE sj_job_executor
(
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64)  NOT NULL,
    executor_info varchar(256) NOT NULL,
    executor_type varchar(3)   NOT NULL,
    create_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_executor_01 ON sj_job_executor (namespace_id, group_name);
CREATE INDEX idx_sj_job_executor_02 ON sj_job_executor (create_dt);

COMMENT ON COLUMN sj_job_executor.id IS '主键';
COMMENT ON COLUMN sj_job_executor.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_executor.group_name IS '组名称';
COMMENT ON COLUMN sj_job_executor.executor_info IS '任务执行器名称';
COMMENT ON COLUMN sj_job_executor.executor_type IS '1:java 2:python 3:go';
COMMENT ON COLUMN sj_job_executor.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_executor.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_executor IS '任务执行器信息';


-- sj_system_user
CREATE TABLE sj_system_user
(
    id        bigserial PRIMARY KEY,
    username  varchar(64)  NOT NULL,
    password  varchar(128) NOT NULL,
    role      smallint     NOT NULL DEFAULT 0,
    create_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sj_system_user.id IS '主键';
COMMENT ON COLUMN sj_system_user.username IS '账号';
COMMENT ON COLUMN sj_system_user.password IS '密码';
COMMENT ON COLUMN sj_system_user.role IS '角色：1-普通用户、2-管理员';
COMMENT ON COLUMN sj_system_user.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user IS '系统用户表';

INSERT INTO sj_system_user (username, password, role)
VALUES ('admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2);

INSERT INTO sj_job VALUES (1, 'dev', 'ruoyi_group', 'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', 1, '', '', '', 0, now(), now());

-- ----------------------------
-- 0. 模型供应商表
-- ----------------------------
DROP TABLE IF EXISTS km_model_provider CASCADE;

CREATE TABLE km_model_provider (
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
COMMENT ON COLUMN km_model_provider.provider_id IS '供应商ID';
COMMENT ON COLUMN km_model_provider.provider_name IS '供应商名称';
COMMENT ON COLUMN km_model_provider.provider_key IS '供应商标识(openai/ollama)';
COMMENT ON COLUMN km_model_provider.provider_type IS '供应商类型（1公用 2本地）';
COMMENT ON COLUMN km_model_provider.default_endpoint IS '默认API地址';
COMMENT ON COLUMN km_model_provider.site_url IS '官网URL';
COMMENT ON COLUMN km_model_provider.icon_url IS '图标URL';
COMMENT ON COLUMN km_model_provider.config_schema IS '配置参数定义';
COMMENT ON COLUMN km_model_provider.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN km_model_provider.sort IS '排序';
COMMENT ON COLUMN km_model_provider.models IS '支持的模型标识(JSON)';

-- ----------------------------
-- 1. 模型管理表
-- ----------------------------
DROP TABLE IF EXISTS km_model CASCADE;

CREATE TABLE km_model (
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
    PRIMARY KEY (model_id)
);

COMMENT ON TABLE km_model IS 'AI模型配置表';
COMMENT ON COLUMN km_model.is_default IS '是否为系统默认使用模型(0-否 1-是)';
COMMENT ON COLUMN km_model.model_name IS '模型名称';
COMMENT ON COLUMN km_model.model_type IS '模型类型:1-语言模型 2-视觉模型 3-语音模型 4-混合模型';
COMMENT ON COLUMN km_model.model_key IS '模型标识';
COMMENT ON COLUMN km_model.api_key IS 'API Key';
COMMENT ON COLUMN km_model.api_base IS 'API Base地址';
COMMENT ON COLUMN km_model.config IS '配置参数';
COMMENT ON COLUMN km_model.is_builtin IS '是否为系统内置模型(N-否 Y-是)';
COMMENT ON COLUMN km_model.model_source IS '模型来源(1公有 2本地)';
COMMENT ON COLUMN km_model.create_dept IS '创建部门';
COMMENT ON COLUMN km_model.create_by IS '创建人';
COMMENT ON COLUMN km_model.create_time IS '创建时间';
COMMENT ON COLUMN km_model.update_by IS '更新人';
COMMENT ON COLUMN km_model.update_time IS '更新时间';
COMMENT ON COLUMN km_model.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_model.remark IS '备注';


-- ----------------------------
-- 2. 知识库主表
-- ----------------------------
DROP TABLE IF EXISTS km_knowledge_base CASCADE;
CREATE TABLE km_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT,
    permission_level VARCHAR(50) DEFAULT 'PRIVATE', -- PRIVATE, TEAM, PUBLIC
    status VARCHAR(50) DEFAULT 'ACTIVE',
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);
COMMENT ON TABLE km_knowledge_base IS '知识库主表';

COMMENT ON COLUMN km_knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN km_knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN km_knowledge_base.owner_id IS '知识库所有者';
COMMENT ON COLUMN km_knowledge_base.permission_level IS '知识库权限(PRIVATE, TEAM, PUBLIC)';
COMMENT ON COLUMN km_knowledge_base.status IS '知识库状态(ACTIVE, INACTIVE)';
COMMENT ON COLUMN km_knowledge_base.create_dept IS '创建部门';
COMMENT ON COLUMN km_knowledge_base.create_by IS '创建人';
COMMENT ON COLUMN km_knowledge_base.create_time IS '创建时间';
COMMENT ON COLUMN km_knowledge_base.update_by IS '更新人';
COMMENT ON COLUMN km_knowledge_base.update_time IS '更新时间';
COMMENT ON COLUMN km_knowledge_base.del_flag IS '删除标识(0-未删除 1-已删除)';

-- ----------------------------
-- 3. 数据集表
-- ----------------------------
DROP TABLE IF EXISTS km_dataset CASCADE;
CREATE TABLE km_dataset (
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
    del_flag CHAR(1) DEFAULT '0'
);
CREATE INDEX idx_dataset_kb_id ON km_dataset(kb_id);
COMMENT ON TABLE km_dataset IS '数据集表';
COMMENT ON COLUMN km_dataset.kb_id IS '知识库ID';
COMMENT ON COLUMN km_dataset.name IS '数据集名称';
COMMENT ON COLUMN km_dataset.config IS '配置参数';
COMMENT ON COLUMN km_dataset.is_system IS '是否为系统数据集';
COMMENT ON COLUMN km_dataset.allowed_file_types IS '支持的文件格式(逗号分隔,*表示全部)';
COMMENT ON COLUMN km_dataset.process_type IS '处理类型:GENERIC_FILE/QA_PAIR/ONLINE_DOC/WEB_LINK';
COMMENT ON COLUMN km_dataset.source_type IS '数据源类型:FILE/WEB/MANUAL';
COMMENT ON COLUMN km_dataset.min_chunk_size IS '最小分块大小';
COMMENT ON COLUMN km_dataset.max_chunk_size IS '最大分块大小';
COMMENT ON COLUMN km_dataset.chunk_overlap IS '分块重叠大小';
COMMENT ON COLUMN km_dataset.create_dept IS '创建部门';
COMMENT ON COLUMN km_dataset.create_by IS '创建人';
COMMENT ON COLUMN km_dataset.create_time IS '创建时间';
COMMENT ON COLUMN km_dataset.update_by IS '更新人';
COMMENT ON COLUMN km_dataset.update_time IS '更新时间';
COMMENT ON COLUMN km_dataset.del_flag IS '删除标识(0-未删除 1-已删除)';

-- ----------------------------
-- 4. 文档表
-- ----------------------------
DROP TABLE IF EXISTS km_document CASCADE;
CREATE TABLE km_document (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    kb_id BIGINT,              -- 冗余字段，便于查询
    original_filename VARCHAR(512),
    file_path VARCHAR(1024),
    oss_id BIGINT,             -- OSS文件ID
    file_type VARCHAR(50),
    file_size BIGINT,
    error_msg TEXT,
    token_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    hash_code VARCHAR(128),
    store_type INTEGER DEFAULT 1, -- 1-OSS, 2-本地文件
    enabled INTEGER DEFAULT 1,    -- 0=禁用, 1=启用
    embedding_status INTEGER DEFAULT 0, -- 0=未生成, 1=生成中, 2=已生成, 3=生成失败
    question_status INTEGER DEFAULT 0,  -- 0=未生成, 1=生成中, 2=已生成, 3=生成失败
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
CREATE INDEX idx_document_dataset_id ON km_document(dataset_id);
CREATE INDEX idx_document_kb_id ON km_document(kb_id);
COMMENT ON TABLE km_document IS '文档表';
COMMENT ON COLUMN km_document.dataset_id IS '数据集ID';
COMMENT ON COLUMN km_document.kb_id IS '知识库ID';
COMMENT ON COLUMN km_document.original_filename IS '原始文件名';
COMMENT ON COLUMN km_document.file_path IS '文件路径';
COMMENT ON COLUMN km_document.oss_id IS 'OSS文件ID';
COMMENT ON COLUMN km_document.file_type IS '文件类型-扩展名';
COMMENT ON COLUMN km_document.file_size IS '文件大小';
COMMENT ON COLUMN km_document.error_msg IS '错误信息';
COMMENT ON COLUMN km_document.token_count IS 'Token数量';
COMMENT ON COLUMN km_document.chunk_count IS '分块数量';
COMMENT ON COLUMN km_document.hash_code IS '文件哈希值';
COMMENT ON COLUMN km_document.store_type IS '存储类型(1-OSS, 2-本地文件)';
COMMENT ON COLUMN km_document.enabled IS '启用状态(0-禁用, 1-启用)';
COMMENT ON COLUMN km_document.embedding_status IS '向量生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document.question_status IS '问答对生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document.status_meta IS '状态元数据';
COMMENT ON COLUMN km_document.create_dept IS '创建部门';
COMMENT ON COLUMN km_document.create_by IS '创建人';
COMMENT ON COLUMN km_document.create_time IS '创建时间';
COMMENT ON COLUMN km_document.update_by IS '更新人';
COMMENT ON COLUMN km_document.update_time IS '更新时间';
COMMENT ON COLUMN km_document.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_document.title IS '文档标题(用于向量化)';
COMMENT ON COLUMN km_document.content IS '在线文档内容(富文本HTML)';
COMMENT ON COLUMN km_document.url IS '网页链接URL';

-- ----------------------------
-- 5. 文档分块表
-- ----------------------------
DROP TABLE IF EXISTS km_document_chunk CASCADE;
CREATE TABLE km_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    kb_id BIGINT,              -- 冗余字段
    title VARCHAR(500),        -- 分块标题
    content TEXT,
    metadata JSONB,
    parent_chain TEXT,         -- 父级标题链路 JSON array
    enabled INTEGER DEFAULT 1,
    embedding_status INTEGER DEFAULT 0,
    question_status INTEGER DEFAULT 0,
    status_meta JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chunk_document_id ON km_document_chunk(document_id);
CREATE INDEX idx_chunk_kb_id ON km_document_chunk(kb_id);
COMMENT ON TABLE km_document_chunk IS '文档分块表';
COMMENT ON COLUMN km_document_chunk.document_id IS '文档ID';
COMMENT ON COLUMN km_document_chunk.kb_id IS '知识库ID';
COMMENT ON COLUMN km_document_chunk.title IS '分块标题';
COMMENT ON COLUMN km_document_chunk.content IS '分块内容';
COMMENT ON COLUMN km_document_chunk.metadata IS '元数据';
COMMENT ON COLUMN km_document_chunk.parent_chain IS '父级标题链路';
COMMENT ON COLUMN km_document_chunk.enabled IS '启用状态(0-禁用, 1-启用)';
COMMENT ON COLUMN km_document_chunk.embedding_status IS '向量生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document_chunk.question_status IS '问答对生成状态(0-未生成, 1-生成中, 2-已生成, 3-生成失败)';
COMMENT ON COLUMN km_document_chunk.status_meta IS '状态元数据';
COMMENT ON COLUMN km_document_chunk.create_time IS '创建时间';

-- ----------------------------
-- 6. 问题表
-- ----------------------------
DROP TABLE IF EXISTS km_question CASCADE;
CREATE TABLE km_question (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    hit_num INT DEFAULT 0,
    source_type VARCHAR(20) DEFAULT 'IMPORT',
    content_search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    create_dept BIGINT,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0'
);
CREATE INDEX idx_question_kb_id ON km_question(kb_id);
CREATE INDEX idx_question_search_vector ON km_question USING GIN (content_search_vector);
COMMENT ON TABLE km_question IS '问题表';
COMMENT ON COLUMN km_question.kb_id IS '知识库ID';
COMMENT ON COLUMN km_question.content IS '问题内容';
COMMENT ON COLUMN km_question.hit_num IS '命中次数';
COMMENT ON COLUMN km_question.source_type IS '来源类型(IMPORT-导入, GENERATED-生成)';
COMMENT ON COLUMN km_question.content_search_vector IS '全文搜索向量';
COMMENT ON COLUMN km_question.create_dept IS '创建部门';
COMMENT ON COLUMN km_question.create_by IS '创建人';
COMMENT ON COLUMN km_question.create_time IS '创建时间';
COMMENT ON COLUMN km_question.update_by IS '更新人';
COMMENT ON COLUMN km_question.update_time IS '更新时间';
COMMENT ON COLUMN km_question.del_flag IS '删除标识(0-未删除 1-已删除)';

-- ----------------------------
-- 7. 问题分块关联表
-- ----------------------------
DROP TABLE IF EXISTS km_question_chunk_map CASCADE;
CREATE TABLE km_question_chunk_map (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    CONSTRAINT uk_question_chunk UNIQUE (question_id, chunk_id)
);
CREATE INDEX idx_qcm_question_id ON km_question_chunk_map(question_id);
CREATE INDEX idx_qcm_chunk_id ON km_question_chunk_map(chunk_id);
COMMENT ON TABLE km_question_chunk_map IS '问题与分块关联表';
COMMENT ON COLUMN km_question_chunk_map.question_id IS '问题ID';
COMMENT ON COLUMN km_question_chunk_map.chunk_id IS '分块ID';

-- ----------------------------
-- 8. 统一向量存储表
-- ----------------------------
DROP TABLE IF EXISTS km_embedding CASCADE;
CREATE TABLE km_embedding (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_type SMALLINT NOT NULL,  -- 0=QUESTION, 1=CONTENT, 2=TITLE
    embedding vector(512),
    text_content TEXT,
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('jiebacfg', coalesce(text_content, ''))) STORED,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_embedding_kb_id ON km_embedding(kb_id);
CREATE INDEX idx_embedding_source ON km_embedding(source_id, source_type);
CREATE INDEX idx_embedding_vector ON km_embedding USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX idx_embedding_search_vector ON km_embedding USING GIN (search_vector);
COMMENT ON TABLE km_embedding IS '统一向量存储表';
COMMENT ON COLUMN km_embedding.kb_id IS '知识库ID';
COMMENT ON COLUMN km_embedding.source_id IS '来源ID';
COMMENT ON COLUMN km_embedding.source_type IS '来源类型(0=QUESTION, 1=CONTENT, 2=TITLE)';
COMMENT ON COLUMN km_embedding.embedding IS '向量';
COMMENT ON COLUMN km_embedding.text_content IS '文本内容';
COMMENT ON COLUMN km_embedding.search_vector IS '全文搜索向量';
COMMENT ON COLUMN km_embedding.create_time IS '创建时间';

-- ----------------------------
-- 9. 临时文件表
-- ----------------------------
DROP TABLE IF EXISTS km_temp_file CASCADE;
CREATE TABLE km_temp_file (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    file_extension VARCHAR(50),
    file_size BIGINT,
    temp_path VARCHAR(1000) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP NOT NULL
);
CREATE INDEX idx_temp_file_dataset ON km_temp_file(dataset_id);
CREATE INDEX idx_temp_file_expire ON km_temp_file(expire_time);
COMMENT ON TABLE km_temp_file IS '临时文件表';
COMMENT ON COLUMN km_temp_file.dataset_id IS '数据集ID';
COMMENT ON COLUMN km_temp_file.original_filename IS '原始文件名';
COMMENT ON COLUMN km_temp_file.file_extension IS '文件扩展名';
COMMENT ON COLUMN km_temp_file.file_size IS '文件大小';
COMMENT ON COLUMN km_temp_file.temp_path IS '临时文件路径';
COMMENT ON COLUMN km_temp_file.create_time IS '创建时间';
COMMENT ON COLUMN km_temp_file.expire_time IS '过期时间';

-- ----------------------------
-- 10. 应用表
-- ----------------------------
DROP TABLE IF EXISTS km_app CASCADE;
CREATE TABLE km_app (
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
    parameters      JSONB           DEFAULT NULL, -- 新增
    source_template_id BIGINT          DEFAULT NULL, -- 新增
    source_template_scope CHAR(1)      DEFAULT NULL, -- 新增
    model_id        BIGINT          DEFAULT NULL,
    enable_execution_detail CHAR(1) DEFAULT '0', -- 新增
    public_access   CHAR(1)         DEFAULT '1', -- 新增
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
COMMENT ON COLUMN km_app.description IS '应用描述';
COMMENT ON COLUMN km_app.icon IS '应用图标';
COMMENT ON COLUMN km_app.app_type IS '应用类型（1固定模板 2自定义工作流）';
COMMENT ON COLUMN km_app.status IS '应用状态（0禁用 1启用）';
COMMENT ON COLUMN km_app.prologue IS '应用前置语-开场白';
COMMENT ON COLUMN km_app.model_setting IS '模型配置';
COMMENT ON COLUMN km_app.knowledge_setting IS '知识库配置';
COMMENT ON COLUMN km_app.workflow_config IS '工作流配置';
COMMENT ON COLUMN km_app.graph_data IS '图数据';
COMMENT ON COLUMN km_app.dsl_data IS 'DSL数据';
COMMENT ON COLUMN km_app.parameters IS '应用参数配置(全局/接口/会话)';
COMMENT ON COLUMN km_app.source_template_id IS '来源模版ID';
COMMENT ON COLUMN km_app.source_template_scope IS '来源模版类型(0系统/1自建)';
COMMENT ON COLUMN km_app.model_id IS '模型ID';
COMMENT ON COLUMN km_app.enable_execution_detail IS '是否启用执行详情（0禁用 1启用）';
COMMENT ON COLUMN km_app.public_access IS '公开访问（0关闭 1开启）';
COMMENT ON COLUMN km_app.create_dept IS '创建部门';
COMMENT ON COLUMN km_app.create_by IS '创建人';
COMMENT ON COLUMN km_app.create_time IS '创建时间';
COMMENT ON COLUMN km_app.update_by IS '更新人';
COMMENT ON COLUMN km_app.update_time IS '更新时间';
COMMENT ON COLUMN km_app.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_app.remark IS '备注';

-- ----------------------------
-- 4.1 应用关联表 (Token, 统计, 知识库)
-- ----------------------------
DROP TABLE IF EXISTS km_app_knowledge CASCADE;

CREATE TABLE km_app_knowledge (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    knowledge_id    BIGINT          NOT NULL,
    sort            INTEGER         DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_kb UNIQUE (app_id, knowledge_id)
);

DROP TABLE IF EXISTS km_app_version CASCADE;

CREATE TABLE km_app_version (
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
COMMENT ON COLUMN km_app_version.version_id IS '版本ID';
COMMENT ON COLUMN km_app_version.app_id IS '应用ID';
COMMENT ON COLUMN km_app_version.version IS '版本号';
COMMENT ON COLUMN km_app_version.app_snapshot IS '应用快照';
COMMENT ON COLUMN km_app_version.create_time IS '创建时间';
COMMENT ON COLUMN km_app_version.create_by IS '创建人';
COMMENT ON COLUMN km_app_version.remark IS '备注';

DROP TABLE IF EXISTS km_app_access_stat CASCADE;

CREATE TABLE km_app_access_stat (
    id              BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    access_count    BIGINT          DEFAULT 0,
    last_access_time TIMESTAMP      DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_km_app_user UNIQUE (app_id, user_id)
);
COMMENT ON TABLE km_app_access_stat IS '应用访问统计表';
COMMENT ON COLUMN km_app_access_stat.id IS 'ID';
COMMENT ON COLUMN km_app_access_stat.app_id IS '应用ID';
COMMENT ON COLUMN km_app_access_stat.user_id IS '用户ID';
COMMENT ON COLUMN km_app_access_stat.access_count IS '访问次数';
COMMENT ON COLUMN km_app_access_stat.last_access_time IS '最后访问时间';

-- 新增：App嵌入Token表
DROP TABLE IF EXISTS km_app_token CASCADE;

CREATE TABLE km_app_token (
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
CREATE INDEX idx_km_app_token_app ON km_app_token(app_id);
COMMENT ON TABLE km_app_token IS '应用Token表';
COMMENT ON COLUMN km_app_token.token_id IS 'Token ID';
COMMENT ON COLUMN km_app_token.app_id IS '应用ID';
COMMENT ON COLUMN km_app_token.token IS 'Token';
COMMENT ON COLUMN km_app_token.token_name IS 'Token名称';
COMMENT ON COLUMN km_app_token.allowed_origins IS '允许的来源';
COMMENT ON COLUMN km_app_token.expires_at IS '过期时间';
COMMENT ON COLUMN km_app_token.status IS '状态（0禁用 1启用）';
COMMENT ON COLUMN km_app_token.remark IS '备注';
COMMENT ON COLUMN km_app_token.del_flag IS '删除标识(0-未删除 1-已删除)';
COMMENT ON COLUMN km_app_token.create_dept IS '创建部门';
COMMENT ON COLUMN km_app_token.create_by IS '创建人';
COMMENT ON COLUMN km_app_token.create_time IS '创建时间';
COMMENT ON COLUMN km_app_token.update_by IS '更新人';
COMMENT ON COLUMN km_app_token.update_time IS '更新时间';

-- ----------------------------
-- 5. 数据源与元数据 (SQL查询节点依赖)
-- ----------------------------
DROP TABLE IF EXISTS km_data_source CASCADE;

CREATE TABLE km_data_source (
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
COMMENT ON COLUMN km_data_source.data_source_id IS '数据源ID';
COMMENT ON COLUMN km_data_source.data_source_name IS '数据源名称';
COMMENT ON COLUMN km_data_source.source_type IS '数据源类型';
COMMENT ON COLUMN km_data_source.ds_key IS '数据源Key';
COMMENT ON COLUMN km_data_source.driver_class_name IS '驱动类名';
COMMENT ON COLUMN km_data_source.jdbc_url IS 'JDBC URL';
COMMENT ON COLUMN km_data_source.username IS '用户名';
COMMENT ON COLUMN km_data_source.password IS '密码';
COMMENT ON COLUMN km_data_source.db_type IS '数据库类型';
COMMENT ON COLUMN km_data_source.is_enabled IS '是否启用（0禁用 1启用）';
COMMENT ON COLUMN km_data_source.create_dept IS '创建部门';
COMMENT ON COLUMN km_data_source.create_by IS '创建人';
COMMENT ON COLUMN km_data_source.create_time IS '创建时间';
COMMENT ON COLUMN km_data_source.update_by IS '更新人';
COMMENT ON COLUMN km_data_source.update_time IS '更新时间';
COMMENT ON COLUMN km_data_source.remark IS '备注';

DROP TABLE IF EXISTS km_database_meta CASCADE;

CREATE TABLE km_database_meta (
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
COMMENT ON COLUMN km_database_meta.meta_id IS '元数据ID';
COMMENT ON COLUMN km_database_meta.data_source_id IS '数据源ID';
COMMENT ON COLUMN km_database_meta.meta_source_type IS '元数据源类型';
COMMENT ON COLUMN km_database_meta.ddl_content IS 'DDL内容';
COMMENT ON COLUMN km_database_meta.table_name IS '表名';
COMMENT ON COLUMN km_database_meta.table_comment IS '表注释';
COMMENT ON COLUMN km_database_meta.columns IS '列信息';
COMMENT ON COLUMN km_database_meta.create_dept IS '创建部门';
COMMENT ON COLUMN km_database_meta.create_by IS '创建人';
COMMENT ON COLUMN km_database_meta.create_time IS '创建时间';
COMMENT ON COLUMN km_database_meta.update_by IS '更新人';
COMMENT ON COLUMN km_database_meta.update_time IS '更新时间';
COMMENT ON COLUMN km_database_meta.remark IS '备注';

-- ----------------------------
-- 6. 工作流执行记录 (整合 Token 统计及耗时)
-- ----------------------------
DROP TABLE IF EXISTS km_workflow_instance CASCADE;

CREATE TABLE km_workflow_instance (
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
COMMENT ON COLUMN km_workflow_instance.instance_id IS '实例ID';
COMMENT ON COLUMN km_workflow_instance.app_id IS '应用ID';
COMMENT ON COLUMN km_workflow_instance.session_id IS '会话ID';
COMMENT ON COLUMN km_workflow_instance.workflow_config IS '工作流配置';
COMMENT ON COLUMN km_workflow_instance.status IS '状态（0运行中 1已完成 2已取消 3异常）';
COMMENT ON COLUMN km_workflow_instance.current_node IS '当前节点';
COMMENT ON COLUMN km_workflow_instance.global_state IS '全局状态';
COMMENT ON COLUMN km_workflow_instance.start_time IS '开始时间';
COMMENT ON COLUMN km_workflow_instance.end_time IS '结束时间';
COMMENT ON COLUMN km_workflow_instance.error_message IS '错误信息';
COMMENT ON COLUMN km_workflow_instance.create_dept IS '创建部门';
COMMENT ON COLUMN km_workflow_instance.create_by IS '创建人';
COMMENT ON COLUMN km_workflow_instance.create_time IS '创建时间';
COMMENT ON COLUMN km_workflow_instance.update_by IS '更新人';
COMMENT ON COLUMN km_workflow_instance.update_time IS '更新时间';

DROP TABLE IF EXISTS km_node_execution CASCADE;

CREATE TABLE km_node_execution (
    execution_id    BIGINT          NOT NULL,
    instance_id     BIGINT          NOT NULL,
    node_id         VARCHAR(64)     NOT NULL,
    node_type       VARCHAR(64)     NOT NULL,
    node_name       VARCHAR(200), -- 新增
    status          VARCHAR(20)     NOT NULL,
    input_params    JSONB,
    output_params   JSONB,
    input_tokens    INTEGER         DEFAULT 0, -- 新增
    output_tokens   INTEGER         DEFAULT 0, -- 新增
    total_tokens    INTEGER         DEFAULT 0, -- 新增
    duration_ms     BIGINT,        -- 新增
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
COMMENT ON COLUMN km_node_execution.execution_id IS '执行ID';
COMMENT ON COLUMN km_node_execution.instance_id IS '实例ID';
COMMENT ON COLUMN km_node_execution.node_id IS '节点ID';
COMMENT ON COLUMN km_node_execution.node_type IS '节点类型';
COMMENT ON COLUMN km_node_execution.node_name IS '节点名称';
COMMENT ON COLUMN km_node_execution.status IS '状态（0运行中 1已完成 2已取消 3异常）';
COMMENT ON COLUMN km_node_execution.input_params IS '输入参数';
COMMENT ON COLUMN km_node_execution.output_params IS '输出参数';
COMMENT ON COLUMN km_node_execution.input_tokens IS '输入Token数';
COMMENT ON COLUMN km_node_execution.output_tokens IS '输出Token数';
COMMENT ON COLUMN km_node_execution.total_tokens IS '总Token数';
COMMENT ON COLUMN km_node_execution.duration_ms IS '耗时（毫秒）';
COMMENT ON COLUMN km_node_execution.start_time IS '开始时间';
COMMENT ON COLUMN km_node_execution.end_time IS '结束时间';
COMMENT ON COLUMN km_node_execution.error_message IS '错误信息';
COMMENT ON COLUMN km_node_execution.retry_count IS '重试次数';
COMMENT ON COLUMN km_node_execution.create_by IS '创建人';
COMMENT ON COLUMN km_node_execution.create_time IS '创建时间';
COMMENT ON COLUMN km_node_execution.update_by IS '更新人';
COMMENT ON COLUMN km_node_execution.update_time IS '更新时间';

-- ----------------------------
-- 7. 聊天会话与消息 (整合 user_type)
-- ----------------------------
DROP TABLE IF EXISTS km_chat_session CASCADE;

CREATE TABLE km_chat_session (
    session_id      BIGINT          NOT NULL,
    app_id          BIGINT          NOT NULL,
    title           VARCHAR(128)    DEFAULT '新会话',
    user_id         BIGINT          NOT NULL,
    user_type       VARCHAR(20)     DEFAULT 'system_user', -- 新增
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
COMMENT ON COLUMN km_chat_session.session_id IS '会话ID';
COMMENT ON COLUMN km_chat_session.app_id IS '应用ID';
COMMENT ON COLUMN km_chat_session.title IS '会话标题';
COMMENT ON COLUMN km_chat_session.user_id IS '用户ID';
COMMENT ON COLUMN km_chat_session.user_type IS '用户类型 (anonymous_user/system_user/third_user)';
COMMENT ON COLUMN km_chat_session.create_dept IS '创建部门';
COMMENT ON COLUMN km_chat_session.create_by IS '创建人';
COMMENT ON COLUMN km_chat_session.create_time IS '创建时间';
COMMENT ON COLUMN km_chat_session.update_by IS '更新人';
COMMENT ON COLUMN km_chat_session.update_time IS '更新时间';
COMMENT ON COLUMN km_chat_session.remark IS '备注';
COMMENT ON COLUMN km_chat_session.del_flag IS '删除标识(0-未删除 1-已删除)';

DROP TABLE IF EXISTS km_chat_message CASCADE;

CREATE TABLE km_chat_message (
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
    PRIMARY KEY (message_id)
);
COMMENT ON TABLE km_chat_message IS '聊天消息表';
COMMENT ON COLUMN km_chat_message.message_id IS '消息ID';
COMMENT ON COLUMN km_chat_message.instance_id IS '实例ID';
COMMENT ON COLUMN km_chat_message.session_id IS '会话ID';
COMMENT ON COLUMN km_chat_message.role IS '角色 (user/assistant/system)';
COMMENT ON COLUMN km_chat_message.content IS '消息内容';
COMMENT ON COLUMN km_chat_message.create_dept IS '创建部门';
COMMENT ON COLUMN km_chat_message.create_by IS '创建人';
COMMENT ON COLUMN km_chat_message.create_time IS '创建时间';
COMMENT ON COLUMN km_chat_message.update_by IS '更新人';
COMMENT ON COLUMN km_chat_message.update_time IS '更新时间';
COMMENT ON COLUMN km_chat_message.remark IS '备注';

-- ----------------------------
-- 8. 工作流节点定义 (整合自定义参数控制)
-- ----------------------------
DROP TABLE IF EXISTS km_node_definition CASCADE;

CREATE TABLE km_node_definition (
    node_def_id         BIGINT          NOT NULL,
    node_type           VARCHAR(100)    NOT NULL,
    node_label          VARCHAR(200)    NOT NULL,
    node_icon           VARCHAR(200),
    node_color          VARCHAR(50),
    category            VARCHAR(50)     NOT NULL,
    description         VARCHAR(500),
    is_system           CHAR(1)         DEFAULT '0',
    is_enabled          CHAR(1)         DEFAULT '1',
    allow_custom_input_params CHAR(1) DEFAULT '0', -- 新增
    allow_custom_output_params CHAR(1) DEFAULT '0', -- 新增
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
COMMENT ON COLUMN km_node_definition.node_def_id IS '节点定义ID';
COMMENT ON COLUMN km_node_definition.node_type IS '节点类型';
COMMENT ON COLUMN km_node_definition.node_label IS '节点标签';
COMMENT ON COLUMN km_node_definition.node_icon IS '节点图标';
COMMENT ON COLUMN km_node_definition.node_color IS '节点颜色';
COMMENT ON COLUMN km_node_definition.category IS '节点分类';
COMMENT ON COLUMN km_node_definition.description IS '节点描述';
COMMENT ON COLUMN km_node_definition.is_system IS '是否系统节点(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.allow_custom_input_params IS '允许自定义输入参数(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.allow_custom_output_params IS '允许自定义输出参数(0-否 1-是)';
COMMENT ON COLUMN km_node_definition.input_params IS '输入参数';
COMMENT ON COLUMN km_node_definition.output_params IS '输出参数';
COMMENT ON COLUMN km_node_definition.version IS '版本';
COMMENT ON COLUMN km_node_definition.parent_version_id IS '父版本ID';
COMMENT ON COLUMN km_node_definition.create_dept IS '创建部门';
COMMENT ON COLUMN km_node_definition.create_by IS '创建人';
COMMENT ON COLUMN km_node_definition.create_time IS '创建时间';
COMMENT ON COLUMN km_node_definition.update_by IS '更新人';
COMMENT ON COLUMN km_node_definition.update_time IS '更新时间';
COMMENT ON COLUMN km_node_definition.remark IS '备注';

DROP TABLE IF EXISTS km_workflow_template CASCADE;

CREATE TABLE km_workflow_template (
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
COMMENT ON COLUMN km_workflow_template.template_id IS '模板ID';
COMMENT ON COLUMN km_workflow_template.template_name IS '模板名称';
COMMENT ON COLUMN km_workflow_template.template_code IS '模板编码';
COMMENT ON COLUMN km_workflow_template.description IS '描述';
COMMENT ON COLUMN km_workflow_template.icon IS '图标';
COMMENT ON COLUMN km_workflow_template.category IS '分类';
COMMENT ON COLUMN km_workflow_template.scope_type IS '范围类型(1-固定 2-自定义)';
COMMENT ON COLUMN km_workflow_template.workflow_config IS '工作流配置';
COMMENT ON COLUMN km_workflow_template.graph_data IS '图数据';
COMMENT ON COLUMN km_workflow_template.version IS '版本';
COMMENT ON COLUMN km_workflow_template.parent_version_id IS '父版本ID';
COMMENT ON COLUMN km_workflow_template.is_published IS '是否发布(0-否 1-是)';
COMMENT ON COLUMN km_workflow_template.publish_time IS '发布时间';
COMMENT ON COLUMN km_workflow_template.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_workflow_template.use_count IS '使用次数';
COMMENT ON COLUMN km_workflow_template.create_dept IS '创建部门';
COMMENT ON COLUMN km_workflow_template.create_by IS '创建人';
COMMENT ON COLUMN km_workflow_template.create_time IS '创建时间';
COMMENT ON COLUMN km_workflow_template.update_by IS '更新人';
COMMENT ON COLUMN km_workflow_template.update_time IS '更新时间';
COMMENT ON COLUMN km_workflow_template.remark IS '备注';

DROP TABLE IF EXISTS km_node_connection_rule CASCADE;

CREATE TABLE km_node_connection_rule (
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
COMMENT ON COLUMN km_node_connection_rule.rule_id IS '规则ID';
COMMENT ON COLUMN km_node_connection_rule.source_node_type IS '源节点类型';
COMMENT ON COLUMN km_node_connection_rule.target_node_type IS '目标节点类型';
COMMENT ON COLUMN km_node_connection_rule.rule_type IS '规则类型:0-允许；1-禁止';
COMMENT ON COLUMN km_node_connection_rule.priority IS '优先级';
COMMENT ON COLUMN km_node_connection_rule.is_enabled IS '是否启用(0-否 1-是)';
COMMENT ON COLUMN km_node_connection_rule.create_dept IS '创建部门';
COMMENT ON COLUMN km_node_connection_rule.create_by IS '创建人';
COMMENT ON COLUMN km_node_connection_rule.create_time IS '创建时间';
COMMENT ON COLUMN km_node_connection_rule.update_by IS '更新人';
COMMENT ON COLUMN km_node_connection_rule.update_time IS '更新时间';
COMMENT ON COLUMN km_node_connection_rule.remark IS '备注';

DROP TABLE IF EXISTS km_tool CASCADE;

CREATE TABLE km_tool (
    tool_id         BIGINT          NOT NULL,
    tool_name       VARCHAR(64)     NOT NULL,
    tool_label      VARCHAR(64)     NOT NULL,
    description     VARCHAR(500)    DEFAULT '',
    tool_type       CHAR(1)         NOT NULL,
    icon            VARCHAR(255)    DEFAULT '',
    input_params_schema JSONB       DEFAULT NULL,
    init_params_schema JSONB        DEFAULT NULL,
    api_spec        TEXT,
    mcp_config      JSONB,
    create_dept     BIGINT,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    del_flag        CHAR(1)         DEFAULT '0',
    remark          VARCHAR(500),
    PRIMARY KEY (tool_id)
);

-- ----------------------------

-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user values(1, 103, 'admin', 'keyi', 'sys_user', 'hnliuwx@gmail.com', '13888888888', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), 103, 1, now(), null, null, '管理员');
insert into sys_user VALUES(3, 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', now(), 103, 1, now(), 3, now(), NULL);
insert into sys_user VALUES(4, 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', now(), 103, 1, now(), 4, now(), NULL);

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '超级管理员',  'superadmin',  1, '1', 't', 't', '0', '0', 103, 1, now(), null, null, '超级管理员');
insert into sys_role values('3', '本部门及以下', 'test1', 3, '4', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, '');
insert into sys_role values('4', '仅本人', 'test2', 4, '5', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, '');

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('3', '3');
insert into sys_user_role values ('4', '4');

insert into sys_client values (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, 0, 0, 103, 1, now(), 1, now());
insert into sys_client values (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, 0, 0, 103, 1, now(), 1, now());

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100, 0,   '0',          '科亿信息技术',   null,0, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(101, 100, '0,100',      '深圳总公司', null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(102, 100, '0,100',      '长沙分公司', null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(103, 101, '0,100,101',  '研发部门',   null,1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(104, 101, '0,100,101',  '市场部门',   null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(105, 101, '0,100,101',  '测试部门',   null,3, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(106, 101, '0,100,101',  '财务部门',   null,4, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(107, 101, '0,100,101',  '运维部门',   null,5, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(108, 102, '0,100,102',  '市场部门',   null,1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(109, 102, '0,100,102',  '财务部门',   null,2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);

insert into sys_config values(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 103, 1, now(), null, null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '用户管理-账号初始密码',         'sys.user.initPassword',         '123456',        'Y', 103, 1, now(), null, null, '初始化密码 123456' );
insert into sys_config values(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',           'theme-dark',    'Y', 103, 1, now(), null, null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(5, '账号自助-是否开启用户注册功能',   'sys.account.registerUser',      'false',         'Y', 103, 1, now(), null, null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(11, 'OSS预览列表资源开关',          'sys.oss.previewListResource',   'true',          'Y', 103, 1, now(), null, null, 'true:开启, false:关闭');
insert into sys_config values(12, '公共演示环境标志',          'sys.demo.enabled',   'true',          'Y', 103, 1, now(), null, null, 'true:开启, false:关闭');

insert into sys_dict_type values(1, '用户性别', 'sys_user_sex',        103, 1, now(), null, null, '用户性别列表');
insert into sys_dict_type values(2, '菜单状态', 'sys_show_hide',       103, 1, now(), null, null, '菜单状态列表');
insert into sys_dict_type values(3, '系统开关', 'sys_normal_disable',  103, 1, now(), null, null, '系统开关列表');
insert into sys_dict_type values(6, '系统是否', 'sys_yes_no',          103, 1, now(), null, null, '系统是否列表');
insert into sys_dict_type values(7, '通知类型', 'sys_notice_type',     103, 1, now(), null, null, '通知类型列表');
insert into sys_dict_type values(8, '通知状态', 'sys_notice_status',   103, 1, now(), null, null, '通知状态列表');
insert into sys_dict_type values(9, '操作类型', 'sys_oper_type',       103, 1, now(), null, null, '操作类型列表');
insert into sys_dict_type values(10, '系统状态', 'sys_common_status',  103, 1, now(), null, null, '登录状态列表');
insert into sys_dict_type values(11, '授权类型', 'sys_grant_type',     103, 1, now(), null, null, '认证授权类型');
insert into sys_dict_type values(12, '设备类型', 'sys_device_type',    103, 1, now(), null, null, '客户端设备类型');
-- 1. 工作流模板分类
INSERT INTO sys_dict_type (dict_id,  dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (13,  '工作流模板分类', 'km_workflow_template_category', 103, 1, now(), NULL, NULL, '工作流模板分类列表');


insert into sys_dict_data values(1, 1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', 103, 1, now(), null, null, '性别男');
insert into sys_dict_data values(2, 2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', 103, 1, now(), null, null, '性别女');
insert into sys_dict_data values(3, 3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', 103, 1, now(), null, null, '性别未知');
insert into sys_dict_data values(4, 1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', 103, 1, now(), null, null, '显示菜单');
insert into sys_dict_data values(5, 2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', 103, 1, now(), null, null, '隐藏菜单');
insert into sys_dict_data values(6, 1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(7, 2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', 103, 1, now(), null, null, '停用状态');
insert into sys_dict_data values(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', 103, 1, now(), null, null, '系统默认是');
insert into sys_dict_data values(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', 103, 1, now(), null, null, '系统默认否');
insert into sys_dict_data values(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', 103, 1, now(), null, null, '通知');
insert into sys_dict_data values(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', 103, 1, now(), null, null, '公告');
insert into sys_dict_data values(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', 103, 1, now(), null, null, '关闭状态');
insert into sys_dict_data values(29, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', 103, 1, now(), null, null, '其他操作');
insert into sys_dict_data values(18, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', 103, 1, now(), null, null, '新增操作');
insert into sys_dict_data values(19, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', 103, 1, now(), null, null, '修改操作');
insert into sys_dict_data values(20, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', 103, 1, now(), null, null, '删除操作');
insert into sys_dict_data values(21, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', 103, 1, now(), null, null, '授权操作');
insert into sys_dict_data values(22, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', 103, 1, now(), null, null, '导出操作');
insert into sys_dict_data values(23, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', 103, 1, now(), null, null, '导入操作');
insert into sys_dict_data values(24, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', 103, 1, now(), null, null, '强退操作');
insert into sys_dict_data values(25, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', 103, 1, now(), null, null, '生成操作');
insert into sys_dict_data values(26, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', 103, 1, now(), null, null, '清空操作');
insert into sys_dict_data values(27, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(28, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', 103, 1, now(), null, null, '停用状态');
insert into sys_dict_data values(30, 0,  '密码认证', 'password',   'sys_grant_type',   '',   'default', 'N', 103, 1, now(), null, null, '密码认证');
insert into sys_dict_data values(31, 0,  '短信认证', 'sms',        'sys_grant_type',   '',   'default', 'N', 103, 1, now(), null, null, '短信认证');
insert into sys_dict_data values(32, 0,  '邮件认证', 'email',      'sys_grant_type',   '',   'default', 'N', 103, 1, now(), null, null, '邮件认证');
insert into sys_dict_data values(33, 0,  '小程序认证', 'xcx',      'sys_grant_type',   '',   'default', 'N', 103, 1, now(), null, null, '小程序认证');
insert into sys_dict_data values(34, 0,  '三方登录认证', 'social', 'sys_grant_type',   '',   'default', 'N', 103, 1, now(), null, null, '三方登录认证');
insert into sys_dict_data values(35, 0,  'PC', 'pc',              'sys_device_type',   '',   'default', 'N', 103, 1, now(), null, null, 'PC');
insert into sys_dict_data values(36, 0,  '安卓', 'android',       'sys_device_type',   '',   'default', 'N', 103, 1, now(), null, null, '安卓');
insert into sys_dict_data values(37, 0,  'iOS', 'ios',            'sys_device_type',   '',   'default', 'N', 103, 1, now(), null, null, 'iOS');
insert into sys_dict_data values(38, 0,  '小程序', 'xcx',         'sys_device_type',   '',   'default', 'N', 103, 1, now(), null, null, '小程序');
-- 知识问答
INSERT INTO sys_dict_data (dict_code,  dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (39, 1, '知识问答', 'knowledge_qa', 'km_workflow_template_category', '', 'primary', 'N', 103, 1, now(), NULL, NULL, '知识问答类型的工作流模板');

-- 智能客服
INSERT INTO sys_dict_data (dict_code,  dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (40, 2, '智能客服', 'customer_service', 'km_workflow_template_category', '', 'success', 'N', 103, 1, now(), NULL, NULL, '智能客服类型的工作流模板');

-- 营销
INSERT INTO sys_dict_data (dict_code,  dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (41, 3, '营销', 'marketing', 'km_workflow_template_category', '', 'warning', 'N', 103, 1, now(), NULL, NULL, '营销类型的工作流模板');

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('3', '1');
insert into sys_role_menu values ('3', '5');
insert into sys_role_menu values ('3', '100');
insert into sys_role_menu values ('3', '101');
insert into sys_role_menu values ('3', '102');
insert into sys_role_menu values ('3', '103');
insert into sys_role_menu values ('3', '104');
insert into sys_role_menu values ('3', '105');
insert into sys_role_menu values ('3', '106');
insert into sys_role_menu values ('3', '107');
insert into sys_role_menu values ('3', '108');
insert into sys_role_menu values ('3', '118');
insert into sys_role_menu values ('3', '123');
insert into sys_role_menu values ('3', '500');
insert into sys_role_menu values ('3', '501');
insert into sys_role_menu values ('3', '1001');
insert into sys_role_menu values ('3', '1002');
insert into sys_role_menu values ('3', '1003');
insert into sys_role_menu values ('3', '1004');
insert into sys_role_menu values ('3', '1005');
insert into sys_role_menu values ('3', '1006');
insert into sys_role_menu values ('3', '1007');
insert into sys_role_menu values ('3', '1008');
insert into sys_role_menu values ('3', '1009');
insert into sys_role_menu values ('3', '1010');
insert into sys_role_menu values ('3', '1011');
insert into sys_role_menu values ('3', '1012');
insert into sys_role_menu values ('3', '1013');
insert into sys_role_menu values ('3', '1014');
insert into sys_role_menu values ('3', '1015');
insert into sys_role_menu values ('3', '1016');
insert into sys_role_menu values ('3', '1017');
insert into sys_role_menu values ('3', '1018');
insert into sys_role_menu values ('3', '1019');
insert into sys_role_menu values ('3', '1020');
insert into sys_role_menu values ('3', '1021');
insert into sys_role_menu values ('3', '1022');
insert into sys_role_menu values ('3', '1023');
insert into sys_role_menu values ('3', '1024');
insert into sys_role_menu values ('3', '1025');
insert into sys_role_menu values ('3', '1026');
insert into sys_role_menu values ('3', '1027');
insert into sys_role_menu values ('3', '1028');
insert into sys_role_menu values ('3', '1029');
insert into sys_role_menu values ('3', '1030');
insert into sys_role_menu values ('3', '1031');
insert into sys_role_menu values ('3', '1032');
insert into sys_role_menu values ('3', '1033');
insert into sys_role_menu values ('3', '1034');
insert into sys_role_menu values ('3', '1035');
insert into sys_role_menu values ('3', '1036');
insert into sys_role_menu values ('3', '1037');
insert into sys_role_menu values ('3', '1038');
insert into sys_role_menu values ('3', '1039');
insert into sys_role_menu values ('3', '1040');
insert into sys_role_menu values ('3', '1041');
insert into sys_role_menu values ('3', '1042');
insert into sys_role_menu values ('3', '1043');
insert into sys_role_menu values ('3', '1044');
insert into sys_role_menu values ('3', '1045');
insert into sys_role_menu values ('3', '1050');
insert into sys_role_menu values ('3', '1061');
insert into sys_role_menu values ('3', '1062');
insert into sys_role_menu values ('3', '1063');
insert into sys_role_menu values ('3', '1064');
insert into sys_role_menu values ('3', '1065');
insert into sys_role_menu values ('3', '1500');
insert into sys_role_menu values ('3', '1501');
insert into sys_role_menu values ('3', '1502');
insert into sys_role_menu values ('3', '1503');
insert into sys_role_menu values ('3', '1504');
insert into sys_role_menu values ('3', '1505');
insert into sys_role_menu values ('3', '1506');
insert into sys_role_menu values ('3', '1507');
insert into sys_role_menu values ('3', '1508');
insert into sys_role_menu values ('3', '1509');
insert into sys_role_menu values ('3', '1510');
insert into sys_role_menu values ('3', '1511');
insert into sys_role_menu values ('3', '1600');
insert into sys_role_menu values ('3', '1601');
insert into sys_role_menu values ('3', '1602');
insert into sys_role_menu values ('3', '1603');
insert into sys_role_menu values ('3', '1620');
insert into sys_role_menu values ('3', '1621');
insert into sys_role_menu values ('3', '1622');
insert into sys_role_menu values ('3', '1623');
insert into sys_role_menu values ('3', '11616');
insert into sys_role_menu values ('3', '11618');
insert into sys_role_menu values ('3', '11619');
insert into sys_role_menu values ('3', '11622');
insert into sys_role_menu values ('3', '11623');
insert into sys_role_menu values ('3', '11629');
insert into sys_role_menu values ('3', '11632');
insert into sys_role_menu values ('3', '11633');
insert into sys_role_menu values ('3', '11638');
insert into sys_role_menu values ('3', '11639');
insert into sys_role_menu values ('3', '11640');
insert into sys_role_menu values ('3', '11641');
insert into sys_role_menu values ('3', '11642');
insert into sys_role_menu values ('3', '11643');
insert into sys_role_menu values ('4', '5');
insert into sys_role_menu values ('4', '1500');
insert into sys_role_menu values ('4', '1501');
insert into sys_role_menu values ('4', '1502');
insert into sys_role_menu values ('4', '1503');
insert into sys_role_menu values ('4', '1504');
insert into sys_role_menu values ('4', '1505');
insert into sys_role_menu values ('4', '1506');
insert into sys_role_menu values ('4', '1507');
insert into sys_role_menu values ('4', '1508');
insert into sys_role_menu values ('4', '1509');
insert into sys_role_menu values ('4', '1510');
insert into sys_role_menu values ('4', '1511');

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, 103, 'ceo',  null, '董事长',    1, '0', 103, 1, now(), null, null, '');
insert into sys_post values(2, 100, 'se',   null, '项目经理',  2, '0', 103, 1, now(), null, null, '');
insert into sys_post values(3, 100, 'hr',   null, '人力资源',  3, '0', 103, 1, now(), null, null, '');
insert into sys_post values(4, 100, 'user', null, '普通员工',  4, '0', 103, 1, now(), null, null, '');

INSERT INTO km_model_provider(provider_id, provider_name, provider_key, provider_type, default_endpoint, site_url, icon_url, config_schema, status, sort, models, create_time) VALUES 
(1, 'OpenAI', 'openai', '1', 'https://api.openai.com/v1', 'https://openai.com', '/model-provider-icon/openai.png', NULL, '0', 1, '[{"modelKey": "gpt-4o", "modelType": "1"}, {"modelKey": "gpt-4o-mini", "modelType": "1"}, {"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-3.5-turbo", "modelType": "1"}, {"modelKey": "text-embedding-3-small", "modelType": "2"}, {"modelKey": "text-embedding-3-large", "modelType": "2"}, {"modelKey": "text-embedding-ada-002", "modelType": "2"}]', CURRENT_TIMESTAMP),
(2, 'Gemini', 'gemini', '1', 'https://generativelanguage.googleapis.com', 'https://ai.google.dev', '/model-provider-icon/gemini.svg', NULL, '0', 2, '[{"modelKey": "gemini-3-flash-preview", "modelType": "1"}, {"modelKey": "gemini-3-pro-preview", "modelType": "1"}, {"modelKey": "gemini-2.5-flash", "modelType": "1"}, {"modelKey": "text-embedding-004", "modelType": "2"}]', CURRENT_TIMESTAMP),
(3, 'Ollama', 'ollama', '2', 'http://localhost:11434', 'https://ollama.com', '/model-provider-icon/ollama.png', NULL, '0', 3, '[{"modelKey": "llama3", "modelType": "1"}, {"modelKey": "llama2", "modelType": "1"}, {"modelKey": "mistral", "modelType": "1"}, {"modelKey": "mixtral", "modelType": "1"}, {"modelKey": "phi3", "modelType": "1"}, {"modelKey": "qwen2", "modelType": "1"}, {"modelKey": "gemma2", "modelType": "1"}, {"modelKey": "nomic-embed-text", "modelType": "2"}, {"modelKey": "mxbai-embed-large", "modelType": "2"}]', CURRENT_TIMESTAMP),
(4, 'DeepSeek', 'deepseek', '1', 'https://api.deepseek.com', 'https://www.deepseek.com', '/model-provider-icon/deepseek.png', NULL, '0', 4, '[{"modelKey": "deepseek-chat", "modelType": "1"}, {"modelKey": "deepseek-coder", "modelType": "1"}]', CURRENT_TIMESTAMP),
(5, 'vLLM', 'vllm', '2', 'http://localhost:8000/v1', 'https://docs.vllm.ai', '/model-provider-icon/vllm.ico', NULL, '0', 5, '[]', CURRENT_TIMESTAMP),
(6, 'Azure OpenAI', 'azure', '1', 'https://{resource}.openai.azure.com', 'https://azure.microsoft.com/products/ai-services/openai-service', '/model-provider-icon/azure.png', NULL, '0', 6, '[{"modelKey": "gpt-4", "modelType": "1"}, {"modelKey": "gpt-4-turbo", "modelType": "1"}, {"modelKey": "gpt-35-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
(7, '阿里云百炼', 'bailian', '1', 'https://dashscope.aliyuncs.com/api/v1', 'https://www.aliyun.com/product/bailian', '/model-provider-icon/bailian.jpeg', NULL, '0', 7, '[{"modelKey": "qwen-max", "modelType": "1"}, {"modelKey": "qwen-plus", "modelType": "1"}, {"modelKey": "qwen-turbo", "modelType": "1"}, {"modelKey": "text-embedding-v1", "modelType": "2"}, {"modelKey": "text-embedding-v2", "modelType": "2"}]', CURRENT_TIMESTAMP),
(8, '智谱AI', 'zhipu', '1', 'https://open.bigmodel.cn/api/paas/v4', 'https://open.bigmodel.cn', '/model-provider-icon/zhipu.png', NULL, '0', 8, '[{"modelKey": "glm-4", "modelType": "1"}, {"modelKey": "glm-4-flash", "modelType": "1"}, {"modelKey": "glm-3-turbo", "modelType": "1"}]', CURRENT_TIMESTAMP),
(9, '火山引擎(豆包)', 'doubao', '1', 'https://ark.cn-beijing.volces.com/api/v3', 'https://www.volcengine.com/product/doubao', '/model-provider-icon/doubao.png', NULL, '0', 9, '[{"modelKey": "doubao-pro-32k", "modelType": "1"}, {"modelKey": "doubao-lite-32k", "modelType": "1"}]', CURRENT_TIMESTAMP),
(10, 'Moonshot', 'moonshot', '1', 'https://api.moonshot.cn/v1', 'https://www.moonshot.cn', '/model-provider-icon/moonshot.ico', NULL, '0', 10, '[{"modelKey": "moonshot-v1-8k", "modelType": "1"}, {"modelKey": "moonshot-v1-32k", "modelType": "1"}, {"modelKey": "moonshot-v1-128k", "modelType": "1"}]', CURRENT_TIMESTAMP);


-- 2. 节点定义数据 (1-10 号节点)
INSERT INTO km_node_definition
(node_def_id, node_type, node_label, node_icon, node_color, category, description, is_system, is_enabled, allow_custom_input_params, allow_custom_output_params, input_params, output_params, "version", parent_version_id, create_dept, create_by, create_time, update_by, update_time, remark) VALUES
(1, 'APP_INFO', '基础信息', 'mdi-information', '#64748BFF', 'basic', '应用的基础信息配置', '1', '1', '0', '0', '[]', '[]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(2, 'START', '开始', 'mdi-play-circle', '#64748BFF', 'basic', '工作流的入口节点', '1', '1', '0', '0', '[]', '[{"key": "userInput", "type": "string", "label": "用户输入", "required": true, "description": "用户的输入内容"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(3, 'END', '结束', 'mdi-stop-circle', '#64748BFF', 'basic', '工作流的结束节点，可以把各节点的输出参数引用进来，组合成最终回复消息作为工作流最终输出', '1', '1', '1', '0', '[{"key": "finalResponse", "type": "string", "label": "最终回复", "required": true, "description": "返回给用户的最终回复内容"}]', '[]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(4, 'LLM_CHAT', 'LLM 对话', 'mdi-robot', '#A855F7FF', 'ai', '调用大语言模型进行对话', '0', '1', '1', '1', '[{"key": "userInput", "type": "string", "label": "输入消息", "required": true, "description": "传递给 LLM 的输入消息"}, {"key": "chatContext", "type": "string", "label": "上下文", "required": false, "description": "比如可以传递知识库的检索结果", "defaultValue": ""}, {"key": "retrievedDocs", "type": "array", "label": "知识检索结果记录", "required": false, "description": "知识检索结果记录列表", "defaultValue": ""}]', '[{"key": "response", "type": "string", "label": "AI 回复", "required": true, "description": "LLM 生成的回复内容"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(5, 'INTENT_CLASSIFIER', '意图分类', 'mdi-sitemap', '#A855F7FF', 'ai', '识别用户输入的意图并分类', '0', '1', '0', '0', '[{"key": "instruction", "type": "string", "label": "文本指令", "required": true, "description": "需要分类的指令"}]', '[{"key": "intent", "type": "string", "label": "匹配的意图", "required": true, "description": "识别出的意图名称"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(6, 'CONDITION', '条件判断', 'mdi-source-branch', '#27560BFF', 'logic', '根据条件表达式进行分支判断', '0', '1', '0', '0', '[{"key": "matchedBranch", "type": "string", "label": "匹配的分支", "required": false, "description": "用于条件判断的值"}]', '[{"key": "matchedBranch", "type": "string", "label": "匹配的分支", "required": true, "description": "满足条件的分支名称"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(7, 'FIXED_RESPONSE', '指定回复', 'mdi-message-text', '#0F1A3ACF', 'action', '返回预设的固定文本内容', '0', '1', '1', '0', '[]', '[{"key": "response", "type": "string", "label": "回复内容", "required": true, "description": "固定的回复文本"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(8, 'DB_QUERY', '数据库对话', 'mdi-database-search', '#A855F7FF', 'ai', '结合LLM智能分析用户问题，生成SQL查询并返回自然语言回答', '0', '1', '0', '0', '[{"key": "userQuery", "type": "string", "label": "用户问题", "required": true, "description": "用户提出的业务问题"}]', '[{"key": "generatedSql", "type": "string", "label": "生成的SQL", "required": true, "description": "LLM生成的SQL语句"}, {"key": "queryResult", "type": "object", "label": "查询结果", "required": true, "description": "SQL执行结果(JSON)"}, {"key": "response", "type": "string", "label": "AI回复", "required": true, "description": "基于查询结果生成的自然语言回答"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(9, 'SQL_GENERATE', 'SQL生成', 'mdi-database-cog', '#8b5cf6', 'ai', '使用LLM分析用户问题，结合数据库元数据生成SQL语句', '0', '1', '0', '0', '[{"key":"userQuery","label":"用户问题","type":"string","required":true,"description":"用户提出的业务问题"}]', '[{"key":"generatedSql","label":"生成的SQL","type":"string","required":true,"description":"LLM生成的SQL语句"}]', 1, NULL, NULL, NULL, now(), NULL, NULL, NULL),
(10, 'SQL_EXECUTE', 'SQL执行', 'mdi-database-arrow-right', '#F59E0BFF', 'database', '执行SQL语句并返回查询结果', '0', '1', '0', '0', '[{"key": "sql", "type": "string", "label": "SQL语句", "required": true, "description": "待执行的SQL语句"}]', '[{"key": "queryResult", "type": "object", "label": "查询结果", "required": true, "description": "SQL执行结果(JSON)"}, {"key": "rowCount", "type": "number", "label": "返回行数", "required": true, "description": "查询返回的行数"}, {"key": "strResult", "type": "string", "label": "查询结果", "required": true, "description": "", "defaultValue": ""}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL),
(11, 'KNOWLEDGE_RETRIEVAL', '知识检索', 'mdi-book-search', '#F59E0BFF', 'database', '从知识库中检索相关文档片段，用于RAG对话', '0', '1', '0', '0', '[{"key": "query", "type": "string", "label": "查询文本", "required": true, "description": "用于检索的查询文本"}]', '[{"key": "context", "type": "string", "label": "检索上下文", "required": true, "description": "拼接后的上下文文本"}, {"key": "docCount", "type": "number", "label": "文档数量", "required": true, "description": "检索到的文档数量"}, {"key": "retrievedDocs", "type": "array", "label": "检索结果", "required": true, "description": "检索到的文档片段列表"}]', 1, NULL, NULL, NULL, now(), 1, now(), NULL);

-- 3. 连接规则数据 (1-44 号规则)
INSERT INTO km_node_connection_rule (rule_id, source_node_type, target_node_type, rule_type, priority, create_time) VALUES 
(1, 'START', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (2, 'START', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP), (3, 'START', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (4, 'START', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(5, 'LLM_CHAT', 'END', '0', 10, CURRENT_TIMESTAMP), (6, 'LLM_CHAT', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (7, 'LLM_CHAT', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (8, 'LLM_CHAT', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(9, 'INTENT_CLASSIFIER', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (10, 'INTENT_CLASSIFIER', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (11, 'INTENT_CLASSIFIER', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (12, 'INTENT_CLASSIFIER', 'END', '0', 10, CURRENT_TIMESTAMP),
(13, 'CONDITION', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (14, 'CONDITION', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (15, 'CONDITION', 'END', '0', 10, CURRENT_TIMESTAMP), (16, 'FIXED_RESPONSE', 'END', '0', 10, CURRENT_TIMESTAMP),
(17, 'START', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (18, 'LLM_CHAT', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (19, 'CONDITION', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP), (20, 'INTENT_CLASSIFIER', 'DB_QUERY', '0', 10, CURRENT_TIMESTAMP),
(21, 'DB_QUERY', 'END', '0', 10, CURRENT_TIMESTAMP), (22, 'DB_QUERY', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (23, 'DB_QUERY', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (24, 'DB_QUERY', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP), (25, 'DB_QUERY', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(26, 'START', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (27, 'LLM_CHAT', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (28, 'CONDITION', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP), (29, 'INTENT_CLASSIFIER', 'SQL_GENERATE', '0', 10, CURRENT_TIMESTAMP),
(30, 'SQL_GENERATE', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (31, 'SQL_GENERATE', 'END', '0', 10, CURRENT_TIMESTAMP), (32, 'SQL_GENERATE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (33, 'SQL_GENERATE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (34, 'SQL_GENERATE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP),
(35, 'START', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (37, 'LLM_CHAT', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (38, 'CONDITION', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP), (39, 'INTENT_CLASSIFIER', 'SQL_EXECUTE', '0', 10, CURRENT_TIMESTAMP),
(40, 'SQL_EXECUTE', 'END', '0', 10, CURRENT_TIMESTAMP), (41, 'SQL_EXECUTE', 'LLM_CHAT', '0', 10, CURRENT_TIMESTAMP), (42, 'SQL_EXECUTE', 'CONDITION', '0', 10, CURRENT_TIMESTAMP), (43, 'SQL_EXECUTE', 'FIXED_RESPONSE', '0', 10, CURRENT_TIMESTAMP), (44, 'SQL_EXECUTE', 'INTENT_CLASSIFIER', '0', 10, CURRENT_TIMESTAMP),
(50, 'START', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(51, 'LLM_CHAT', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(52, 'CONDITION', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(53, 'INTENT_CLASSIFIER', 'KNOWLEDGE_RETRIEVAL', '0', 10, NOW()),
(54, 'KNOWLEDGE_RETRIEVAL', 'LLM_CHAT', '0', 10, NOW()),
(55, 'KNOWLEDGE_RETRIEVAL', 'CONDITION', '0', 10, NOW()),
(56, 'KNOWLEDGE_RETRIEVAL', 'END', '0', 10, NOW());


-- 4. 菜单数据
-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '18', 'system',           null, '', '1', '0', 'M', '0', '0', '', 'system',   103, 1, now(), null, null, '系统管理目录');
insert into sys_menu values('2', '系统监控', '0', '3', 'monitor',          null, '', '1', '0', 'M', '0', '0', '', 'monitor',  103, 1, now(), null, null, '系统监控目录');
insert into sys_menu values('3', '系统工具', '0', '4', 'tool',             null, '', '1', '0', 'M', '0', '0', '', 'tool',     103, 1, now(), null, null, '系统工具目录');
insert into sys_menu values('4', 'KMatrix官网', '0', '999', 'http://www.kyxms.cn', null, '', '0', '0', 'M', '0', '0', '', 'guide',    103, 1, now(), null, null, 'KMatrix官网地址');
insert into sys_menu VALUES('5', '测试菜单', '0', '5', 'demo',             null, '', '1', '0', 'M', '0', '1', null, 'star',       103, 1, now(), null, null, '测试菜单');
-- 二级菜单
insert into sys_menu values('100',  '用户管理',     '1',   '1', 'user',             'system/user/index',            '', '1', '0', 'C', '0', '0', 'system:user:list',            'user',          103, 1, now(), null, null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理',     '1',   '2', 'role',             'system/role/index',            '', '1', '0', 'C', '0', '0', 'system:role:list',            'peoples',       103, 1, now(), null, null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理',     '1',   '3', 'menu',             'system/menu/index',            '', '1', '0', 'C', '0', '0', 'system:menu:list',            'tree-table',    103, 1, now(), null, null, '菜单管理菜单');
insert into sys_menu values('103',  '部门管理',     '1',   '4', 'dept',             'system/dept/index',            '', '1', '0', 'C', '0', '0', 'system:dept:list',            'tree',          103, 1, now(), null, null, '部门管理菜单');
insert into sys_menu values('104',  '岗位管理',     '1',   '5', 'post',             'system/post/index',            '', '1', '0', 'C', '0', '0', 'system:post:list',            'post',          103, 1, now(), null, null, '岗位管理菜单');
insert into sys_menu values('105',  '字典管理',     '1',   '6', 'dict',             'system/dict/index',            '', '1', '0', 'C', '0', '0', 'system:dict:list',            'dict',          103, 1, now(), null, null, '字典管理菜单');
insert into sys_menu values('106',  '参数设置',     '1',   '7', 'config',           'system/config/index',          '', '1', '0', 'C', '0', '0', 'system:config:list',          'edit',          103, 1, now(), null, null, '参数设置菜单');
insert into sys_menu values('107',  '通知公告',     '1',   '8', 'notice',           'system/notice/index',          '', '1', '0', 'C', '0', '0', 'system:notice:list',          'message',       103, 1, now(), null, null, '通知公告菜单');
insert into sys_menu values('108',  '日志管理',     '1',   '9', 'log',              '',                             '', '1', '0', 'M', '0', '0', '',                            'log',           103, 1, now(), null, null, '日志管理菜单');
insert into sys_menu values('109',  '在线用户',     '2',   '1', 'online',           'monitor/online/index',         '', '1', '0', 'C', '0', '0', 'monitor:online:list',         'online',        103, 1, now(), null, null, '在线用户菜单');
insert into sys_menu values('113',  '缓存监控',     '2',   '5', 'cache',            'monitor/cache/index',          '', '1', '0', 'C', '0', '0', 'monitor:cache:list',          'redis',         103, 1, now(), null, null, '缓存监控菜单');
insert into sys_menu values('115',  '代码生成',     '3',   '2', 'gen',              'tool/gen/index',               '', '1', '0', 'C', '0', '0', 'tool:gen:list',               'code',          103, 1, now(), null, null, '代码生成菜单');
insert into sys_menu values('123',  '客户端管理',   '1',   '11', 'client',           'system/client/index',          '', '1', '0', 'C', '0', '0', 'system:client:list',          'international', 103, 1, now(), null, null, '客户端管理菜单');
insert into sys_menu values('133', '文件配置管理',  '1',   '10', 'oss-config/index',              'system/oss-config/index', '', '1', '1', 'C', '1', '0', 'system:ossConfig:list',  '#',                103, 1, now(), null, null, '/system/oss');
-- oss菜单
insert into sys_menu values('118',  '文件管理',     '1',   '10', 'oss',              'system/oss/index',            '', '1', '0', 'C', '0', '0', 'system:oss:list',             'upload',        103, 1, now(), null, null, '文件管理菜单');
-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '1', '0', 'C', '0', '0', 'monitor:operlog:list',    'form',          103, 1, now(), null, null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '1', '0', 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    103, 1, now(), null, null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1001', '用户查询', '100', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1002', '用户新增', '100', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1003', '用户修改', '100', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1004', '用户删除', '100', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1005', '用户导出', '100', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:export',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1006', '用户导入', '100', '6',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:import',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1007', '重置密码', '100', '7',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd',       '#', 103, 1, now(), null, null, '');
-- 角色管理按钮
insert into sys_menu values('1008', '角色查询', '101', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1009', '角色新增', '101', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1010', '角色修改', '101', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1011', '角色删除', '101', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1012', '角色导出', '101', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:export',         '#', 103, 1, now(), null, null, '');
-- 菜单管理按钮
insert into sys_menu values('1013', '菜单查询', '102', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1014', '菜单新增', '102', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1015', '菜单修改', '102', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1016', '菜单删除', '102', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:remove',         '#', 103, 1, now(), null, null, '');
-- 部门管理按钮
insert into sys_menu values('1017', '部门查询', '103', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1018', '部门新增', '103', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1019', '部门修改', '103', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1020', '部门删除', '103', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:remove',         '#', 103, 1, now(), null, null, '');
-- 岗位管理按钮
insert into sys_menu values('1021', '岗位查询', '104', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1022', '岗位新增', '104', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1023', '岗位修改', '104', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1024', '岗位删除', '104', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1025', '岗位导出', '104', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:export',         '#', 103, 1, now(), null, null, '');
-- 字典管理按钮
insert into sys_menu values('1026', '字典查询', '105', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1027', '字典新增', '105', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1028', '字典修改', '105', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1029', '字典删除', '105', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1030', '字典导出', '105', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:export',         '#', 103, 1, now(), null, null, '');
-- 参数设置按钮
insert into sys_menu values('1031', '参数查询', '106', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1032', '参数新增', '106', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1033', '参数修改', '106', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:edit',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1034', '参数删除', '106', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:remove',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1035', '参数导出', '106', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:export',       '#', 103, 1, now(), null, null, '');
-- 通知公告按钮
insert into sys_menu values('1036', '公告查询', '107', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1037', '公告新增', '107', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1038', '公告修改', '107', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:edit',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1039', '公告删除', '107', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:remove',       '#', 103, 1, now(), null, null, '');
-- 操作日志按钮
insert into sys_menu values('1040', '操作查询', '500', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:query',      '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1041', '操作删除', '500', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:remove',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1042', '日志导出', '500', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:export',     '#', 103, 1, now(), null, null, '');
-- 登录日志按钮
insert into sys_menu values('1043', '登录查询', '501', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:query',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1044', '登录删除', '501', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:remove',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1045', '日志导出', '501', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:export',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1050', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 103, 1, now(), null, null, '');
-- 在线用户按钮
insert into sys_menu values('1046', '在线查询', '109', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:query',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1047', '批量强退', '109', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1048', '单条强退', '109', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, now(), null, null, '');
-- 代码生成按钮
insert into sys_menu values('1055', '生成查询', '115', '1', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:query',             '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1056', '生成修改', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:edit',              '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1057', '生成删除', '115', '3', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:remove',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1058', '导入代码', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:import',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1059', '预览代码', '115', '4', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:preview',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1060', '生成代码', '115', '5', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:code',              '#', 103, 1, now(), null, null, '');
-- oss相关按钮
insert into sys_menu values('1600', '文件查询', '118', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1601', '文件上传', '118', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:upload',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1602', '文件下载', '118', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:download',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1603', '文件删除', '118', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:remove',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1620', '配置列表', '118', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:list',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1621', '配置添加', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:add',    '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1622', '配置编辑', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:edit',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1623', '配置删除', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:ossConfig:remove', '#', 103, 1, now(), null, null, '');
-- 客户端管理按钮
insert into sys_menu values('1061', '客户端管理查询', '123', '1',  '#', '', '', '1', '0', 'F', '0', '0', 'system:client:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1062', '客户端管理新增', '123', '2',  '#', '', '', '1', '0', 'F', '0', '0', 'system:client:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1063', '客户端管理修改', '123', '3',  '#', '', '', '1', '0', 'F', '0', '0', 'system:client:edit',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1064', '客户端管理删除', '123', '4',  '#', '', '', '1', '0', 'F', '0', '0', 'system:client:remove',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1065', '客户端管理导出', '123', '5',  '#', '', '', '1', '0', 'F', '0', '0', 'system:client:export',       '#', 103, 1, now(), null, null, '');

-- AI相关
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES 
(2000, 'AI 管理', 0, 1, 'ai', NULL, 1, 0, 'M', '0', '0', '', 'robot', 1, CURRENT_TIMESTAMP, 'AI模块根菜单'),
(2001, '模型管理', 2000, 1, 'model-manager', 'ai/model-manager/index', 1, 0, 'C', '0', '0', 'ai:model:list', 'mdi-robot-outline', 1, CURRENT_TIMESTAMP, '模型管理菜单'),
(2018, '知识库管理', 2000, 2, 'knowledge-manager', 'ai/knowledge-manager/index', 1, 0, 'C', '0', '0', 'ai:knowledge:list', 'mdi-database', 1, NOW(), '知识库管理'),
(2002, '应用管理', 2000, 3, 'app-manager', 'ai/app-manager/index', 1, 0, 'C', '0', '0', 'ai:app:list', 'mdi-application', 1, CURRENT_TIMESTAMP, 'AI应用管理菜单'),
(2020, '工作流模板', 2000, 4, 'workflow-template', 'ai/workflow-template/index', '1', '0', 'C', '0', '0', 'ai:workflowTemplate:list', 'mdi-workflow', 1, NOW(), '工作流模板管理'),
(2015, '节点定义', 2000, 5, 'node-definition', 'ai/node-definition/index', 1, 1, 'C', '0', '0', 'ai:nodeDefinition:list', 'mdi-menu', 1, CURRENT_TIMESTAMP, ''),
(2016, '数据源管理', 2000, 6, 'datasource-manager', 'ai/datasource-manager/index', 1, 1, 'C', '0', '0', 'ai:datasourceManager:list', 'mdi-database-plus', 1, CURRENT_TIMESTAMP, ''),
(2003, '应用查询', 2002, 1, '', '', 1, 0, 'F', '0', '0', 'ai:app:query', '#', 1, CURRENT_TIMESTAMP, ''),
(2004, '应用新增', 2002, 2, '', '', 1, 0, 'F', '0', '0', 'ai:app:add', '#', 1, CURRENT_TIMESTAMP, ''),
(2005, '应用修改', 2002, 3, '', '', 1, 0, 'F', '0', '0', 'ai:app:edit', '#', 1, CURRENT_TIMESTAMP, ''),
(2006, '应用删除', 2002, 4, '', '', 1, 0, 'F', '0', '0', 'ai:app:remove', '#', 1, CURRENT_TIMESTAMP, ''),
(2007, '应用导出', 2002, 5, '', '', 1, 0, 'F', '0', '0', 'ai:app:export', '#', 1, CURRENT_TIMESTAMP, ''),
(2008, '工作流查询', 2002, 6, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:list', '#', 1, CURRENT_TIMESTAMP, ''),
(2009, '工作流保存', 2002, 7, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:add,ai:workflow:edit', '#', 1, CURRENT_TIMESTAMP, ''),
(2010, '工作流编排', 2000, 7, 'workflow', 'ai/workflow/index', 1, 1, 'C', '1', '0', 'ai:app:workflow', '#', 1, CURRENT_TIMESTAMP, '工作流编排页面（隐藏）'),
(2011, 'AI对话', 2000, 8, 'chat', 'ai/chat/index', 1, 1, 'C', '1', '0', 'ai:chat:view', 'chat', 1, CURRENT_TIMESTAMP, 'AI聊天对话页面'),
(2012, '发送消息', 2011, 1, '', '', 1, 0, 'F', '0', '0', 'ai:chat:send', '#', 1, CURRENT_TIMESTAMP, ''),
(2013, '查看历史', 2011, 2, '', '', 1, 0, 'F', '0', '0', 'ai:chat:history', '#', 1, CURRENT_TIMESTAMP, ''),
(2014, '清空对话', 2011, 3, '', '', 1, 0, 'F', '0', '0', 'ai:chat:clear', '#', 1, CURRENT_TIMESTAMP, ''),
(2017, 'APP详情', 2000, 9, 'app-detail', 'ai/app-detail/index', 1, 1, 'C', '1', '0', 'ai:appDetail:view', 'mdi-menu', 1, CURRENT_TIMESTAMP,''),
(2019, '知识库详情', 2000, 10, 'knowledge-detail', 'ai/knowledge-detail/index', 1, 1, 'C', '1', '0', 'ai:knowledge:view', 'mdi-database-search', 1, NOW(), '知识库详情'),
(2021, '模板查询', 2020, 1, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:query', '#', 1, NOW(), ''),
(2022, '模板新增', 2020, 2, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:add', '#', 1, NOW(), ''),
(2023, '模板修改', 2020, 3, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:edit', '#', 1, NOW(), ''),
(2024, '模板删除', 2020, 4, '', '', '1', '0', 'F', '0', '0', 'ai:workflowTemplate:remove', '#', 1, NOW(), ''),
(2025, '模板工作流编排', 2000, 11, 'template-editor', 'ai/template-editor/index', '1', '1', 'C', '1', '0', 'ai:templateEditor:view', 'mdi-database-search', 1, NOW(), '模板工作流编排'),
(2026, '分块管理', 2000, 12, 'chunk-manager', 'ai/chunk-manager/index', '1', '1', 'C', '1', '0', 'ai:chunkManager:list', 'mdi-database-search', 1, NOW(), '分块管理');

INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id <= 2026;

-- ======================================================================
-- AI 模块补充按钮级接口权限 (菜单 ID 2027-2060)
-- 补充各控制器 @SaCheckPermission 对应的按钮级菜单权限，
-- 与已有页面菜单形成完整的 列表/查询/新增/修改/删除 权限体系
-- ======================================================================

-- ----------------------------
-- 模型管理按钮权限 (parent: 2001)
-- KmModelController: ai:model:list 已有；补充 query/add/edit/remove
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2027, '模型查询', 2001, 1, '', '', 1, 0, 'F', '0', '0', 'ai:model:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2028, '模型新增', 2001, 2, '', '', 1, 0, 'F', '0', '0', 'ai:model:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2029, '模型修改', 2001, 3, '', '', 1, 0, 'F', '0', '0', 'ai:model:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2030, '模型删除', 2001, 4, '', '', 1, 0, 'F', '0', '0', 'ai:model:remove', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 工作流模板按钮权限 (parent: 2020)
-- KmWorkflowTemplateController 使用 ai:workflowTemplate:* (驼峰，与已有菜单保持一致)
-- SQL 已有: 2021=query, 2022=add, 2023=edit, 2024=remove，无需重复，仅追加列表查询权限
-- 注意: KmWorkflowTemplateController 代码中使用了 ai:workflow-template:* (连字符)，
--       与以下 SQL 权限标识不一致，建议统一为驼峰格式
-- ----------------------------
-- (工作流模板已有 query/add/edit/remove 四个按钮，菜单 2021-2024，无需补充)

-- ----------------------------
-- 节点定义按钮权限 (parent: 2015)
-- KmNodeDefinitionController: ai:workflow:node:list/query/add/edit/remove
-- 已有 2015 节点定义页面菜单(ai:nodeDefinition:list)；補充操作按钮
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2031, '节点查询', 2015, 1, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:node:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2032, '节点新增', 2015, 2, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:node:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2033, '节点修改', 2015, 3, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:node:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2034, '节点删除', 2015, 4, '', '', 1, 0, 'F', '0', '0', 'ai:workflow:node:remove', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 数据源管理按钮权限 (parent: 2016)
-- KmDataSourceController / KmDatabaseMetaController: ai:datasource:list/query/add/edit/remove
-- 已有 2016 数据源管理页面菜单(ai:datasourceManager:list)；补充操作按钮
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2035, '数据源列表', 2016, 1, '', '', 1, 0, 'F', '0', '0', 'ai:datasource:list',   '#', 1, CURRENT_TIMESTAMP, ''),
(2036, '数据源查询', 2016, 2, '', '', 1, 0, 'F', '0', '0', 'ai:datasource:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2037, '数据源新增', 2016, 3, '', '', 1, 0, 'F', '0', '0', 'ai:datasource:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2038, '数据源修改', 2016, 4, '', '', 1, 0, 'F', '0', '0', 'ai:datasource:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2039, '数据源删除', 2016, 5, '', '', 1, 0, 'F', '0', '0', 'ai:datasource:remove', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 将以上补充的按钮权限分配给超级管理员角色(role_id=1)
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2027 AND menu_id <= 2039;

-- ======================================================================
-- 知识库相关控制器按钮级接口权限 (菜单 ID 2040-2075)
-- KmKnowledgeBaseController / KmDocumentController / KmDocumentChunkController
-- KmQuestionController / KmRetrievalController
-- ======================================================================

-- ----------------------------
-- 知识库管理按钮权限 (parent: 2018)
-- KmKnowledgeBaseController: ai:knowledge:list 已有页面菜单；补充操作按钮
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2040, '知识库查询', 2018, 1, '', '', 1, 0, 'F', '0', '0', 'ai:knowledge:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2041, '知识库新增', 2018, 2, '', '', 1, 0, 'F', '0', '0', 'ai:knowledge:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2042, '知识库修改', 2018, 3, '', '', 1, 0, 'F', '0', '0', 'ai:knowledge:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2043, '知识库删除', 2018, 4, '', '', 1, 0, 'F', '0', '0', 'ai:knowledge:remove', '#', 1, CURRENT_TIMESTAMP, ''),
(2044, '知识库检索', 2018, 5, '', '', 1, 0, 'F', '0', '0', 'ai:retrieval:search', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 文档管理按钮权限 (parent: 2019 知识库详情页)
-- KmDocumentController: ai:document:list/query/add/edit/remove/download
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2050, '文档列表', 2019, 1, '', '', 1, 0, 'F', '0', '0', 'ai:document:list',     '#', 1, CURRENT_TIMESTAMP, ''),
(2051, '文档查询', 2019, 2, '', '', 1, 0, 'F', '0', '0', 'ai:document:query',    '#', 1, CURRENT_TIMESTAMP, ''),
(2052, '文档上传', 2019, 3, '', '', 1, 0, 'F', '0', '0', 'ai:document:add',      '#', 1, CURRENT_TIMESTAMP, ''),
(2053, '文档修改', 2019, 4, '', '', 1, 0, 'F', '0', '0', 'ai:document:edit',     '#', 1, CURRENT_TIMESTAMP, ''),
(2054, '文档删除', 2019, 5, '', '', 1, 0, 'F', '0', '0', 'ai:document:remove',   '#', 1, CURRENT_TIMESTAMP, ''),
(2055, '文档下载', 2019, 6, '', '', 1, 0, 'F', '0', '0', 'ai:document:download', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 切片管理按钮权限 (parent: 2026 分块管理页)
-- KmDocumentChunkController: ai:chunk:list/query/add/edit/remove
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2060, '切片列表', 2026, 1, '', '', 1, 0, 'F', '0', '0', 'ai:chunk:list',   '#', 1, CURRENT_TIMESTAMP, ''),
(2061, '切片查询', 2026, 2, '', '', 1, 0, 'F', '0', '0', 'ai:chunk:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2062, '切片新增', 2026, 3, '', '', 1, 0, 'F', '0', '0', 'ai:chunk:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2063, '切片修改', 2026, 4, '', '', 1, 0, 'F', '0', '0', 'ai:chunk:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2064, '切片删除', 2026, 5, '', '', 1, 0, 'F', '0', '0', 'ai:chunk:remove', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 问题管理按钮权限 (parent: 2026 分块管理页)
-- KmQuestionController: ai:question:list/query/add/edit/remove
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2070, '问题列表', 2026, 6,  '', '', 1, 0, 'F', '0', '0', 'ai:question:list',   '#', 1, CURRENT_TIMESTAMP, ''),
(2071, '问题查询', 2026, 7,  '', '', 1, 0, 'F', '0', '0', 'ai:question:query',  '#', 1, CURRENT_TIMESTAMP, ''),
(2072, '问题新增', 2026, 8,  '', '', 1, 0, 'F', '0', '0', 'ai:question:add',    '#', 1, CURRENT_TIMESTAMP, ''),
(2073, '问题修改', 2026, 9,  '', '', 1, 0, 'F', '0', '0', 'ai:question:edit',   '#', 1, CURRENT_TIMESTAMP, ''),
(2074, '问题删除', 2026, 10, '', '', 1, 0, 'F', '0', '0', 'ai:question:remove', '#', 1, CURRENT_TIMESTAMP, '');

-- ----------------------------
-- 将以上知识库相关按钮权限分配给超级管理员角色(role_id=1)
-- ----------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2040 AND menu_id <= 2074;


-- 内置系统模板: 标准知识库问答
INSERT INTO km_workflow_template
(template_id, template_name, template_code, description, icon, category, scope_type, workflow_config, graph_data, "version", parent_version_id, is_published, publish_time, is_enabled, use_count, create_dept, create_by, create_time, update_by, update_time, remark, dsl_data) VALUES
(1, '标准知识库问答', 'standard', '标准知识库问答', 'mdi-file-document-outline', 'knowledge_qa', '0', '{}', '{"edges": [{"id": "e-start--knowledge_retrieval-1770706944479", "data": {}, "type": "custom", "source": "start", "target": "knowledge_retrieval-1770706944479", "animated": false, "updatable": "target", "sourceHandle": null, "targetHandle": null}, {"id": "e-knowledge_retrieval-1770706944479--llm_chat-1770706948229", "data": {}, "type": "custom", "source": "knowledge_retrieval-1770706944479", "target": "llm_chat-1770706948229", "animated": false, "updatable": "target", "sourceHandle": null, "targetHandle": null}, {"id": "e-llm_chat-1770706948229--end", "data": {}, "type": "custom", "source": "llm_chat-1770706948229", "target": "end", "animated": false, "updatable": "target", "sourceHandle": null, "targetHandle": null}], "nodes": [{"id": "start", "data": {"id": "start", "config": {"globalParams": [{"key": "userId", "type": "string", "label": "用户ID", "required": true}, {"key": "userName", "type": "string", "label": "用户名称", "required": true}, {"key": "sessionId", "type": "string", "label": "会话ID", "required": true}, {"key": "historyContext", "type": "array", "label": "历史上下文", "required": true}]}, "status": "idle", "nodeIcon": "mdi-play-circle", "nodeType": "START", "nodeColor": "#64748BFF", "nodeLabel": "开始", "description": "工作流的入口节点", "paramBindings": [], "customInputParams": [], "customOutputParams": []}, "type": "custom", "position": {"x": 50, "y": 160}}, {"id": "end", "data": {"id": "end", "status": "idle", "nodeIcon": "mdi-stop-circle", "nodeType": "END", "nodeColor": "#64748BFF", "nodeLabel": "结束", "description": "工作流的结束节点，可以把各节点的输出参数引用进来，组合成最终回复消息作为工作流最终输出", "paramBindings": [{"paramKey": "finalResponse", "sourceKey": "llm_chat-1770706948229", "sourceType": "node", "sourceParam": "response"}], "customInputParams": [], "customOutputParams": []}, "type": "custom", "position": {"x": 1730, "y": 186}}, {"id": "knowledge_retrieval-1770706944479", "data": {"id": "knowledge_retrieval-1770706944479", "config": {"mode": "HYBRID", "topK": 5, "kbIds": [], "threshold": 0.5, "datasetIds": [], "enableRerank": true, "emptyResponse": ""}, "status": "idle", "isSystem": "0", "nodeIcon": "mdi-book-search", "nodeType": "KNOWLEDGE_RETRIEVAL", "nodeColor": "#F59E0BFF", "nodeLabel": "知识检索 1", "description": "从知识库中检索相关文档片段，用于RAG对话", "paramBindings": [{"paramKey": "query", "sourceKey": "start", "sourceType": "node", "sourceParam": "userInput"}], "customInputParams": [], "customOutputParams": []}, "type": "custom", "position": {"x": 522, "y": 50}}, {"id": "llm_chat-1770706948229", "data": {"id": "llm_chat-1770706948229", "config": {"modelId": null, "maxTokens": 2048, "userPrompt": "已知信息：${chatContext}\n问题：${userInput}", "temperature": 0.7, "streamOutput": true, "systemPrompt": ""}, "status": "idle", "isSystem": "0", "nodeIcon": "mdi-robot", "nodeType": "LLM_CHAT", "nodeColor": "#A855F7FF", "nodeLabel": "LLM 对话 1", "description": "调用大语言模型进行对话", "paramBindings": [{"paramKey": "userInput", "sourceKey": "start", "sourceType": "node", "sourceParam": "userInput"}, {"paramKey": "chatContext", "sourceKey": "knowledge_retrieval-1770706944479", "sourceType": "node", "sourceParam": "context"}, {"paramKey": "retrievedDocs", "sourceKey": "knowledge_retrieval-1770706944479", "sourceType": "node", "sourceParam": "retrievedDocs"}], "customInputParams": [], "customOutputParams": []}, "type": "custom", "position": {"x": 1126, "y": 157}}]}', 1, NULL, '0', NULL, '1', 0, 103, 1, '2026-02-10 14:51:11.698', 1, '2026-02-10 15:03:42.356', NULL, '{"name": "标准知识库问答-2", "edges": [{"to": "knowledge_retrieval-1770706944479", "from": "start"}, {"to": "llm_chat-1770706948229", "from": "knowledge_retrieval-1770706944479"}, {"to": "end", "from": "llm_chat-1770706948229"}], "nodes": [{"id": "start", "name": "开始", "type": "START", "config": {"globalParams": [{"key": "userId", "type": "string", "label": "用户ID", "required": true}, {"key": "userName", "type": "string", "label": "用户名称", "required": true}, {"key": "sessionId", "type": "string", "label": "会话ID", "required": true}, {"key": "historyContext", "type": "array", "label": "历史上下文", "required": true}]}, "inputs": {}}, {"id": "end", "name": "结束", "type": "END", "config": {}, "inputs": {"finalResponse": "${llm_chat-1770706948229.response}"}}, {"id": "knowledge_retrieval-1770706944479", "name": "知识检索 1", "type": "KNOWLEDGE_RETRIEVAL", "config": {"mode": "HYBRID", "topK": 5, "kbIds": [], "threshold": 0.5, "datasetIds": [], "enableRerank": true, "emptyResponse": ""}, "inputs": {"query": "${start.userInput}"}}, {"id": "llm_chat-1770706948229", "name": "LLM 对话 1", "type": "LLM_CHAT", "config": {"modelId": null, "maxTokens": 2048, "userPrompt": "已知信息：${chatContext}\n问题：${userInput}", "temperature": 0.7, "streamOutput": true, "systemPrompt": ""}, "inputs": {"userInput": "${start.userInput}", "chatContext": "${knowledge_retrieval-1770706944479.context}", "retrievedDocs": "${knowledge_retrieval-1770706944479.retrievedDocs}"}}], "entryPoint": "start"}');
