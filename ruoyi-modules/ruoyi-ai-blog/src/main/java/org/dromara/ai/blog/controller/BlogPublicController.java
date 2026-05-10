package org.dromara.ai.blog.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.blog.domain.vo.*;
import org.dromara.ai.blog.service.IBlogCategoryService;
import org.dromara.ai.blog.service.IBlogPublicService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客公共门户 Controller（匿名访问）
 *
 * @author Mahone
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/blog/public")
public class BlogPublicController extends BaseController {

    private final IBlogPublicService blogPublicService;
    private final IBlogCategoryService blogCategoryService;

    @Value("${blog.git.internal-key:}")
    private String internalApiKey;

    @SaIgnore
    @GetMapping("/topics")
    public R<List<BlogTopicVo>> topics() {
        return R.ok(blogPublicService.listTopics());
    }

    @SaIgnore
    @GetMapping("/categories")
    public R<List<BlogCategoryVo>> categories(@RequestParam(required = false) String topicSlug) {
        return R.ok(blogPublicService.getCategoryTree(topicSlug));
    }

    @SaIgnore
    @GetMapping("/articles")
    public TableDataInfo<BlogArticlePublicVo> articles(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String topicSlug,
            PageQuery pageQuery) {
        return blogPublicService.listPublished(categoryId, tag, topicSlug, pageQuery);
    }

    @SaIgnore
    @GetMapping("/articles/{slug}")
    public R<BlogArticleDetailVo> articleDetail(@PathVariable String slug) {
        BlogArticleDetailVo detail = blogPublicService.getBySlug(slug);
        if (detail == null) {
            return R.fail(404, "文章不存在");
        }
        return R.ok(detail);
    }

    @SaIgnore
    @GetMapping("/slugs")
    public R<List<String>> slugs(@RequestParam(required = false) String topicSlug) {
        return R.ok(blogPublicService.getAllPublishedSlugs(topicSlug));
    }

    @SaIgnore
    @GetMapping("/tags")
    public R<List<BlogTagVo>> tags() {
        return R.ok(blogPublicService.getAllTags());
    }

    @SaIgnore
    @GetMapping("/git/token")
    public R<GitCategoryTokenVo> getGitToken(
            @RequestParam Long categoryId,
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {
        if (internalApiKey != null && !internalApiKey.isEmpty()
                && !internalApiKey.equals(internalKey)) {
            return R.fail(403, "无效的内部服务密钥");
        }
        return R.ok(blogCategoryService.getGitCategoryToken(categoryId));
    }
}
