package org.dromara.ai.workflow.constant;

/**
 * 节点连接规则常量
 *
 * @author Mahone
 */
public interface ConnectionRuleConstants {

    /** 规则类型：允许连接 */
    String RULE_TYPE_ALLOW = "0";

    /** 规则类型：禁止连接 */
    String RULE_TYPE_DENY = "1";

    /** 启用状态：启用 */
    String IS_ENABLED_YES = "1";

    /** 启用状态：停用 */
    String IS_ENABLED_NO = "0";

    /** 连接模式：白名单（默认拒绝，仅允许规则表中 rule_type=0 的连接） */
    String MODE_WHITELIST = "whitelist";

    /** 连接模式：黑名单（默认允许，仅禁止规则表中 rule_type=1 的连接） */
    String MODE_BLACKLIST = "blacklist";

    /** sys_config 中连接模式的配置键 */
    String CONFIG_KEY_MODE = "workflow.connection.mode";

    /** Redis 缓存 key：全量启用规则 */
    String CACHE_KEY_ALL_RULES = "workflow:connection:rules:all";

    /** Redis 缓存 key：当前连接模式 */
    String CACHE_KEY_MODE = "workflow:connection:mode";

    /** 缓存 TTL（秒） */
    long CACHE_TTL_SECONDS = 3600L;
}
