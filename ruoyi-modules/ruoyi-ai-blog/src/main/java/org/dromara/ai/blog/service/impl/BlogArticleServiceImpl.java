package org.dromara.ai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.domain.KmBlogArticle;
import org.dromara.ai.blog.domain.bo.KmBlogArticleBo;
import org.dromara.ai.blog.domain.bo.KmBlogArticleSaveBo;
import org.dromara.ai.blog.domain.vo.KmBlogArticleVo;
import org.dromara.ai.blog.mapper.KmBlogArticleMapper;
import org.dromara.ai.blog.scanner.BlogMarkdownProcessor;
import org.dromara.ai.blog.service.IBlogArticleService;
import org.dromara.ai.blog.service.IBlogKnowledgeSyncPort;
import org.dromara.ai.blog.util.PinyinUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客文章管理服务实现
 *
 * @author KMatrix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl implements IBlogArticleService {

    private final KmBlogArticleMapper articleMapper;
    private final BlogMarkdownProcessor markdownProcessor;
    private final IBlogKnowledgeSyncPort knowledgeSyncPort;

    @Override
    public TableDataInfo<KmBlogArticleVo> pageList(KmBlogArticleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmBlogArticle> wrapper = buildQueryWrapper(bo);
        Page<KmBlogArticleVo> page = articleMapper.selectPageVoList(pageQuery.build(), wrapper);
        return new TableDataInfo<>(page.getRecords(), page.getTotal());
    }

    @Override
    public KmBlogArticleVo queryById(Long id) {
        KmBlogArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在: id=" + id);
        }
        return MapstructUtils.convert(article, KmBlogArticleVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(KmBlogArticleSaveBo bo) {
        String slug = resolveSlug(bo.getSlug(), bo.getTitle());
        checkSlugUnique(slug, null);

        KmBlogArticle article = new KmBlogArticle();
        article.setCategoryId(bo.getCategoryId());
        article.setTitle(bo.getTitle());
        article.setSlug(slug);
        article.setContent(bo.getContent());
        article.setDescription(bo.getDescription());
        article.setCoverImage(bo.getCoverImage());
        article.setTags(bo.getTags());
        article.setStatus(resolveStatus(bo.getStatus()));
        article.setSource(KmBlogArticle.SOURCE_ONLINE);
        article.setDatasetId(bo.getDatasetId());
        article.setViewCount(0);

        if (StringUtils.hasText(bo.getContent())) {
            article.setContentHash(DigestUtils.md5DigestAsHex(
                    bo.getContent().getBytes(StandardCharsets.UTF_8)));
        }
        if (KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleMapper.insert(article);

        if (KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
            asyncSyncToKb(article);
        }

        evictBlogCache();
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, KmBlogArticleSaveBo bo) {
        KmBlogArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在: id=" + id);
        }

        if (KmBlogArticle.SOURCE_FILE.equals(article.getSource())) {
            if (StringUtils.hasText(bo.getContent()) && !bo.getContent().equals(article.getContent())) {
                throw new ServiceException("FILE 来源的文章内容由文件管理，不允许通过管理端修改 content 字段");
            }
            article.setStatus(resolveStatus(bo.getStatus()));
            article.setDatasetId(bo.getDatasetId());
            article.setDescription(bo.getDescription());
            article.setCoverImage(bo.getCoverImage());
            article.setTags(bo.getTags());
        } else {
            String slug = resolveSlug(bo.getSlug(), bo.getTitle());
            checkSlugUnique(slug, id);

            article.setCategoryId(bo.getCategoryId());
            article.setTitle(bo.getTitle());
            article.setSlug(slug);
            article.setDescription(bo.getDescription());
            article.setCoverImage(bo.getCoverImage());
            article.setTags(bo.getTags());
            article.setDatasetId(bo.getDatasetId());

            boolean statusChanged = !resolveStatus(bo.getStatus()).equals(article.getStatus());
            article.setStatus(resolveStatus(bo.getStatus()));

            if (StringUtils.hasText(bo.getContent())) {
                article.setContent(bo.getContent());
                article.setContentHash(DigestUtils.md5DigestAsHex(
                        bo.getContent().getBytes(StandardCharsets.UTF_8)));
            }
            if (KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus()) && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
            if (statusChanged && KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
                asyncSyncToKb(article);
            }
        }

        articleMapper.updateById(article);
        evictBlogCache();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleStatus(Long id, String status) {
        KmBlogArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在: id=" + id);
        }

        String oldStatus = article.getStatus();
        String newStatus = resolveStatus(status);
        article.setStatus(newStatus);

        if (KmBlogArticle.STATUS_PUBLISHED.equals(newStatus) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleMapper.updateById(article);

        if (KmBlogArticle.STATUS_DRAFT.equals(oldStatus) && KmBlogArticle.STATUS_PUBLISHED.equals(newStatus)) {
            asyncSyncToKb(article);
        }

        evictBlogCache();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatusByIds(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) return false;
        String newStatus = resolveStatus(status);
        List<KmBlogArticle> articles = articleMapper.selectBatchIds(ids);
        for (KmBlogArticle article : articles) {
            String oldStatus = article.getStatus();
            article.setStatus(newStatus);
            if (KmBlogArticle.STATUS_PUBLISHED.equals(newStatus) && article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
            articleMapper.updateById(article);
            if (KmBlogArticle.STATUS_DRAFT.equals(oldStatus) && KmBlogArticle.STATUS_PUBLISHED.equals(newStatus)) {
                asyncSyncToKb(article);
            }
        }
        evictBlogCache();
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;
        List<KmBlogArticle> articles = articleMapper.selectBatchIds(ids);
        List<Long> kmDocumentIds = articles.stream()
                .map(KmBlogArticle::getKmDocumentId)
                .filter(docId -> docId != null)
                .toList();

        articleMapper.deleteByIds(ids);
        evictBlogCache();

        if (!kmDocumentIds.isEmpty()) {
            asyncDeleteKmDocuments(kmDocumentIds);
        }
        return true;
    }

    @Override
    public Boolean syncToKb(Long id) {
        KmBlogArticle article = articleMapper.selectById(id);
        if (article == null) throw new ServiceException("文章不存在: id=" + id);
        if (!KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new ServiceException("只有已发布的文章才能同步到知识库");
        }

        String topicDir = extractTopicDir(article.getSourcePath());
        Long datasetId = markdownProcessor.resolveDatasetId(article.getDatasetId(), topicDir);
        if (datasetId == null) {
            throw new ServiceException("未配置知识库数据集ID，请在文章或专题中配置 datasetId");
        }

        markdownProcessor.syncToKnowledgeBase(article, datasetId);
        return true;
    }

    @Override
    public Boolean syncToKbBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;
        for (Long id : ids) {
            try {
                syncToKb(id);
            } catch (Exception e) {
                log.warn("[BlogArticleService] 批量同步知识库部分失败: articleId={}, 原因: {}", id, e.getMessage());
            }
        }
        return true;
    }

    private LambdaQueryWrapper<KmBlogArticle> buildQueryWrapper(KmBlogArticleBo bo) {
        LambdaQueryWrapper<KmBlogArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(bo.getTitle())) {
            wrapper.apply("a.title LIKE CONCAT('%', {0}, '%')", bo.getTitle());
        }
        if (bo.getCategoryId() != null) {
            wrapper.apply("a.category_id = {0}", bo.getCategoryId());
        }
        if (StringUtils.hasText(bo.getStatus())) {
            wrapper.apply("a.status = {0}", bo.getStatus());
        }
        if (StringUtils.hasText(bo.getSource())) {
            wrapper.apply("a.source = {0}", bo.getSource());
        }
        if (StringUtils.hasText(bo.getTags())) {
            wrapper.apply("a.tags LIKE CONCAT('%', {0}, '%')", bo.getTags());
        }
        return wrapper;
    }

    private String resolveSlug(String slug, String title) {
        if (StringUtils.hasText(slug)) {
            return markdownProcessor.toKebabCase(slug);
        }
        String pinyin = PinyinUtils.toPinyin(title);
        return markdownProcessor.toKebabCase(pinyin);
    }

    private void checkSlugUnique(String slug, Long excludeId) {
        LambdaQueryWrapper<KmBlogArticle> wrapper = new LambdaQueryWrapper<KmBlogArticle>()
                .eq(KmBlogArticle::getSlug, slug);
        if (excludeId != null) wrapper.ne(KmBlogArticle::getId, excludeId);
        if (articleMapper.exists(wrapper)) {
            throw new ServiceException("slug 已存在，请修改后重试: " + slug);
        }
    }

    private String resolveStatus(String status) {
        if (StringUtils.hasText(status) && status.equalsIgnoreCase("PUBLISHED")) {
            return KmBlogArticle.STATUS_PUBLISHED;
        }
        return KmBlogArticle.STATUS_DRAFT;
    }

    private String extractTopicDir(String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) return "";
        int firstSlash = sourcePath.indexOf('/');
        return firstSlash < 0 ? "" : sourcePath.substring(0, firstSlash);
    }

    @Async
    public void asyncSyncToKb(KmBlogArticle article) {
        try {
            String topicDir = extractTopicDir(article.getSourcePath());
            Long datasetId = markdownProcessor.resolveDatasetId(article.getDatasetId(), topicDir);
            if (datasetId != null) {
                markdownProcessor.syncToKnowledgeBase(article, datasetId);
            }
        } catch (Exception e) {
            log.warn("[BlogArticleService] 异步知识库同步失败: articleId={}, 原因: {}", article.getId(), e.getMessage());
        }
    }

    @Async
    public void asyncDeleteKmDocuments(List<Long> kmDocumentIds) {
        for (Long docId : kmDocumentIds) {
            try {
                knowledgeSyncPort.deleteDocument(docId);
            } catch (Exception e) {
                log.warn("[BlogArticleService] 删除知识库文档失败: kmDocumentId={}, 原因: {}", docId, e.getMessage());
            }
        }
    }

    private void evictBlogCache() {
        try {
            RedisUtils.deleteObject("blog:category:tree");
            RedisUtils.deleteObject("blog:slugs:all");
        } catch (Exception e) {
            log.debug("[BlogArticleService] 清除缓存失败: {}", e.getMessage());
        }
    }
}
