package org.dromara.ai.workflow.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.workflow.constant.ConnectionRuleConstants;
import org.dromara.ai.workflow.domain.KmNodeConnectionRule;
import org.dromara.ai.workflow.domain.KmNodeDefinition;
import org.dromara.ai.workflow.domain.bo.KmConnectionInboundSaveBo;
import org.dromara.ai.workflow.domain.bo.KmConnectionMatrixSaveBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleQueryBo;
import org.dromara.ai.workflow.domain.vo.KmConnectionModeVo;
import org.dromara.ai.workflow.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.workflow.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.workflow.mapper.KmNodeConnectionRuleMapper;
import org.dromara.ai.workflow.mapper.KmNodeDefinitionMapper;
import org.dromara.ai.workflow.service.ConnectionRuleCacheService;
import org.dromara.ai.workflow.service.IKmNodeConnectionRuleService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点连接规则 Service 实现
 *
 * @author Mahone
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmNodeConnectionRuleServiceImpl implements IKmNodeConnectionRuleService {

    private final KmNodeConnectionRuleMapper baseMapper;
    private final KmNodeDefinitionMapper nodeDefinitionMapper;
    private final ConnectionRuleCacheService cacheService;
    private final ISysConfigService configService;

    // ===== 原有方法（保持兼容） =====

    @Override
    public List<KmNodeConnectionRuleVo> queryList() {
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(KmNodeConnectionRule::getIsEnabled, ConnectionRuleConstants.IS_ENABLED_YES);
        lqw.orderByDesc(KmNodeConnectionRule::getPriority);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public Map<String, List<String>> getConnectionRulesMap() {
        String mode = cacheService.getConnectionMode();
        List<KmNodeConnectionRuleVo> list = queryList();
        Map<String, List<String>> map = new HashMap<>();
        String targetRuleType = ConnectionRuleConstants.MODE_WHITELIST.equals(mode)
                ? ConnectionRuleConstants.RULE_TYPE_ALLOW
                : ConnectionRuleConstants.RULE_TYPE_DENY;
        for (KmNodeConnectionRuleVo vo : list) {
            if (targetRuleType.equals(vo.getRuleType())) {
                map.computeIfAbsent(vo.getSourceNodeType(), k -> new ArrayList<>()).add(vo.getTargetNodeType());
            }
        }
        return map;
    }

    // ===== 管理界面 CRUD =====

    @Override
    public TableDataInfo<KmNodeConnectionRuleVo> queryPageList(KmNodeConnectionRuleQueryBo query, PageQuery pageQuery) {
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = buildQueryWrapper(query);
        Page<KmNodeConnectionRuleVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    private LambdaQueryWrapper<KmNodeConnectionRule> buildQueryWrapper(KmNodeConnectionRuleQueryBo query) {
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StrUtil.isNotBlank(query.getSourceNodeType()),
                KmNodeConnectionRule::getSourceNodeType, query.getSourceNodeType());
        lqw.eq(StrUtil.isNotBlank(query.getTargetNodeType()),
                KmNodeConnectionRule::getTargetNodeType, query.getTargetNodeType());
        lqw.eq(StrUtil.isNotBlank(query.getRuleType()),
                KmNodeConnectionRule::getRuleType, query.getRuleType());
        lqw.eq(StrUtil.isNotBlank(query.getIsEnabled()),
                KmNodeConnectionRule::getIsEnabled, query.getIsEnabled());
        lqw.orderByDesc(KmNodeConnectionRule::getPriority);
        return lqw;
    }

    @Override
    public KmNodeConnectionRuleVo queryById(Long ruleId) {
        KmNodeConnectionRuleVo vo = baseMapper.selectVoById(ruleId);
        if (vo == null) {
            throw new ServiceException("规则不存在");
        }
        return vo;
    }

    @Override
    public void insertByBo(KmNodeConnectionRuleBo bo) {
        checkDuplicate(bo.getSourceNodeType(), bo.getTargetNodeType(), bo.getRuleType(), null);
        KmNodeConnectionRule entity = MapstructUtils.convert(bo, KmNodeConnectionRule.class);
        if (entity.getPriority() == null) {
            entity.setPriority(10);
        }
        if (StrUtil.isBlank(entity.getIsEnabled())) {
            entity.setIsEnabled(ConnectionRuleConstants.IS_ENABLED_YES);
        }
        baseMapper.insert(entity);
        cacheService.evictRulesCache();
    }

    @Override
    public void updateByBo(KmNodeConnectionRuleBo bo) {
        if (bo.getRuleId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        checkDuplicate(bo.getSourceNodeType(), bo.getTargetNodeType(), bo.getRuleType(), bo.getRuleId());
        KmNodeConnectionRule entity = MapstructUtils.convert(bo, KmNodeConnectionRule.class);
        baseMapper.updateById(entity);
        cacheService.evictRulesCache();
    }

    @Override
    public void deleteWithValidByIds(Long[] ruleIds) {
        baseMapper.deleteByIds(List.of(ruleIds));
        cacheService.evictRulesCache();
    }

    @Override
    public void updateStatus(KmNodeConnectionRuleBo bo) {
        if (bo.getRuleId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        KmNodeConnectionRule entity = new KmNodeConnectionRule();
        entity.setRuleId(bo.getRuleId());
        entity.setIsEnabled(bo.getIsEnabled());
        baseMapper.updateById(entity);
        cacheService.evictRulesCache();
    }

    // ===== 矩阵视图 =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void matrixSave(KmConnectionMatrixSaveBo bo) {
        String sourceType = bo.getSourceNodeType();
        String ruleType = bo.getRuleType();

        // 只管理出边：全量替换 sourceNodeType = sourceType 且 ruleType 匹配的规则
        LambdaQueryWrapper<KmNodeConnectionRule> delWrapper = Wrappers.lambdaQuery();
        delWrapper.eq(KmNodeConnectionRule::getSourceNodeType, sourceType);
        delWrapper.eq(KmNodeConnectionRule::getRuleType, ruleType);
        baseMapper.delete(delWrapper);

        // 重建出边规则：sourceType -> outboundTarget
        if (bo.getOutboundTargets() != null) {
            for (String target : bo.getOutboundTargets()) {
                KmNodeConnectionRule outRule = new KmNodeConnectionRule();
                outRule.setSourceNodeType(sourceType);
                outRule.setTargetNodeType(target);
                outRule.setRuleType(ruleType);
                outRule.setPriority(10);
                outRule.setIsEnabled(ConnectionRuleConstants.IS_ENABLED_YES);
                baseMapper.insert(outRule);
            }
        }

        cacheService.evictRulesCache();
        log.info("矩阵视图保存完成：sourceType={}, outbound={}",
                sourceType,
                bo.getOutboundTargets() == null ? 0 : bo.getOutboundTargets().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inboundSave(KmConnectionInboundSaveBo bo) {
        String targetType = bo.getTargetNodeType();
        String ruleType = bo.getRuleType();
        List<String> inboundSources = bo.getInboundSources() != null ? bo.getInboundSources() : List.of();

        // 删除所有 targetNodeType = targetType 且 ruleType 匹配的规则（全量替换入边）
        LambdaQueryWrapper<KmNodeConnectionRule> delWrapper = Wrappers.lambdaQuery();
        delWrapper.eq(KmNodeConnectionRule::getTargetNodeType, targetType);
        delWrapper.eq(KmNodeConnectionRule::getRuleType, ruleType);
        baseMapper.delete(delWrapper);

        // 重建：inboundSource -> targetType
        for (String source : inboundSources) {
            KmNodeConnectionRule rule = new KmNodeConnectionRule();
            rule.setSourceNodeType(source);
            rule.setTargetNodeType(targetType);
            rule.setRuleType(ruleType);
            rule.setPriority(10);
            rule.setIsEnabled(ConnectionRuleConstants.IS_ENABLED_YES);
            baseMapper.insert(rule);
        }

        cacheService.evictRulesCache();
        log.info("入边保存完成：targetType={}, inbound={}", targetType, inboundSources.size());
    }

    // ===== 模式管理 =====

    @Override
    public KmConnectionModeVo getConnectionMode() {
        String mode = cacheService.getConnectionMode();
        KmConnectionModeVo vo = new KmConnectionModeVo();
        vo.setMode(mode);
        if (ConnectionRuleConstants.MODE_WHITELIST.equals(mode)) {
            vo.setModeLabel("白名单");
            vo.setDescription("默认拒绝所有连接，仅允许规则表中 rule_type=0 的节点对建立连接");
        } else {
            vo.setModeLabel("黑名单");
            vo.setDescription("默认允许所有连接，仅禁止规则表中 rule_type=1 的节点对建立连接");
        }
        return vo;
    }

    @Override
    public void switchConnectionMode(String mode) {
        if (!ConnectionRuleConstants.MODE_WHITELIST.equals(mode)
                && !ConnectionRuleConstants.MODE_BLACKLIST.equals(mode)) {
            throw new ServiceException("无效的连接模式，仅支持 whitelist 或 blacklist");
        }
        SysConfigBo configBo = new SysConfigBo();
        configBo.setConfigKey(ConnectionRuleConstants.CONFIG_KEY_MODE);
        configBo.setConfigValue(mode);
        configService.updateConfig(configBo);
        // 直接写入 Redis，不依赖 sys_config 缓存的失效时机
        cacheService.setConnectionMode(mode);
        log.info("连接模式已切换为：{}", mode);
    }

    // ===== 节点类型下拉 =====

    @Override
    public List<KmNodeDefinitionVo> getEnabledNodeTypes() {
        LambdaQueryWrapper<KmNodeDefinition> lqw = Wrappers.lambdaQuery();
        lqw.eq(KmNodeDefinition::getIsEnabled, ConnectionRuleConstants.IS_ENABLED_YES);
        lqw.orderByAsc(KmNodeDefinition::getCategory)
                .orderByAsc(KmNodeDefinition::getNodeType);
        return nodeDefinitionMapper.selectVoList(lqw);
    }

    // ===== 私有方法 =====

    /**
     * 校验节点对唯一性
     *
     * @param sourceNodeType 源节点类型
     * @param targetNodeType 目标节点类型
     * @param excludeRuleId  编辑时排除自身
     */
    private void checkDuplicate(String sourceNodeType, String targetNodeType, String ruleType, Long excludeRuleId) {
        LambdaQueryWrapper<KmNodeConnectionRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(KmNodeConnectionRule::getSourceNodeType, sourceNodeType);
        lqw.eq(KmNodeConnectionRule::getTargetNodeType, targetNodeType);
        lqw.eq(StrUtil.isNotBlank(ruleType), KmNodeConnectionRule::getRuleType, ruleType);
        if (excludeRuleId != null) {
            lqw.ne(KmNodeConnectionRule::getRuleId, excludeRuleId);
        }
        if (baseMapper.exists(lqw)) {
            throw new ServiceException("该节点对的连接规则已存在");
        }
    }}
