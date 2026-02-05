package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.vo.KmQuestionVo;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
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
}
