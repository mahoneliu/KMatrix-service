package org.dromara.ai.workflow.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.ConnectionRuleConstants;
import org.dromara.ai.workflow.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.workflow.service.ConnectionRuleCacheService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流节点连接校验器
 * <p>
 * 支持白名单/黑名单双模式，通过 Redis 缓存保证高性能。
 *
 * @author Mahone
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowConnectionValidator {

    private final ConnectionRuleCacheService cacheService;

    /**
     * 校验两个节点之间是否允许建立连接
     *
     * @param sourceType 源节点类型
     * @param targetType 目标节点类型
     * @return true=允许，false=拒绝
     */
    public boolean isConnectionAllowed(String sourceType, String targetType) {
        String mode = cacheService.getConnectionMode();
        List<KmNodeConnectionRuleVo> rules = cacheService.getAllEnabledRules();

        // 构建快速查找 Map：key = "source:target" -> ruleType
        Map<String, String> ruleMap = rules.stream()
                .collect(Collectors.toMap(
                        r -> r.getSourceNodeType() + ":" + r.getTargetNodeType(),
                        KmNodeConnectionRuleVo::getRuleType,
                        (existing, replacement) -> existing  // 重复时保留第一条
                ));

        String key = sourceType + ":" + targetType;
        String ruleType = ruleMap.get(key);

        if (ConnectionRuleConstants.MODE_WHITELIST.equals(mode)) {
            // 白名单：规则存在且为 ALLOW 才放行
            boolean allowed = ConnectionRuleConstants.RULE_TYPE_ALLOW.equals(ruleType);
            if (!allowed) {
                log.debug("白名单模式拒绝连接：{} -> {}", sourceType, targetType);
            }
            return allowed;
        } else {
            // 黑名单：规则存在且为 DENY 才拒绝，否则放行
            boolean denied = ConnectionRuleConstants.RULE_TYPE_DENY.equals(ruleType);
            if (denied) {
                log.debug("黑名单模式拒绝连接：{} -> {}", sourceType, targetType);
            }
            return !denied;
        }
    }
}
