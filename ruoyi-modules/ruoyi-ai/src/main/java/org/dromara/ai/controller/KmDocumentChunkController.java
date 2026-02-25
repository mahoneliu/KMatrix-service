package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmDocumentChunkBo;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.ai.service.IKmDocumentChunkService;
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
import java.util.Map;

/**
 * 文档切片管理
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/chunk")
public class KmDocumentChunkController extends BaseController {

    private final IKmDocumentChunkService chunkService;

    /**
     * 查询文档下的切片列表
     */
    @SaCheckPermission("ai:chunk:list")
    @GetMapping("/listByDocument/{documentId}")
    public R<List<KmDocumentChunkVo>> listByDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long documentId) {
        return R.ok(chunkService.listByDocumentId(documentId));
    }

    /**
     * 获取切片详细信息
     */
    @SaCheckPermission("ai:chunk:query")
    @GetMapping("/{id:\\d+}")
    public R<KmDocumentChunkVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(chunkService.queryById(id));
    }

    /**
     * 修改切片
     */
    @SaCheckPermission("ai:chunk:edit")
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) {
            return R.fail("ID不能为空");
        }
        Long id = Long.valueOf(idObj.toString());

        String title = body.containsKey("title") ? body.get("title").toString() : null;
        String content = body.containsKey("content") ? body.get("content").toString() : null;

        return toAjax(chunkService.updateChunk(id, title, content));
    }

    /**
     * 删除切片
     */
    @SaCheckPermission("ai:chunk:remove")
    @Log(title = "知识库切片", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id:\\d+}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(chunkService.deleteById(id));
    }

    /**
     * 分页查询切片列表
     */
    @SaCheckPermission("ai:chunk:list")
    @GetMapping("/list")
    public TableDataInfo<KmDocumentChunkVo> list(KmDocumentChunkBo bo, PageQuery pageQuery) {
        return chunkService.pageList(bo, pageQuery);
    }

    /**
     * 手工添加切片
     */
    @SaCheckPermission("ai:chunk:add")
    @Log(title = "知识库切片", businessType = BusinessType.INSERT)
    @PostMapping
    public R<KmDocumentChunkVo> add(@Validated @RequestBody KmDocumentChunkBo bo) {
        return R.ok(chunkService.addChunk(bo));
    }

    /**
     * 启用切片
     */
    @SaCheckPermission("ai:chunk:edit")
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping("/enable/{id:\\d+}")
    public R<Void> enable(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(chunkService.enableChunk(id, true));
    }

    /**
     * 禁用切片
     */
    @SaCheckPermission("ai:chunk:edit")
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping("/disable/{id:\\d+}")
    public R<Void> disable(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(chunkService.enableChunk(id, false));
    }

    /**
     * 批量启用切片
     */
    @SaCheckPermission("ai:chunk:edit")
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping("/batchEnable")
    public R<Void> batchEnable(@RequestBody List<Long> ids) {
        return toAjax(chunkService.batchEnable(ids, true));
    }

    /**
     * 批量禁用切片
     */
    @SaCheckPermission("ai:chunk:edit")
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping("/batchDisable")
    public R<Void> batchDisable(@RequestBody List<Long> ids) {
        return toAjax(chunkService.batchEnable(ids, false));
    }

    /**
     * 批量删除切片
     */
    @DemoBlock
    @SaCheckPermission("ai:chunk:remove")
    @Log(title = "知识库切片", businessType = BusinessType.DELETE)
    @DeleteMapping("/batchDelete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        return toAjax(chunkService.batchDelete(ids));
    }
}
