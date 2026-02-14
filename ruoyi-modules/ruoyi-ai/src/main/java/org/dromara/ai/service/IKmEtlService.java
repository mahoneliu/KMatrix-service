package org.dromara.ai.service;

import java.util.List;

import org.dromara.ai.domain.bo.ChunkResult;
/**
 * ETL处理服务接口
 * 负责文档的解析、分块、向量化
 *
 * @author Mahone
 * @date 2026-01-28
 */
import org.dromara.ai.domain.enums.EmbeddingOption;

/**
 * ETL处理服务接口
 * 负责文档的解析、分块、向量化
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmEtlService {

    /**
     * 处理文档向量化 (异步)
     *
     * @param documentId 文档ID
     * @param option     向量化选项
     */
    void processEmbeddingAsync(Long documentId, EmbeddingOption option);

    /**
     * 处理文档 (异步)
     * 包含: 解析 -> 分块 -> 向量化 -> 存储
     *
     * @param documentId 文档ID
     */
    void processDocumentAsync(Long documentId, List<ChunkResult> chunks);

    /**
     * 解析文档内容
     *
     * @param documentId 文档ID
     * @return 解析后的纯文本
     */
    String parseDocument(Long documentId);

    /**
     * 对文本进行分块
     *
     * @param text      原始文本
     * @param chunkSize 块大小 (字符数)
     * @param overlap   重叠大小
     * @return 分块列表
     */
    List<String> splitText(String text, int chunkSize, int overlap);

    /**
     * 对切片进行向量化并存储
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @param chunks     切片列表
     */
    void embedAndStore(Long documentId, Long kbId, List<String> chunks);

    /**
     * 删除文档的所有切片
     *
     * @param documentId 文档ID
     */
    void deleteChunksByDocumentId(Long documentId);
}
