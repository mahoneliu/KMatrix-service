package org.dromara.ai.blog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.service.IBlogKnowledgeSyncPort;
import org.dromara.ai.knowledge.domain.vo.KmDocumentVo;
import org.dromara.ai.knowledge.service.IKmDocumentService;
import org.springframework.stereotype.Component;

/**
 * 博客知识库同步端口
 *
 * @author Mahone
 */
@Slf4j
@Component("kmBlogKnowledgeSyncPort")
@RequiredArgsConstructor
public class KmBlogKnowledgeSyncPort implements IBlogKnowledgeSyncPort {

    private final IKmDocumentService kmDocumentService;

    @Override
    public Long syncArticle(Long articleId, String title, String content, Long datasetId, Long existingDocId) {
        try {
            if (existingDocId != null) {
                // 已同步过：删除旧文档并重新创建
                try {
                    kmDocumentService.deleteById(existingDocId);
                } catch (Exception e) {
                    log.warn("[KmBlogKnowledgeSyncPort] 删除旧知识库文档失败: kmDocumentId={}, 原因: {}",
                            existingDocId, e.getMessage());
                }
            }
            KmDocumentVo docVo = kmDocumentService.createOnlineDocument(datasetId, title, content,"md");
            if (docVo != null && docVo.getId() != null) {
                log.info("[KmBlogKnowledgeSyncPort] 文章已同步到知识库: articleId={}, kmDocumentId={}",
                        articleId, docVo.getId());
                return docVo.getId();
            }
        } catch (Exception e) {
            log.warn("[KmBlogKnowledgeSyncPort] 知识库同步失败: articleId={}, 原因: {}", articleId, e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteDocument(Long kmDocumentId) {
        try {
            kmDocumentService.deleteById(kmDocumentId);
            log.info("[KmBlogKnowledgeSyncPort] 已删除知识库文档: kmDocumentId={}", kmDocumentId);
        } catch (Exception e) {
            log.warn("[KmBlogKnowledgeSyncPort] 删除知识库文档失败: kmDocumentId={}, 原因: {}", kmDocumentId, e.getMessage());
        }
    }
}
