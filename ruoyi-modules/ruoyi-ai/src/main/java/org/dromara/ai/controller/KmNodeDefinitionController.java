package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmNodeDefinitionBo;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.service.IKmNodeConnectionRuleService;
import org.dromara.ai.service.IKmNodeDefinitionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.core.annotation.DemoBlock;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流节点定义控制器
 *
 * @author Mahone
 * @date 2026-01-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/workflow/node")
public class KmNodeDefinitionController extends BaseController {

    private final IKmNodeDefinitionService workflowNodeService;
    private final IKmNodeConnectionRuleService connectionRuleService;

    /**
     * 获取所有节点类型定义
     *
     * @return 节点类型定义列表
     */
    @GetMapping("/definitions")
    public R<List<KmNodeDefinitionVo>> getNodeDefinitions() {
        List<KmNodeDefinitionVo> definitions = workflowNodeService.getNodeDefinitions();
        return R.ok(definitions);
    }

    /**
     * 获取所有节点连接规则
     *
     * @return 规则映射表 (源节点类型 -> 允许的目标节点类型列表)
     */
    @GetMapping("/connection/rules")
    public R<Map<String, List<String>>> getConnectionRules() {
        return R.ok(connectionRuleService.getConnectionRulesMap());
    }

    // ========== 节点定义管理 CRUD API ==========

    /**
     * 分页查询节点定义列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission("ai:workflow:node:list")
    @GetMapping("/definition/list")
    public TableDataInfo<KmNodeDefinitionVo> list(
            KmNodeDefinitionBo bo,
            PageQuery pageQuery) {
        return workflowNodeService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取节点定义详情
     *
     * @param nodeDefId 节点定义ID
     * @return 节点定义详情
     */
    @SaCheckPermission("ai:workflow:node:query")
    @GetMapping("/definition/{nodeDefId}")
    public R<KmNodeDefinitionVo> getInfo(
            @PathVariable Long nodeDefId) {
        KmNodeDefinitionVo vo = workflowNodeService.getNodeDefinitionById(nodeDefId);
        return R.ok(vo);
    }

    /**
     * 新增节点定义
     *
     * @param bo 节点定义业务对象
     * @return 新节点定义ID
     */
    @Log(title = "节点定义管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("ai:workflow:node:add")
    @PostMapping("/definition")
    public R<Long> add(
            @Validated @RequestBody KmNodeDefinitionBo bo) {
        Long nodeDefId = workflowNodeService.addNodeDefinition(bo);
        return R.ok(nodeDefId);
    }

    /**
     * 复制节点定义
     *
     * @param nodeDefId   源节点定义ID
     * @param newNodeType 新节点类型
     * @return 新节点定义ID
     */
    @Log(title = "节点定义管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("ai:workflow:node:add")
    @PostMapping("/definition/copy/{nodeDefId}")
    public R<Long> copy(
            @PathVariable Long nodeDefId,
            @RequestParam String newNodeType) {
        Long newNodeDefId = workflowNodeService.copyNodeDefinition(nodeDefId, newNodeType);
        return R.ok(newNodeDefId);
    }

    /**
     * 更新节点定义
     *
     * @param bo 节点定义业务对象
     * @return 操作结果
     */
    @Log(title = "节点定义管理", businessType = BusinessType.UPDATE)
    @DemoBlock
    @SaCheckPermission("ai:workflow:node:edit")
    @PutMapping("/definition")
    public R<Void> edit(
            @Validated @RequestBody KmNodeDefinitionBo bo) {
        workflowNodeService.updateNodeDefinition(bo);
        return R.ok();
    }

    /**
     * 删除节点定义
     *
     * @param nodeDefIds 节点定义ID数组
     * @return 操作结果
     */
    @Log(title = "节点定义管理", businessType = BusinessType.DELETE)
    @DemoBlock
    @SaCheckPermission("ai:workflow:node:remove")
    @DeleteMapping("/definition/{nodeDefIds}")
    public R<Void> remove(
            @PathVariable Long[] nodeDefIds) {
        workflowNodeService.deleteNodeDefinitions(nodeDefIds);
        return R.ok();
    }
}
