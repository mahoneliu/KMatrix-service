package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;

import java.util.List;

/**
 * 知识库检索服务接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmRetrievalService {

    /**
     * 执行检索
     *
     * @param bo 检索请求参数
     * @return 检索结果列表
     */
    List<KmRetrievalResultVo> search(KmRetrievalBo bo);

    /**
     * 向量检索
     *
     * @param query      查询文本
     * @param datasetIds 数据集ID列表
     * @param topK       返回数量
     * @param threshold  相似度阈值
     * @return 检索结果
     */

}
