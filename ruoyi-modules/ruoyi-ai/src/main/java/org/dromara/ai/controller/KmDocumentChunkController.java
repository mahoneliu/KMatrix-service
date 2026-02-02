package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.ai.service.IKmDocumentChunkService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
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
    @GetMapping("/listByDocument/{documentId}")
    public R<List<KmDocumentChunkVo>> listByDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long documentId) {
        return R.ok(chunkService.listByDocumentId(documentId));
    }

    /**
     * 获取切片详细信息
     */
    @GetMapping("/{id}")
    public R<KmDocumentChunkVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(chunkService.queryById(id));
    }

    /**
     * 修改切片内容
     */
    @Log(title = "知识库切片", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        Object contentObj = body.get("content");
        if (idObj == null || contentObj == null) {
            return R.fail("参数不完整");
        }
        Long id = Long.valueOf(idObj.toString());
        String content = contentObj.toString();
        return toAjax(chunkService.updateChunk(id, content));
    }

    /**
     * 删除切片
     */
    @Log(title = "知识库切片", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(chunkService.deleteById(id));
    }
}
