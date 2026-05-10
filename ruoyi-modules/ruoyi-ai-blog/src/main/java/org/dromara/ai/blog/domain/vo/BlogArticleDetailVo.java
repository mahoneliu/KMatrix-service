package org.dromara.ai.blog.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客文章详情 VO（含 Markdown 内容和上下篇导航）
 *
 * @author KMatrix
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BlogArticleDetailVo extends BlogArticlePublicVo {

    private String content;
    private BlogArticlePublicVo prevArticle;
    private BlogArticlePublicVo nextArticle;
}
