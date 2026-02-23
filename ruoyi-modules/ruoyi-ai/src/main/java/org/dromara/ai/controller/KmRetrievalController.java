package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.service.IKmRetrievalService;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库检索
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/retrieval")
public class KmRetrievalController extends BaseController {

    private final IKmRetrievalService retrievalService;

    /**
     * 执行知识库检索
     *
     * 支持三种模式:
     * - VECTOR: 向量相似度检索
     * - KEYWORD: 关键词全文检索
     * - HYBRID: 混合检索 (向量 + 关键词 + RRF 融合)
     */
    @SaCheckPermission("ai:retrieval:search")
    @PostMapping("/search")
    public R<List<KmRetrievalResultVo>> search(@RequestBody KmRetrievalBo bo) {
        return R.ok(retrievalService.search(bo));
    }

    /**
     * 简单查询接口 (GET 方式)
     *
     * @param query 查询文本
     * @param kbId  知识库ID (可选)
     * @param topK  返回数量 (默认 5)
     */
    @SaCheckPermission("ai:retrieval:search")
    @GetMapping("/query")
    public R<List<KmRetrievalResultVo>> query(
            @RequestParam String query,
            @RequestParam(required = false) Long kbId,
            @RequestParam(defaultValue = "5") Integer topK) {

        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery(query);
        bo.setTopK(topK);
        bo.setMode("VECTOR");

        if (kbId != null) {
            bo.setKbIds(List.of(kbId));
        }

        return R.ok(retrievalService.search(bo));
    }

    /**
     * 混合检索接口
     *
     * @param query 查询文本
     * @param kbId  知识库ID (可选)
     * @param topK  返回数量 (默认 5)
     */
    @SaCheckPermission("ai:retrieval:search")
    @GetMapping("/hybrid")
    public R<List<KmRetrievalResultVo>> hybridQuery(
            @RequestParam String query,
            @RequestParam(required = false) Long kbId,
            @RequestParam(defaultValue = "5") Integer topK) {

        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery(query);
        bo.setTopK(topK);
        bo.setMode("HYBRID");

        if (kbId != null) {
            bo.setKbIds(List.of(kbId));
        }

        return R.ok(retrievalService.search(bo));
    }
}
