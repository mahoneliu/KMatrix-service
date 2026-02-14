package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmNodeConnectionRule;
import org.dromara.ai.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.mapper.KmNodeConnectionRuleMapper;
import org.dromara.ai.service.IKmNodeConnectionRuleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点连接规则 Service 实现
 *
 * @author Mahone
 * @date 2026-01-20
 */
@RequiredArgsConstructor
@Service
public class KmNodeConnectionRuleServiceImpl implements IKmNodeConnectionRuleService {

    private final KmNodeConnectionRuleMapper baseMapper;

    @Override
    public List<KmNodeConnectionRuleVo> queryList() {
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(KmNodeConnectionRule::getIsEnabled, "1");
        lqw.orderByDesc(KmNodeConnectionRule::getPriority);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public Map<String, List<String>> getConnectionRulesMap() {
        List<KmNodeConnectionRuleVo> list = queryList();
        Map<String, List<String>> map = new HashMap<>();
        for (KmNodeConnectionRuleVo vo : list) {
            // 规则类型 (0允许连接/1禁止连接)
            if ("0".equals(vo.getRuleType())) {
                map.computeIfAbsent(vo.getSourceNodeType(), k -> new ArrayList<>()).add(vo.getTargetNodeType());
            }
        }
        return map;
    }
}
