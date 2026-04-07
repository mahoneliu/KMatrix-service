package org.dromara.ai.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.workflow.domain.bo.KmConnectionInboundSaveBo;
import org.dromara.ai.workflow.domain.bo.KmConnectionMatrixSaveBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleBo;
import org.dromara.ai.workflow.domain.bo.KmNodeConnectionRuleQueryBo;
import org.dromara.ai.workflow.domain.vo.KmConnectionModeVo;
import org.dromara.ai.workflow.domain.vo.KmNodeConnectionRuleVo;
import org.dromara.ai.workflow.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.workflow.service.IKmNodeConnectionRuleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节点连接规则管理控制器
 *
 * @author Mahone
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/connection-rule")
public class KmNodeConnectionRuleController extends BaseController {

    private final IKmNodeConnectionRuleService connectionRuleService;

    // ===== 列表视图 CRUD =====

    /**
     * 分页查询规则列表
     */
    @SaCheckPermission("ai:connectionRule:list")
    @GetMapping("/list")
    public TableDataInfo<KmNodeConnectionRuleVo> list(KmNodeConnectionRuleQueryBo query, PageQuery pageQuery) {
        return connectionRuleService.queryPageList(query, pageQuery);
    }

    /**
     * 查询单条规则
     */
    @SaCheckPermission("ai:connectionRule:list")
    @GetMapping("/{ruleId}")
    public R<KmNodeConnectionRuleVo> getInfo(@PathVariable Long ruleId) {
        return R.ok(connectionRuleService.queryById(ruleId));
    }

    /**
     * 新增规则
     */
    @Log(title = "节点连接规则", businessType = BusinessType.INSERT)
    @SaCheckPermission("ai:connectionRule:add")
    @PostMapping
    public R<Void> add(@Validated @RequestBody KmNodeConnectionRuleBo bo) {
        connectionRuleService.insertByBo(bo);
        return R.ok();
    }

    /**
     * 编辑规则
     */
    @Log(title = "节点连接规则", businessType = BusinessType.UPDATE)
    @SaCheckPermission("ai:connectionRule:edit")
    @PutMapping
    public R<Void> edit(@Validated @RequestBody KmNodeConnectionRuleBo bo) {
        connectionRuleService.updateByBo(bo);
        return R.ok();
    }

    /**
     * 删除规则（支持批量）
     */
    @Log(title = "节点连接规则", businessType = BusinessType.DELETE)
    @SaCheckPermission("ai:connectionRule:remove")
    @DeleteMapping("/{ruleIds}")
    public R<Void> remove(@PathVariable Long[] ruleIds) {
        connectionRuleService.deleteWithValidByIds(ruleIds);
        return R.ok();
    }

    /**
     * 启用/停用规则
     */
    @Log(title = "节点连接规则", businessType = BusinessType.UPDATE)
    @SaCheckPermission("ai:connectionRule:edit")
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody KmNodeConnectionRuleBo bo) {
        connectionRuleService.updateStatus(bo);
        return R.ok();
    }

    // ===== 矩阵视图 =====

    /**
     * 矩阵视图批量保存（全量替换）
     */
    @Log(title = "节点连接规则-矩阵保存", businessType = BusinessType.UPDATE)
    @SaCheckPermission("ai:connectionRule:edit")
    @PostMapping("/matrix/save")
    public R<Void> matrixSave(@Validated @RequestBody KmConnectionMatrixSaveBo bo) {
        connectionRuleService.matrixSave(bo);
        return R.ok();
    }

    /**
     * 矩阵视图入边保存
     */
    @Log(title = "节点连接规则-入边保存", businessType = BusinessType.UPDATE)
    @SaCheckPermission("ai:connectionRule:edit")
    @PostMapping("/matrix/inbound-save")
    public R<Void> inboundSave(@Validated @RequestBody KmConnectionInboundSaveBo bo) {
        connectionRuleService.inboundSave(bo);
        return R.ok();
    }

    // ===== 模式管理 =====

    /**
     * 查询当前连接模式
     */
    @SaCheckPermission("ai:connectionRule:list")
    @GetMapping("/mode")
    public R<KmConnectionModeVo> getConnectionMode() {
        return R.ok(connectionRuleService.getConnectionMode());
    }

    /**
     * 切换连接模式
     *
     * @param mode whitelist 或 blacklist
     */
    @Log(title = "节点连接规则-模式切换", businessType = BusinessType.UPDATE)
    @SaCheckPermission("ai:connectionRule:config")
    @PutMapping("/mode")
    public R<Void> switchConnectionMode(@RequestParam String mode) {
        connectionRuleService.switchConnectionMode(mode);
        return R.ok();
    }

    // ===== 节点类型下拉 =====

    /**
     * 获取所有启用的节点类型（供前端下拉）
     */
    @SaCheckPermission("ai:connectionRule:list")
    @GetMapping("/node-types")
    public R<List<KmNodeDefinitionVo>> getEnabledNodeTypes() {
        return R.ok(connectionRuleService.getEnabledNodeTypes());
    }
}
