package org.dromara.ai.blog.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.ai.blog.domain.KmBlogArticle;
import org.dromara.ai.blog.domain.vo.KmBlogArticleVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 博客文章 Mapper 接口
 *
 * @author KMatrix
 */
public interface KmBlogArticleMapper extends BaseMapperPlus<KmBlogArticle, KmBlogArticleVo> {

    Page<KmBlogArticleVo> selectPageVoList(@Param("page") Page<KmBlogArticleVo> page,
                                           @Param(Constants.WRAPPER) Wrapper<KmBlogArticle> queryWrapper);

    KmBlogArticle selectBySourcePath(@Param("sourcePath") String sourcePath);

    KmBlogArticle selectBySlug(@Param("slug") String slug);

    KmBlogArticle selectPrev(@Param("categoryId") Long categoryId,
                              @Param("publishedAt") java.time.LocalDateTime publishedAt);

    KmBlogArticle selectNext(@Param("categoryId") Long categoryId,
                              @Param("publishedAt") java.time.LocalDateTime publishedAt);

    void incrementViewCount(@Param("articleId") Long articleId, @Param("delta") long delta);
}
