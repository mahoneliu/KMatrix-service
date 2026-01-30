package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmWorkflowTemplateBo;
import org.dromara.ai.domain.vo.KmWorkflowTemplateVo;
import org.dromara.ai.service.IKmWorkflowTemplateService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 工作流模板Controller
 *
 * @author Mahone
 * @date 2026-01-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/workflow-template")
public class KmWorkflowTemplateController extends BaseController {

    private final IKmWorkflowTemplateService templateService;

    /**
     * 查询工作流模板列表
     */
    @SaCheckPermission("ai:workflow-template:list")
    @GetMapping("/list")
    public TableDataInfo<KmWorkflowTemplateVo> list(KmWorkflowTemplateBo bo, PageQuery pageQuery) {
        return templateService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取模板分类列表
     */
    @GetMapping("/categories")
    public R<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> categories = List.of(
                Map.of("value", "knowledge_qa", "label", "知识问答"),
                Map.of("value", "customer_service", "label", "客服"),
                Map.of("value", "marketing", "label", "营销"),
                Map.of("value", "custom", "label", "自定义"));
        return R.ok(categories);
    }

    /**
     * 获取工作流模板详情
     */
    @SaCheckPermission("ai:workflow-template:query")
    @GetMapping("/{templateId}")
    public R<KmWorkflowTemplateVo> getInfo(@PathVariable Long templateId) {
        return R.ok(templateService.queryById(templateId));
    }

    /**
     * 新增工作流模板 (仅用户模板)
     */
    @SaCheckPermission("ai:workflow-template:add")
    @Log(title = "工作流模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody KmWorkflowTemplateBo bo) {
        // 强制设置为用户模板
        bo.setScopeType("1");
        return toAjax(templateService.insertByBo(bo));
    }

    /**
     * 修改工作流模板 (仅用户模板)
     */
    @SaCheckPermission("ai:workflow-template:edit")
    @Log(title = "工作流模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody KmWorkflowTemplateBo bo) {
        // 检查是否为系统模板
        KmWorkflowTemplateVo existing = templateService.queryById(bo.getTemplateId());
        if (existing != null && "0".equals(existing.getScopeType())) {
            throw new ServiceException("系统模板不允许修改");
        }
        return toAjax(templateService.updateByBo(bo));
    }

    /**
     * 删除工作流模板 (仅用户模板)
     */
    @SaCheckPermission("ai:workflow-template:remove")
    @Log(title = "工作流模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public R<Void> remove(@PathVariable Long[] templateIds) {
        // 检查是否包含系统模板
        for (Long templateId : templateIds) {
            KmWorkflowTemplateVo existing = templateService.queryById(templateId);
            if (existing != null && "0".equals(existing.getScopeType())) {
                throw new ServiceException("系统模板不允许删除");
            }
        }
        return toAjax(templateService.deleteWithValidByIds(Arrays.asList(templateIds), true));
    }

    /**
     * 通过模板创建应用
     */
    @SaCheckPermission("ai:app:add")
    @Log(title = "通过模板创建应用", businessType = BusinessType.INSERT)
    @PostMapping("/createApp/{templateId}")
    public R<Long> createAppFromTemplate(@PathVariable Long templateId,
            @RequestBody Map<String, String> body) {
        String appName = body.get("appName");
        if (appName == null || appName.isBlank()) {
            throw new ServiceException("应用名称不能为空");
        }
        Long appId = templateService.createAppFromTemplate(templateId, appName);
        return R.ok(appId);
    }
}
