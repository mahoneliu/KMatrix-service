package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmQuestionBo;
import org.dromara.ai.domain.vo.KmQuestionVo;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库问题管理
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/question")
public class KmQuestionController extends BaseController {

    private final IKmQuestionService questionService;

    /**
     * 分页查询问题列表
     */
    @GetMapping("/list")
    public TableDataInfo<KmQuestionVo> list(KmQuestionBo bo, PageQuery pageQuery) {
        return questionService.pageList(bo, pageQuery);
    }

    /**
     * 查询切片关联的问题列表
     */
    @GetMapping("/listByChunk/{chunkId}")
    public R<List<KmQuestionVo>> listByChunk(@NotNull(message = "切片ID不能为空") @PathVariable Long chunkId) {
        return R.ok(questionService.listByChunkId(chunkId));
    }

    /**
     * 查询文档下的所有问题
     */
    @GetMapping("/listByDocument/{documentId}")
    public R<List<KmQuestionVo>> listByDocument(@NotNull(message = "文档ID不能为空") @PathVariable Long documentId) {
        return R.ok(questionService.listByDocumentId(documentId));
    }

    /**
     * 手动添加问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Object> body) {
        Object chunkIdObj = body.get("chunkId");
        Object contentObj = body.get("content");
        if (chunkIdObj == null || contentObj == null) {
            return R.fail("参数不完整");
        }
        Long chunkId = Long.valueOf(chunkIdObj.toString());
        String content = contentObj.toString();
        return toAjax(questionService.addQuestion(chunkId, content));
    }

    /**
     * 关联现有问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping("/link")
    public R<Void> link(@RequestBody Map<String, Object> body) {
        Object chunkIdObj = body.get("chunkId");
        Object questionIdObj = body.get("questionId");

        if (chunkIdObj == null || questionIdObj == null) {
            return R.fail("参数不完整");
        }

        Long chunkId = Long.valueOf(chunkIdObj.toString());
        Long questionId = Long.valueOf(questionIdObj.toString());

        return toAjax(questionService.linkQuestion(chunkId, questionId));
    }

    /**
     * 取消关联问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.DELETE)
    @PostMapping("/unlink")
    public R<Void> unlink(@RequestBody Map<String, Object> body) {
        Object chunkIdObj = body.get("chunkId");
        Object questionIdObj = body.get("questionId");

        if (chunkIdObj == null || questionIdObj == null) {
            return R.fail("参数不完整");
        }

        Long chunkId = Long.valueOf(chunkIdObj.toString());
        Long questionId = Long.valueOf(questionIdObj.toString());

        return toAjax(questionService.unlinkQuestion(chunkId, questionId));
    }

    /**
     * 删除问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(questionService.deleteById(id));
    }

    /**
     * AI自动生成问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<List<KmQuestionVo>> generate(@RequestBody Map<String, Object> body) {
        Object chunkIdObj = body.get("chunkId");
        if (chunkIdObj == null) {
            return R.fail("切片ID不能为空");
        }
        Long chunkId = Long.valueOf(chunkIdObj.toString());

        Object modelIdObj = body.get("modelId");
        Long modelId = modelIdObj != null ? Long.valueOf(modelIdObj.toString()) : null;

        // 获取提示词和参数
        String prompt = body.get("prompt") != null ? body.get("prompt").toString() : null;
        Double temperature = body.get("temperature") != null ? Double.valueOf(body.get("temperature").toString())
                : null;
        Integer maxTokens = body.get("maxTokens") != null ? Integer.valueOf(body.get("maxTokens").toString()) : null;

        return R.ok(questionService.generateQuestions(chunkId, modelId, prompt, temperature, maxTokens));
    }

    /**
     * 批量为切片生成问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping("/batchGenerate")
    public R<Void> batchGenerate(@RequestBody Map<String, Object> body) {
        Object chunkIdsObj = body.get("chunkIds");
        if (chunkIdsObj == null) {
            return R.fail("切片ID列表不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Number> chunkIdNumbers = (List<Number>) chunkIdsObj;
        List<Long> chunkIds = chunkIdNumbers.stream().map(Number::longValue).toList();

        Object modelIdObj = body.get("modelId");
        Long modelId = modelIdObj != null ? Long.valueOf(modelIdObj.toString()) : null;

        // 获取提示词和参数
        String prompt = body.get("prompt") != null ? body.get("prompt").toString() : null;
        Double temperature = body.get("temperature") != null ? Double.valueOf(body.get("temperature").toString())
                : null;
        Integer maxTokens = body.get("maxTokens") != null ? Integer.valueOf(body.get("maxTokens").toString()) : null;

        return toAjax(questionService.batchGenerateQuestions(chunkIds, modelId, prompt, temperature, maxTokens));
    }

    /**
     * 查询知识库下的所有问题
     */
    @GetMapping("/listByKb/{kbId}")
    public R<List<KmQuestionVo>> listByKb(@NotNull(message = "知识库ID不能为空") @PathVariable Long kbId) {
        return R.ok(questionService.listByKbId(kbId));
    }

    /**
     * 更新问题内容
     */
    @Log(title = "知识库问题", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public R<Void> update(@NotNull(message = "问题ID不能为空") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Object contentObj = body.get("content");
        if (contentObj == null) {
            return R.fail("问题内容不能为空");
        }
        String content = contentObj.toString();
        return toAjax(questionService.updateQuestion(id, content));
    }

    /**
     * 批量删除问题
     */
    @Log(title = "知识库问题", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public R<Void> batchRemove(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return R.fail("问题ID列表不能为空");
        }
        return toAjax(questionService.batchDelete(ids));
    }

    /**
     * 批量添加问题到知识库
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping("/batchAdd")
    public R<Void> batchAdd(@RequestBody Map<String, Object> body) {
        Object kbIdObj = body.get("kbId");
        Object contentsObj = body.get("contents");

        if (kbIdObj == null || contentsObj == null) {
            return R.fail("参数不完整");
        }

        Long kbId = Long.valueOf(kbIdObj.toString());
        @SuppressWarnings("unchecked")
        List<String> contents = (List<String>) contentsObj;

        return toAjax(questionService.batchAddToKb(kbId, contents));
    }

    /**
     * 获取问题关联的分段列表
     */
    @GetMapping("/{id}/chunks")
    public R<List<Map<String, Object>>> getLinkedChunks(@NotNull(message = "问题ID不能为空") @PathVariable Long id) {
        return R.ok(questionService.getLinkedChunks(id));
    }

    /**
     * 批量关联问题到分段
     */
    @Log(title = "知识库问题", businessType = BusinessType.INSERT)
    @PostMapping("/batchLink")
    public R<Void> batchLink(@RequestBody Map<String, Object> body) {
        Object questionIdObj = body.get("questionId");
        Object chunkIdsObj = body.get("chunkIds");

        if (questionIdObj == null || chunkIdsObj == null) {
            return R.fail("参数不完整");
        }

        Long questionId = Long.valueOf(questionIdObj.toString());
        @SuppressWarnings("unchecked")
        List<Number> chunkIdNumbers = (List<Number>) chunkIdsObj;
        List<Long> chunkIds = chunkIdNumbers.stream().map(Number::longValue).toList();

        return toAjax(questionService.batchLinkToChunks(questionId, chunkIds));
    }
}
