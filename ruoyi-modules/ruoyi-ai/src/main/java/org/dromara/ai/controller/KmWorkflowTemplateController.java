package org.dromara.ai.controller;

import org.dromara.common.core.utils.MessageUtils;

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
import org.dromara.common.core.annotation.DemoBlock;
import org.dromara.system.domain.bo.SysDictDataBo;
import org.dromara.system.domain.vo.SysDictDataVo;
import org.dromara.system.service.ISysDictDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ISysDictDataService dictDataService;

    /**
     * 查询工作流模板列表
     */
    @SaCheckPermission("ai:workflowTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<KmWorkflowTemplateVo> list(KmWorkflowTemplateBo bo, PageQuery pageQuery) {
        return templateService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取模板分类列表(从字典中动态获取)
     */
    @GetMapping("/categories")
    public R<List<Map<String, String>>> getCategories() {
        // 从字典中查询工作流模板分类
        SysDictDataBo queryBo = new SysDictDataBo();
        queryBo.setDictType("km_workflow_template_category");
        List<SysDictDataVo> dictDataList = dictDataService.selectDictDataList(queryBo);

        // 转换为前端需要的格式
        List<Map<String, String>> categories = dictDataList.stream()
                .map(dict -> Map.of(
                        "value", dict.getDictValue(),
                        "label", dict.getDictLabel()))
                .collect(Collectors.toList());

        return R.ok(categories);
    }

    /**
     * 获取工作流模板详情
     */
    @SaCheckPermission("ai:workflowTemplate:query")
    @GetMapping("/{templateId}")
    public R<KmWorkflowTemplateVo> getInfo(@PathVariable Long templateId) {
        return R.ok(templateService.queryById(templateId));
    }

    /**
     * 新增工作流模板 (仅用户模板)
     */
    @SaCheckPermission("ai:workflowTemplate:add")
    @Log(title = "工作流模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody KmWorkflowTemplateBo bo) {
        // 强制设置为用户模板
        bo.setScopeType("1");
        templateService.insertByBo(bo);
        return R.ok(bo.getTemplateId());
    }

    /**
     * 修改工作流模板 (仅用户模板)
     */
    @SaCheckPermission("ai:workflowTemplate:edit")
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
    @DemoBlock
    @SaCheckPermission("ai:workflowTemplate:remove")
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
            throw new ServiceException(MessageUtils.message("ai.msg.app.name_required"));
        }
        Long appId = templateService.createAppFromTemplate(templateId, appName);
        return R.ok(appId);
    }

    /**
     * 复制模板为自定义模板
     */
    @SaCheckPermission("ai:workflowTemplate:add")
    @Log(title = "复制工作流模板", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{templateId}")
    public R<Long> copyTemplate(@PathVariable Long templateId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("newName");
        if (newName == null || newName.isBlank()) {
            throw new ServiceException("新模板名称不能为空");
        }
        Long newTemplateId = templateService.copyTemplate(templateId, newName);
        return R.ok(newTemplateId);
    }
}
