package org.dromara.ai.blog.service;

import org.dromara.ai.blog.domain.vo.BlogArticleDetailVo;
import org.dromara.ai.blog.domain.vo.BlogArticlePublicVo;
import org.dromara.ai.blog.domain.vo.BlogCategoryVo;
import org.dromara.ai.blog.domain.vo.BlogTagVo;
import org.dromara.ai.blog.domain.vo.BlogTopicVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 博客公共门户服务接口（匿名访问）
 *
 * @author KMatrix
 */
public interface IBlogPublicService {

    List<BlogTopicVo> listTopics();

    List<BlogCategoryVo> getCategoryTree(String topicSlug);

    TableDataInfo<BlogArticlePublicVo> listPublished(Long categoryId, String tag,
                                                      String topicSlug, PageQuery pageQuery);

    BlogArticleDetailVo getBySlug(String slug);

    List<String> getAllPublishedSlugs(String topicSlug);

    List<BlogTagVo> getAllTags();
}
