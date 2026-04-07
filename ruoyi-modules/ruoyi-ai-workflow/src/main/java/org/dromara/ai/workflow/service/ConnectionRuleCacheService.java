package org.dromara.ai.workflow.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.ConnectionRuleConstants;
import org.dromara.ai.workflow.domain.KmNodeConnectionRule;
import org.dromara.ai.workflow.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.workflow.mapper.KmNodeConnectionRuleMapper;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 节点连接规则缓存服务
 * <p>
 * 使用单一 Redis key 缓存全量启用规则，TTL=1小时。
 * 任何规则变更后调用 evictRulesCache() 使缓存失效。
 *
 * @author Mahone
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionRuleCacheService {

    private final KmNodeConnectionRuleMapper ruleMapper;
    private final ISysConfigService configService;

    /**
     * 获取所有启用的连接规则（优先读缓存）
     */
    public List<KmNodeConnectionRuleVo> getAllEnabledRules() {
        List<KmNodeConnectionRuleVo> cached = RedisUtils.getCacheObject(ConnectionRuleConstants.CACHE_KEY_ALL_RULES);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，从 DB 加载
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KmNodeConnectionRule::getIsEnabled, ConnectionRuleConstants.IS_ENABLED_YES);
        List<KmNodeConnectionRuleVo> rules = ruleMapper.selectVoList(lqw);
        RedisUtils.setCacheObject(ConnectionRuleConstants.CACHE_KEY_ALL_RULES, rules,
                Duration.ofSeconds(ConnectionRuleConstants.CACHE_TTL_SECONDS));
        log.debug("连接规则缓存已加载，共 {} 条", rules.size());
        return rules;
    }

    /**
     * 获取当前连接模式（优先读缓存）
     * 若 sys_config 中不存在或值无效，默认返回 whitelist
     */
    public String getConnectionMode() {
        String cached = RedisUtils.getCacheObject(ConnectionRuleConstants.CACHE_KEY_MODE);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        // 缓存未命中，从 sys_config 加载
        String mode = configService.selectConfigByKey(ConnectionRuleConstants.CONFIG_KEY_MODE);
        if (!ConnectionRuleConstants.MODE_WHITELIST.equals(mode)
                && !ConnectionRuleConstants.MODE_BLACKLIST.equals(mode)) {
            mode = ConnectionRuleConstants.MODE_WHITELIST;
        }
        RedisUtils.setCacheObject(ConnectionRuleConstants.CACHE_KEY_MODE, mode,
                Duration.ofSeconds(ConnectionRuleConstants.CACHE_TTL_SECONDS));
        return mode;
    }

    /**
     * 清除规则缓存（规则变更后调用）
     */
    public void evictRulesCache() {
        RedisUtils.deleteObject(ConnectionRuleConstants.CACHE_KEY_ALL_RULES);
        log.debug("连接规则缓存已清除");
    }

    /**
     * 清除模式缓存（模式切换后调用）
     */
    public void evictModeCache() {
        RedisUtils.deleteObject(ConnectionRuleConstants.CACHE_KEY_MODE);
        log.debug("连接模式缓存已清除");
    }

    /**
     * 直接写入模式缓存（切换模式后立即生效，不依赖 sys_config 缓存失效时机）
     */
    public void setConnectionMode(String mode) {
        RedisUtils.setCacheObject(ConnectionRuleConstants.CACHE_KEY_MODE, mode,
                Duration.ofSeconds(ConnectionRuleConstants.CACHE_TTL_SECONDS));
        log.debug("连接模式缓存已更新为：{}", mode);
    }
}
