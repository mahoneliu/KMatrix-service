package org.dromara.ai.blog.service;

/**
 * 博客知识库同步端口接口（SPI）
 * <p>
 *
 * @author Mahone
 */
public interface IBlogKnowledgeSyncPort {

    /**
     * 将博客文章同步到知识库
     *
     * @param articleId     文章 ID
     * @param title         文章标题
     * @param content       文章 Markdown 内容
     * @param datasetId     目标数据集 ID
     * @param existingDocId 已有的知识库文档 ID（首次同步时为 null）
     * @return 同步后的知识库文档 ID，失败或跳过时返回 null
     */
    Long syncArticle(Long articleId, String title, String content, Long datasetId, Long existingDocId);

    /**
     * 删除知识库中的博客文章文档
     *
     * @param kmDocumentId 知识库文档 ID
     */
    void deleteDocument(Long kmDocumentId);
}
