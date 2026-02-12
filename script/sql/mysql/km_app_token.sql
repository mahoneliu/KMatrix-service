-- ----------------------------
-- Table structure for km_app_token
-- App嵌入Token表，用于第三方应用授权
-- ----------------------------
DROP TABLE IF EXISTS `km_app_token`;
CREATE TABLE `km_app_token` (
    `token_id`        BIGINT(20)    NOT NULL COMMENT 'Token ID',
    `app_id`          BIGINT(20)    NOT NULL COMMENT '关联应用ID',
    `token`           VARCHAR(64)   NOT NULL COMMENT 'Token值',
    `token_name`      VARCHAR(100)  NOT NULL COMMENT 'Token名称',
    `allowed_origins` VARCHAR(500)  DEFAULT '*' COMMENT '允许的来源域名(逗号分隔,*表示全部)',
    `expires_at`      DATETIME      DEFAULT NULL COMMENT '过期时间(null表示永不过期)',
    `status`          CHAR(1)       DEFAULT '1' COMMENT '状态(0停用 1启用)',
    `remark`          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `del_flag`        CHAR(1)       DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    `create_dept`     BIGINT(20)    DEFAULT NULL COMMENT '创建部门',
    `create_by`       BIGINT(20)    DEFAULT NULL COMMENT '创建者',
    `create_time`     DATETIME      DEFAULT NULL COMMENT '创建时间',
    `update_by`       BIGINT(20)    DEFAULT NULL COMMENT '更新者',
    `update_time`     DATETIME      DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`token_id`),
    UNIQUE KEY `idx_token` (`token`),
    KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App嵌入Token表';
