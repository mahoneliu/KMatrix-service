package org.dromara.ai.service;

import org.dromara.ai.domain.bo.ChunkResult;

import java.util.List;

/**
 * 向量化服务接口
 * 提供文档分块的向量化和存储功能
 * 设计为可复用的服务，支持工作流节点调用
 *
 * @author Mahone
 * @date 2026-02-07
 */
public interface IKmEmbeddingService {

    /**
     * 为分块列表生成向量并存储
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @param chunks     分块列表
     */
    void embedAndStoreChunks(Long documentId, Long kbId, List<ChunkResult> chunks);

    /**
     * 为QA对分块列表生成向量并存储
     * QA对特殊处理：每个chunk的metadata中包含questions列表，需要创建问题-答案关联
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @param chunks     QA对分块列表（metadata包含questions字段）
     */
    void embedAndStoreQaChunks(Long documentId, Long kbId, List<ChunkResult> chunks);

    /**
     * 为标题生成向量并存储（文档级别，只存一次）
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @param title      文档标题
     */
    void embedTitleForDocument(Long documentId, Long kbId, String title);

    /**
     * 为单个文本内容生成向量
     *
     * @param text 文本内容
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 批量为文本生成向量
     *
     * @param texts 文本列表
     * @return 向量数组列表
     */
    List<float[]> embedBatch(List<String> texts);
}
