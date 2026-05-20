-- =====================================================
-- V0.0.10 增加blog功能
-- =====================================================

CREATE TABLE IF NOT EXISTS km_blog_category (
    id            BIGSERIAL       NOT NULL,
    parent_id     BIGINT          NOT NULL DEFAULT 0,           -- 父分类ID，0=根节点（专题节点）
    name          VARCHAR(100)    NOT NULL,                     -- 分类名称
    path          VARCHAR(500)    NOT NULL,                     -- 完整路径，如 "/ai/workflow"
    order_num     INT             NOT NULL DEFAULT 0,           -- 排序
    source        VARCHAR(20)     NOT NULL DEFAULT 'FILE',      -- 来源：FILE=扫描入库, ONLINE=管理端创建
    is_topic      CHAR(1)         NOT NULL DEFAULT '0',         -- 是否为专题节点（顶层）：1=是, 0=否
    dataset_id    BIGINT          DEFAULT NULL,                 -- 关联知识库数据集ID（仅专题节点有效）
    topic_slug    VARCHAR(200)    DEFAULT NULL,                 -- 专题URL标识，如 "ai-engineering"（仅专题节点有效）
    custom_domain VARCHAR(500)    DEFAULT NULL,                 -- 绑定的独立域名（可选）
    del_flag      CHAR(1)         NOT NULL DEFAULT '0',
    create_dept   BIGINT          DEFAULT NULL,
    create_by     BIGINT          DEFAULT NULL,
    create_time   TIMESTAMP       DEFAULT NULL,
    update_by     BIGINT          DEFAULT NULL,
    update_time   TIMESTAMP       DEFAULT NULL,
    git_token_encrypted VARCHAR(1000) DEFAULT NULL,
    git_owner     VARCHAR(200)    DEFAULT NULL,
    git_repo      VARCHAR(200)    DEFAULT NULL,
    git_branch    VARCHAR(100)    DEFAULT 'master',
    git_root_path VARCHAR(500)    DEFAULT NULL,
    git_platform  VARCHAR(20)     DEFAULT 'gitee',
    CONSTRAINT pk_km_blog_category PRIMARY KEY (id),
    CONSTRAINT uq_blog_category_path UNIQUE (path),
    CONSTRAINT uq_blog_category_topic_slug UNIQUE (topic_slug)
    );

CREATE INDEX IF NOT EXISTS idx_blog_category_parent_id  ON km_blog_category (parent_id);
CREATE INDEX IF NOT EXISTS idx_blog_category_is_topic   ON km_blog_category (is_topic);
CREATE INDEX IF NOT EXISTS idx_blog_category_topic_slug ON km_blog_category (topic_slug);

COMMENT ON TABLE km_blog_category IS '博客分类表（顶层节点即为专题，is_topic=1标识）';
COMMENT ON COLUMN km_blog_category.id IS '主键ID';
COMMENT ON COLUMN km_blog_category.parent_id IS '父分类ID，0=根节点（专题节点）';
COMMENT ON COLUMN km_blog_category.name IS '分类名称';
COMMENT ON COLUMN km_blog_category.path IS '完整路径，如 /ai/workflow';
COMMENT ON COLUMN km_blog_category.order_num IS '排序';
COMMENT ON COLUMN km_blog_category.source IS '来源：FILE=扫描入库, ONLINE=管理端创建, GIT=Git仓库（path字段存储git URL）';
COMMENT ON COLUMN km_blog_category.is_topic IS '是否为专题节点（顶层）：1=是, 0=否';
COMMENT ON COLUMN km_blog_category.dataset_id IS '关联知识库数据集ID（仅专题节点有效）';
COMMENT ON COLUMN km_blog_category.topic_slug IS '专题URL标识，如 ai-engineering（仅专题节点有效，全局唯一）';
COMMENT ON COLUMN km_blog_category.custom_domain IS '绑定的独立域名（可选）';
COMMENT ON COLUMN km_blog_category.del_flag IS '删除标志（0=未删除，1=已删除）';
COMMENT ON COLUMN km_blog_category.git_token_encrypted IS 'AES-256 加密后的 GitHub PAT（Personal Access Token）';
COMMENT ON COLUMN km_blog_category.git_owner           IS 'Git 仓库 owner（用户名或组织名）';
COMMENT ON COLUMN km_blog_category.git_repo            IS 'Git 仓库名称';
COMMENT ON COLUMN km_blog_category.git_branch          IS 'Git 默认分支，默认为 main';
COMMENT ON COLUMN km_blog_category.git_root_path       IS 'Git 仓库子目录路径，NULL 表示仓库根目录';
COMMENT ON COLUMN km_blog_category.git_platform IS 'Git 平台（github=GitHub，gitee=Gitee），默认 gitee';

-- ======================================================================
-- 表: km_blog_article（博客文章）
-- ======================================================================
CREATE TABLE IF NOT EXISTS km_blog_article (
    id             BIGSERIAL       NOT NULL,
    category_id    BIGINT          NOT NULL DEFAULT 0,          -- 关联分类ID
    title          VARCHAR(500)    NOT NULL,                    -- 文章标题
    slug           VARCHAR(300)    NOT NULL,                    -- URL标识，全局唯一
    content        TEXT            DEFAULT NULL,                -- Markdown原文
    description    VARCHAR(1000)   DEFAULT NULL,                -- SEO摘要
    cover_image    VARCHAR(1000)   DEFAULT NULL,                -- 封面图URL
    tags           VARCHAR(2000)   DEFAULT NULL,                -- JSON数组字符串，如 ["Java","AI"]
    status         VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',    -- 状态：DRAFT=草稿, PUBLISHED=已发布
    source         VARCHAR(20)     NOT NULL DEFAULT 'FILE',     -- 来源：FILE=扫描入库, ONLINE=管理端创建
    dataset_id     BIGINT          DEFAULT NULL,                -- 文章级datasetId（可选，覆盖专题级别）
    km_document_id BIGINT          DEFAULT NULL,                -- 同步到知识库后的文档ID
    source_path    VARCHAR(1000)   DEFAULT NULL,                -- 源文件相对路径（Scanner用），如 ai/langgraph.md
    content_hash   VARCHAR(64)     DEFAULT NULL,                -- MD5，增量更新检测
    published_at   TIMESTAMP       DEFAULT NULL,                -- 发布时间
    view_count     INT             NOT NULL DEFAULT 0,          -- 浏览次数
    del_flag       CHAR(1)         NOT NULL DEFAULT '0',
    create_dept    BIGINT          DEFAULT NULL,
    create_by      BIGINT          DEFAULT NULL,
    create_time    TIMESTAMP       DEFAULT NULL,
    update_by      BIGINT          DEFAULT NULL,
    update_time    TIMESTAMP       DEFAULT NULL,
    CONSTRAINT pk_km_blog_article PRIMARY KEY (id),
    CONSTRAINT uq_blog_article_slug UNIQUE (slug),
    CONSTRAINT uq_blog_article_source_path UNIQUE (source_path)
    );

CREATE INDEX IF NOT EXISTS idx_blog_article_category    ON km_blog_article (category_id);
CREATE INDEX IF NOT EXISTS idx_blog_article_status      ON km_blog_article (status);
CREATE INDEX IF NOT EXISTS idx_blog_article_source      ON km_blog_article (source);
CREATE INDEX IF NOT EXISTS idx_blog_article_published   ON km_blog_article (published_at DESC);

COMMENT ON TABLE km_blog_article IS '博客文章表';
COMMENT ON COLUMN km_blog_article.id IS '主键ID';
COMMENT ON COLUMN km_blog_article.category_id IS '关联分类ID';
COMMENT ON COLUMN km_blog_article.title IS '文章标题';
COMMENT ON COLUMN km_blog_article.slug IS 'URL标识，全局唯一';
COMMENT ON COLUMN km_blog_article.content IS 'Markdown原文';
COMMENT ON COLUMN km_blog_article.description IS 'SEO摘要';
COMMENT ON COLUMN km_blog_article.cover_image IS '封面图URL';
COMMENT ON COLUMN km_blog_article.tags IS 'JSON数组字符串，如 ["Java","AI"]';
COMMENT ON COLUMN km_blog_article.status IS '状态：DRAFT=草稿, PUBLISHED=已发布';
COMMENT ON COLUMN km_blog_article.source IS '来源：FILE=扫描入库, ONLINE=管理端创建';
COMMENT ON COLUMN km_blog_article.dataset_id IS '文章级datasetId（可选，覆盖专题级别）';
COMMENT ON COLUMN km_blog_article.km_document_id IS '同步到知识库后的文档ID';
COMMENT ON COLUMN km_blog_article.source_path IS '源文件相对路径（Scanner用），如 ai/langgraph.md';
COMMENT ON COLUMN km_blog_article.content_hash IS 'MD5哈希，用于增量更新检测';
COMMENT ON COLUMN km_blog_article.published_at IS '发布时间';
COMMENT ON COLUMN km_blog_article.view_count IS '浏览次数';
COMMENT ON COLUMN km_blog_article.del_flag IS '删除标志（0=未删除，1=已删除）';

-- 初始化博客默认知识库数据集ID配置项
-- 值为 '0' 表示未配置，BlogMarkdownProcessor 将跳过知识库同步
INSERT INTO sys_config (config_id,config_name, config_key, config_value, config_type, remark)
VALUES
    (   2102,
        '博客默认知识库数据集ID',
        'blog.default.dataset.id',
        '0',
        'N',
        '博客文章同步到知识库时的默认数据集ID。优先级：文章Frontmatter > 专题_topic.yml > 此配置。值为0表示未配置，跳过知识库同步。'
    )ON CONFLICT (config_id) DO NOTHING;

-- ======================================================================
--  新增博客管理菜单
-- 博客管理作为独立一级目录（parent_id=0），menu_id 从 2600 开始
-- ======================================================================

-- ======================================================================
-- 1. 菜单数据
-- ======================================================================
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    -- 博客管理一级目录（parent_id=0，独立模块）
    ('2600', '博客管理', '0', '6', 'blog', null, '', '1', '0', 'M', '0', '0', '', 'mdi-post', 103, 1, now(), null, null, '博客管理模块'),

    -- 文章管理（二级菜单）
    ('2610', '文章管理', '2600', '1', 'article', 'blog/article/index', '', '1', '0', 'C', '0', '0', 'blog:article:list', 'mdi-file-document-edit', 103, 1, now(), null, null, '博客文章管理'),
    -- 文章管理按钮权限
    ('2611', '文章查询', '2610', '1', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:query', '#', 103, 1, now(), null, null, ''),
    ('2612', '文章新增', '2610', '2', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:add', '#', 103, 1, now(), null, null, ''),
    ('2613', '文章修改', '2610', '3', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:edit', '#', 103, 1, now(), null, null, ''),
    ('2614', '文章删除', '2610', '4', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:remove', '#', 103, 1, now(), null, null, ''),
    ('2615', '状态切换', '2610', '5', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:status', '#', 103, 1, now(), null, null, ''),
    ('2616', '同步知识库', '2610', '6', '', '', '', '1', '0', 'F', '0', '0', 'blog:article:syncKb', '#', 103, 1, now(), null, null, ''),

    -- 分类管理（二级菜单）
    ('2620', '分类管理', '2600', '2', 'category', 'blog/category/index', '', '1', '0', 'C', '0', '0', 'blog:category:list', 'mdi-file-tree', 103, 1, now(), null, null, '博客分类与专题管理'),
    -- 分类管理按钮权限
    ('2621', '分类查询', '2620', '1', '', '', '', '1', '0', 'F', '0', '0', 'blog:category:query', '#', 103, 1, now(), null, null, ''),
    ('2622', '分类新增', '2620', '2', '', '', '', '1', '0', 'F', '0', '0', 'blog:category:add', '#', 103, 1, now(), null, null, ''),
    ('2623', '分类修改', '2620', '3', '', '', '', '1', '0', 'F', '0', '0', 'blog:category:edit', '#', 103, 1, now(), null, null, ''),
    ('2624', '分类删除', '2620', '4', '', '', '', '1', '0', 'F', '0', '0', 'blog:category:remove', '#', 103, 1, now(), null, null, '')
    ON CONFLICT (menu_id) DO NOTHING;

-- ======================================================================
-- 2. 为超级管理员角色（role_id=1）和 AI 管理员角色（role_id=3）授予权限
-- ======================================================================
INSERT INTO sys_role_menu (role_id, menu_id)
VALUES
    -- 超级管理员（role_id=1）
    ('1', '2600'),
    ('1', '2610'),
    ('1', '2611'),
    ('1', '2612'),
    ('1', '2613'),
    ('1', '2614'),
    ('1', '2615'),
    ('1', '2616'),
    ('1', '2620'),
    ('1', '2621'),
    ('1', '2622'),
    ('1', '2623'),
    ('1', '2624'),
    -- AI 管理员（role_id=3）
    ('3', '2600'),
    ('3', '2610'),
    ('3', '2611'),
    ('3', '2612'),
    ('3', '2613'),
    ('3', '2614'),
    ('3', '2615'),
    ('3', '2616'),
    ('3', '2620'),
    ('3', '2621'),
    ('3', '2622'),
    ('3', '2623'),
    ('3', '2624')
    ON CONFLICT (role_id, menu_id) DO NOTHING;

