package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmKnowledgeBaseBo;
import org.dromara.ai.domain.vo.KmKnowledgeBaseVo;
import org.dromara.ai.domain.vo.KmStatisticsVo;
import org.dromara.ai.service.IKmKnowledgeBaseService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/kb")
public class KmKnowledgeBaseController extends BaseController {

    private final IKmKnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库列表
     */
    @SaCheckPermission("ai:knowledge:list")
    @GetMapping("/list")
    public TableDataInfo<KmKnowledgeBaseVo> list(KmKnowledgeBaseBo bo, PageQuery pageQuery) {
        return knowledgeBaseService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询所有知识库 (下拉选择用)
     */
    @SaCheckPermission("ai:knowledge:list")
    @GetMapping("/listAll")
    public R<List<KmKnowledgeBaseVo>> listAll(KmKnowledgeBaseBo bo) {
        return R.ok(knowledgeBaseService.queryList(bo));
    }

    /**
     * 获取知识库详细信息
     */
    @SaCheckPermission("ai:knowledge:query")
    @GetMapping("/{id}")
    public R<KmKnowledgeBaseVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(knowledgeBaseService.queryById(id));
    }

    /**
     * 获取知识库统计信息 (Global)
     */
    @SaCheckPermission("ai:knowledge:query")
    @GetMapping("/statistics")
    public R<KmStatisticsVo> getStatistics() {
        return R.ok(knowledgeBaseService.getStatistics());
    }

    /**
     * 获取知识库统计信息 (Specific KB)
     */
    @SaCheckPermission("ai:knowledge:query")
    @GetMapping("/{id}/statistics")
    public R<KmStatisticsVo> getStatistics(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(knowledgeBaseService.getStatistics(id));
    }

    /**
     * 新增知识库
     */
    @SaCheckPermission("ai:knowledge:add")
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Long> add(@Validated(AddGroup.class) @RequestBody KmKnowledgeBaseBo bo) {
        return R.ok(knowledgeBaseService.insertByBo(bo));
    }

    /**
     * 修改知识库
     */
    @SaCheckPermission("ai:knowledge:edit")
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmKnowledgeBaseBo bo) {
        return toAjax(knowledgeBaseService.updateByBo(bo));
    }

    /**
     * 删除知识库
     */
    @SaCheckPermission("ai:knowledge:remove")
    @Log(title = "知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(knowledgeBaseService.deleteWithValidByIds(List.of(ids), true));
    }
}
