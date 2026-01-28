package org.dromara.ai.service;

import java.util.List;

/**
 * ETL处理服务接口
 * 负责文档的解析、分块、向量化
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmEtlService {

    /**
     * 处理文档 (异步)
     * 包含: 解析 -> 分块 -> 向量化 -> 存储
     *
     * @param documentId 文档ID
     */
    void processDocumentAsync(Long documentId);

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
     * @param chunks     切片列表
     */
    void embedAndStore(Long documentId, List<String> chunks);

    /**
     * 删除文档的所有切片
     *
     * @param documentId 文档ID
     */
    void deleteChunksByDocumentId(Long documentId);
}
