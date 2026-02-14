package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmRetrievalResultVo;

import java.util.List;

/**
 * 重排序服务接口
 *
 * @author Mahone
 * @date 2026-01-29
 */
public interface IKmRerankService {

    /**
     * 对检索结果进行重排序
     *
     * @param query   查询文本
     * @param results 原始检索结果
     * @param topK    返回的最大结果数
     * @return 重排序后的结果列表
     */
    List<KmRetrievalResultVo> rerank(String query, List<KmRetrievalResultVo> results, int topK);

    /**
     * 检查 Reranker 模型是否已启用
     *
     * @return true 如果已启用
     */
    boolean isEnabled();
}
