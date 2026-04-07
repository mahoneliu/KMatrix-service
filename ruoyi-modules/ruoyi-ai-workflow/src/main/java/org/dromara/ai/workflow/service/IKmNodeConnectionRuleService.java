package org.dromara.ai.workflow.service;

import org.dromara.ai.workflow.domain.bo.KmConnectionInboundSaveBo;
import org.dromara.ai.workflow.domain.bo.KmConnectionMatrixSaveBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleQueryBo;
import org.dromara.ai.workflow.domain.vo.KmConnectionModeVo;
import org.dromara.ai.workflow.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.workflow.domain.vo.KmNodeDefinitionVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;
import java.util.Map;

/**
 * 节点连接规则 Service 接口
 *
 * @author Mahone
 */
public interface IKmNodeConnectionRuleService {

    // ===== 原有方法（保持兼容） =====

    /**
     * 查询所有启用的连接规则
     */
    List<KmNodeConnectionRuleVo> queryList();

    /**
     * 获取节点连接规则映射表（白名单模式专用）
     * Key: 源节点类型, Value: 允许的目标节点类型列表
     */
    Map<String, List<String>> getConnectionRulesMap();

    // ===== 管理界面 CRUD =====

    /**
     * 分页查询规则列表
     */
    TableDataInfo<KmNodeConnectionRuleVo> queryPageList(KmNodeConnectionRuleQueryBo query, PageQuery pageQuery);

    /**
     * 根据 ID 查询单条规则
     */
    KmNodeConnectionRuleVo queryById(Long ruleId);

    /**
     * 新增规则
     */
    void insertByBo(KmNodeConnectionRuleBo bo);

    /**
     * 编辑规则
     */
    void updateByBo(KmNodeConnectionRuleBo bo);

    /**
     * 批量删除规则
     */
    void deleteWithValidByIds(Long[] ruleIds);

    /**
     * 启用/停用规则
     */
    void updateStatus(KmNodeConnectionRuleBo bo);

    // ===== 矩阵视图 =====

    /**
     * 矩阵视图批量保存（全量替换策略）
     */
    void matrixSave(KmConnectionMatrixSaveBo bo);

    /**
     * 矩阵视图入边保存：维护 inboundSource -> targetNodeType 规则
     * 只影响 targetNodeType 的入边，不干预其他节点的出边规则
     */
    void inboundSave(KmConnectionInboundSaveBo bo);

    // ===== 模式管理 =====

    /**
     * 获取当前连接模式
     */
    KmConnectionModeVo getConnectionMode();

    /**
     * 切换连接模式
     *
     * @param mode whitelist 或 blacklist
     */
    void switchConnectionMode(String mode);

    // ===== 节点类型下拉 =====

    /**
     * 获取所有启用的节点类型（供前端下拉）
     */
    List<KmNodeDefinitionVo> getEnabledNodeTypes();
}
