package org.dromara.ai.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.ai.service.IKmNodeDefinitionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // ========== 节点定义管理 CRUD API ==========

    /**
     * 分页查询节点定义列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:list")
    @GetMapping("/definition/list")
    public org.dromara.common.mybatis.core.page.TableDataInfo<KmNodeDefinitionVo> list(
            org.dromara.ai.domain.query.KmNodeDefinitionQuery query,
            org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        return workflowNodeService.queryPageList(query, pageQuery);
    }

    /**
     * 获取节点定义详情
     *
     * @param nodeDefId 节点定义ID
     * @return 节点定义详情
     */
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:query")
    @GetMapping("/definition/{nodeDefId}")
    public R<KmNodeDefinitionVo> getInfo(
            @org.springframework.web.bind.annotation.PathVariable Long nodeDefId) {
        KmNodeDefinitionVo vo = workflowNodeService.getNodeDefinitionById(nodeDefId);
        return R.ok(vo);
    }

    /**
     * 新增节点定义
     *
     * @param bo 节点定义业务对象
     * @return 新节点定义ID
     */
    @org.dromara.common.log.annotation.Log(title = "节点定义管理", businessType = org.dromara.common.log.enums.BusinessType.INSERT)
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:add")
    @org.springframework.web.bind.annotation.PostMapping("/definition")
    public R<Long> add(
            @Validated @org.springframework.web.bind.annotation.RequestBody org.dromara.ai.domain.bo.KmNodeDefinitionBo bo) {
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
    @org.dromara.common.log.annotation.Log(title = "节点定义管理", businessType = org.dromara.common.log.enums.BusinessType.INSERT)
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:add")
    @org.springframework.web.bind.annotation.PostMapping("/definition/copy/{nodeDefId}")
    public R<Long> copy(
            @org.springframework.web.bind.annotation.PathVariable Long nodeDefId,
            @org.springframework.web.bind.annotation.RequestParam String newNodeType) {
        Long newNodeDefId = workflowNodeService.copyNodeDefinition(nodeDefId, newNodeType);
        return R.ok(newNodeDefId);
    }

    /**
     * 更新节点定义
     *
     * @param bo 节点定义业务对象
     * @return 操作结果
     */
    @org.dromara.common.log.annotation.Log(title = "节点定义管理", businessType = org.dromara.common.log.enums.BusinessType.UPDATE)
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:edit")
    @org.springframework.web.bind.annotation.PutMapping("/definition")
    public R<Void> edit(
            @Validated @org.springframework.web.bind.annotation.RequestBody org.dromara.ai.domain.bo.KmNodeDefinitionBo bo) {
        workflowNodeService.updateNodeDefinition(bo);
        return R.ok();
    }

    /**
     * 删除节点定义
     *
     * @param nodeDefIds 节点定义ID数组
     * @return 操作结果
     */
    @org.dromara.common.log.annotation.Log(title = "节点定义管理", businessType = org.dromara.common.log.enums.BusinessType.DELETE)
    @cn.dev33.satoken.annotation.SaCheckPermission("ai:workflow:node:remove")
    @org.springframework.web.bind.annotation.DeleteMapping("/definition/{nodeDefIds}")
    public R<Void> remove(
            @org.springframework.web.bind.annotation.PathVariable Long[] nodeDefIds) {
        workflowNodeService.deleteNodeDefinitions(nodeDefIds);
        return R.ok();
    }
}
