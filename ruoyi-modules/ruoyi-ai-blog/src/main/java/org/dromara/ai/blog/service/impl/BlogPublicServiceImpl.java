package org.dromara.ai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.domain.KmBlogArticle;
import org.dromara.ai.blog.domain.KmBlogCategory;
import org.dromara.ai.blog.domain.vo.*;
import org.dromara.ai.blog.mapper.KmBlogArticleMapper;
import org.dromara.ai.blog.mapper.KmBlogCategoryMapper;
import org.dromara.ai.blog.service.IBlogPublicService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 博客公共门户服务实现（匿名访问）
 *
 * @author KMatrix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogPublicServiceImpl implements IBlogPublicService {

    private static final String CACHE_CATEGORY_TREE = "blog:category:tree";
    private static final String CACHE_SLUGS_ALL = "blog:slugs:all";
    private static final Duration CACHE_10_MIN = Duration.ofMinutes(10);
    private static final Duration CACHE_30_MIN = Duration.ofMinutes(30);

    private final KmBlogArticleMapper articleMapper;
    private final KmBlogCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<BlogTopicVo> listTopics() {
        List<BlogCategoryVo> topics = categoryMapper.selectTopics();
        return topics.stream().map(this::toTopicVo).collect(Collectors.toList());
    }

    @Override
    public List<BlogCategoryVo> getCategoryTree(String topicSlug) {
        String cacheKey = StringUtils.hasText(topicSlug)
                ? CACHE_CATEGORY_TREE + ":" + topicSlug
                : CACHE_CATEGORY_TREE;

        List<BlogCategoryVo> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) return cached;

        Long topicId = null;
        if (StringUtils.hasText(topicSlug)) {
            KmBlogCategory topic = categoryMapper.selectOne(new LambdaQueryWrapper<KmBlogCategory>()
                    .eq(KmBlogCategory::getTopicSlug, topicSlug)
                    .eq(KmBlogCategory::getDelFlag, "0"));
            if (topic != null) topicId = topic.getId();
        }

        List<BlogCategoryVo> flatList = categoryMapper.selectCategoryTreeWithCount(topicId);
        List<BlogCategoryVo> tree = buildTree(flatList, 0L);

        RedisUtils.setCacheObject(cacheKey, tree, CACHE_10_MIN);
        return tree;
    }

    @Override
    public TableDataInfo<BlogArticlePublicVo> listPublished(Long categoryId, String tag,
                                                             String topicSlug, PageQuery pageQuery) {
        LambdaQueryWrapper<KmBlogArticle> wrapper = new LambdaQueryWrapper<KmBlogArticle>()
                .eq(KmBlogArticle::getStatus, KmBlogArticle.STATUS_PUBLISHED)
                .orderByDesc(KmBlogArticle::getPublishedAt);

        if (categoryId != null) wrapper.eq(KmBlogArticle::getCategoryId, categoryId);
        if (StringUtils.hasText(tag)) wrapper.like(KmBlogArticle::getTags, tag);
        if (StringUtils.hasText(topicSlug)) {
            List<Long> categoryIds = getCategoryIdsUnderTopic(topicSlug);
            if (categoryIds.isEmpty()) return new TableDataInfo<>(new ArrayList<>(), 0);
            wrapper.in(KmBlogArticle::getCategoryId, categoryIds);
        }

        wrapper.select(KmBlogArticle.class, field -> !field.getColumn().equals("content"));

        Page<KmBlogArticle> page = articleMapper.selectPage(pageQuery.build(), wrapper);
        List<BlogArticlePublicVo> voList = page.getRecords().stream()
                .map(this::toPublicVo)
                .collect(Collectors.toList());
        return new TableDataInfo<>(voList, page.getTotal());
    }

    @Override
    public BlogArticleDetailVo getBySlug(String slug) {
        KmBlogArticle article = articleMapper.selectBySlug(slug);
        if (article == null || !KmBlogArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
            return null;
        }

        String redisKey = "blog:view:" + article.getId();
        try {
            RedisUtils.incrAtomicValue(redisKey);
        } catch (Exception e) {
            log.debug("[BlogPublicService] 浏览量 incr 失败: {}", e.getMessage());
        }

        BlogArticleDetailVo detail = new BlogArticleDetailVo();
        copyToDetailVo(article, detail);

        KmBlogArticle prev = articleMapper.selectPrev(article.getCategoryId(), article.getPublishedAt());
        KmBlogArticle next = articleMapper.selectNext(article.getCategoryId(), article.getPublishedAt());
        detail.setPrevArticle(prev != null ? toPublicVo(prev) : null);
        detail.setNextArticle(next != null ? toPublicVo(next) : null);

        return detail;
    }

    @Override
    public List<String> getAllPublishedSlugs(String topicSlug) {
        String cacheKey = StringUtils.hasText(topicSlug)
                ? CACHE_SLUGS_ALL + ":" + topicSlug
                : CACHE_SLUGS_ALL;

        List<String> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) return cached;

        LambdaQueryWrapper<KmBlogArticle> wrapper = new LambdaQueryWrapper<KmBlogArticle>()
                .eq(KmBlogArticle::getStatus, KmBlogArticle.STATUS_PUBLISHED)
                .select(KmBlogArticle::getSlug);

        if (StringUtils.hasText(topicSlug)) {
            List<Long> categoryIds = getCategoryIdsUnderTopic(topicSlug);
            if (!categoryIds.isEmpty()) wrapper.in(KmBlogArticle::getCategoryId, categoryIds);
        }

        List<String> slugs = articleMapper.selectList(wrapper).stream()
                .map(KmBlogArticle::getSlug)
                .collect(Collectors.toList());

        RedisUtils.setCacheObject(cacheKey, slugs, CACHE_30_MIN);
        return slugs;
    }

    @Override
    public List<BlogTagVo> getAllTags() {
        String cacheKey = "blog:tags:all";
        List<BlogTagVo> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) return cached;

        List<KmBlogArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KmBlogArticle>()
                        .eq(KmBlogArticle::getStatus, KmBlogArticle.STATUS_PUBLISHED)
                        .isNotNull(KmBlogArticle::getTags)
                        .select(KmBlogArticle::getTags));

        java.util.Map<String, Integer> tagCount = new java.util.HashMap<>();
        for (KmBlogArticle article : articles) {
            for (String tag : parseTags(article.getTags())) {
                tagCount.merge(tag, 1, Integer::sum);
            }
        }

        List<BlogTagVo> result = tagCount.entrySet().stream()
                .map(e -> {
                    BlogTagVo vo = new BlogTagVo();
                    vo.setTag(e.getKey());
                    vo.setArticleCount(e.getValue());
                    return vo;
                })
                .sorted((a, b) -> b.getArticleCount() - a.getArticleCount())
                .collect(Collectors.toList());

        RedisUtils.setCacheObject(cacheKey, result, CACHE_10_MIN);
        return result;
    }

    private List<Long> getCategoryIdsUnderTopic(String topicSlug) {
        KmBlogCategory topic = categoryMapper.selectOne(new LambdaQueryWrapper<KmBlogCategory>()
                .eq(KmBlogCategory::getTopicSlug, topicSlug)
                .eq(KmBlogCategory::getDelFlag, "0"));
        if (topic == null) return new ArrayList<>();
        List<BlogCategoryVo> flatList = categoryMapper.selectCategoryTreeWithCount(topic.getId());
        return flatList.stream().map(BlogCategoryVo::getId).collect(Collectors.toList());
    }

    private List<BlogCategoryVo> buildTree(List<BlogCategoryVo> flatList, Long parentId) {
        List<BlogCategoryVo> result = new ArrayList<>();
        for (BlogCategoryVo vo : flatList) {
            Long pid = vo.getParentId() == null ? 0L : vo.getParentId();
            if (parentId.equals(pid)) {
                vo.setChildren(buildTree(flatList, vo.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private BlogArticlePublicVo toPublicVo(KmBlogArticle article) {
        BlogArticlePublicVo vo = new BlogArticlePublicVo();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSlug(article.getSlug());
        vo.setDescription(article.getDescription());
        vo.setCoverImage(article.getCoverImage());
        vo.setTags(parseTags(article.getTags()));
        vo.setCategoryId(article.getCategoryId());
        vo.setSource(article.getSource());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setViewCount(article.getViewCount());
        return vo;
    }

    private void copyToDetailVo(KmBlogArticle article, BlogArticleDetailVo vo) {
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSlug(article.getSlug());
        vo.setDescription(article.getDescription());
        vo.setCoverImage(article.getCoverImage());
        vo.setTags(parseTags(article.getTags()));
        vo.setCategoryId(article.getCategoryId());
        vo.setSource(article.getSource());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setViewCount(article.getViewCount());
        vo.setContent(article.getContent());
    }

    private List<String> parseTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) return new ArrayList<>();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private BlogTopicVo toTopicVo(BlogCategoryVo categoryVo) {
        BlogTopicVo topicVo = new BlogTopicVo();
        topicVo.setId(categoryVo.getId());
        topicVo.setName(categoryVo.getName());
        topicVo.setPath(categoryVo.getPath());
        topicVo.setDatasetId(categoryVo.getDatasetId());
        topicVo.setTopicSlug(categoryVo.getTopicSlug());
        topicVo.setCustomDomain(categoryVo.getCustomDomain());
        topicVo.setOrderNum(categoryVo.getOrderNum());
        topicVo.setArticleCount(categoryVo.getArticleCount());
        topicVo.setSource(categoryVo.getSource());
        return topicVo;
    }
}
