package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmWorkflowBo;
import org.dromara.ai.domain.vo.KmWorkflowVo;
import org.dromara.ai.service.IKmWorkflowService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 工作流定义Controller
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/workflow")
public class KmWorkflowController extends BaseController {

    private final IKmWorkflowService workflowService;

    /**
     * 查询工作流定义列表
     */
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/list")
    public TableDataInfo<KmWorkflowVo> list(KmWorkflowBo bo, PageQuery pageQuery) {
        return workflowService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取工作流定义详细信息
     */
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/{flowId}")
    public R<KmWorkflowVo> getInfo(@PathVariable Long flowId) {
        return R.ok(workflowService.queryById(flowId));
    }

    /**
     * 新增工作流定义
     */
    @SaCheckPermission("ai:workflow:add")
    @Log(title = "工作流定义", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody KmWorkflowBo bo) {
        return toAjax(workflowService.insertByBo(bo));
    }

    /**
     * 修改工作流定义
     */
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "工作流定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody KmWorkflowBo bo) {
        return toAjax(workflowService.updateByBo(bo));
    }

    /**
     * 删除工作流定义
     */
    @SaCheckPermission("ai:workflow:remove")
    @Log(title = "工作流定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{flowIds}")
    public R<Void> remove(@PathVariable Long[] flowIds) {
        return toAjax(workflowService.deleteWithValidByIds(Arrays.asList(flowIds), true));
    }
}
