package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmNodeConnectionRuleVo;
import java.util.List;
import java.util.Map;

/**
 * 节点连接规则 Service 接口
 *
 * @author Mahone
 * @date 2026-01-20
 */
public interface IKmNodeConnectionRuleService {

    /**
     * 查询所有启用的连接规则
     * 
     * @return 规则列表
     */
    List<KmNodeConnectionRuleVo> queryList();

    /**
     * 获取节点连接规则映射表
     * Key: 源节点类型, Value: 允许的目标节点类型列表
     * 
     * @return 规则映射表
     */
    Map<String, List<String>> getConnectionRulesMap();
}
