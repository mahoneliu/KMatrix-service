package org.dromara.ai.blog.scanner;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.domain.KmBlogArticle;
import org.dromara.ai.blog.domain.KmBlogCategory;
import org.dromara.ai.blog.mapper.KmBlogArticleMapper;
import org.dromara.ai.blog.mapper.KmBlogCategoryMapper;
import org.dromara.ai.blog.service.IBlogKnowledgeSyncPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 博客 Markdown 文件处理核心组件
 * <p>
 * 知识库同步通过 {@link IBlogKnowledgeSyncPort} 接口解耦，
 * 可按需扩展。
 *
 * @author KMatrix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogMarkdownProcessor {

    private final KmBlogArticleMapper articleMapper;
    private final KmBlogCategoryMapper categoryMapper;
    private final IBlogKnowledgeSyncPort knowledgeSyncPort;
    private final JdbcTemplate jdbcTemplate;

    public Long resolveDatasetId(Long frontmatterDatasetId, String topicDir) {
        if (frontmatterDatasetId != null && frontmatterDatasetId > 0) {
            return frontmatterDatasetId;
        }
        if (StringUtils.hasText(topicDir)) {
            KmBlogCategory topicCategory = categoryMapper.selectByPath("/" + topicDir);
            if (topicCategory != null && topicCategory.getDatasetId() != null
                    && topicCategory.getDatasetId() > 0) {
                return topicCategory.getDatasetId();
            }
        }
        try {
            String configValue = jdbcTemplate.queryForObject(
                    "SELECT config_value FROM sys_config WHERE config_key = 'blog.default.dataset.id' LIMIT 1",
                    String.class);
            if (StringUtils.hasText(configValue) && !"0".equals(configValue.trim())) {
                return Long.parseLong(configValue.trim());
            }
        } catch (Exception e) {
            log.debug("[BlogMarkdownProcessor] 读取 sys_config blog.default.dataset.id 失败: {}", e.getMessage());
        }
        return null;
    }

    public void syncToKnowledgeBase(KmBlogArticle article, Long datasetId) {
        if (!KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus()) || datasetId == null) {
            return;
        }
        try {
            Long newDocId = knowledgeSyncPort.syncArticle(article.getId(), article.getTitle(),
                    article.getContent(), datasetId, article.getKmDocumentId());
            if (newDocId != null && !newDocId.equals(article.getKmDocumentId())) {
                articleMapper.update(null,
                        new LambdaUpdateWrapper<KmBlogArticle>()
                                .eq(KmBlogArticle::getId, article.getId())
                                .set(KmBlogArticle::getKmDocumentId, newDocId));
                article.setKmDocumentId(newDocId);
            }
        } catch (Exception e) {
            log.warn("[BlogMarkdownProcessor] 知识库同步失败: articleId={}, 原因: {}", article.getId(), e.getMessage());
        }
    }


    public String toKebabCase(String input) {
        if (!StringUtils.hasText(input)) return "";
        return input.trim()
                .toLowerCase()
                .replaceAll("[\\s_/]+", "-")
                .replaceAll("[^a-z0-9\\-]", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }

}
