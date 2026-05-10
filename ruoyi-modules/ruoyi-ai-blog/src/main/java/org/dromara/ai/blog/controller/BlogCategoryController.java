package org.dromara.ai.blog.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.blog.domain.bo.KmBlogCategorySaveBo;
import org.dromara.ai.blog.domain.vo.BlogCategoryVo;
import org.dromara.ai.blog.domain.vo.BlogTopicVo;
import org.dromara.ai.blog.domain.vo.GitCategoryConfigVo;
import org.dromara.ai.blog.service.IBlogCategoryService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客分类管理端 Controller
 *
 * @author Mahone
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog/category")
public class BlogCategoryController extends BaseController {

    private final IBlogCategoryService blogCategoryService;

    @SaCheckLogin
    @GetMapping("/tree")
    public R<List<BlogCategoryVo>> tree() {
        return R.ok(blogCategoryService.getCategoryTree());
    }

    @SaCheckLogin
    @GetMapping("/topics")
    public R<List<BlogTopicVo>> topics() {
        return R.ok(blogCategoryService.listTopics());
    }

    @SaCheckPermission("portal:blog:category:add")
    @Log(title = "博客分类", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@RequestBody @Validated KmBlogCategorySaveBo bo) {
        return R.ok(blogCategoryService.save(bo));
    }

    @SaCheckPermission("portal:blog:category:add")
    @Log(title = "博客分类-GIT", businessType = BusinessType.INSERT)
    @PostMapping("/git")
    public R<Long> saveGit(@RequestBody @Validated KmBlogCategorySaveBo bo) {
        return R.ok(blogCategoryService.saveGitCategory(bo));
    }

    @SaCheckPermission("portal:blog:category:query")
    @GetMapping("/git/{id}/config")
    public R<GitCategoryConfigVo> getGitConfig(@PathVariable Long id) {
        return R.ok(blogCategoryService.getGitCategoryConfig(id));
    }

    @SaCheckPermission("portal:blog:category:edit")
    @Log(title = "博客分类", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody KmBlogCategorySaveBo bo) {
        return R.ok(blogCategoryService.update(id, bo));
    }

    @SaCheckPermission("portal:blog:category:edit")
    @Log(title = "博客分类-GIT", businessType = BusinessType.UPDATE)
    @PutMapping("/git/{id}")
    public R<Boolean> updateGit(@PathVariable Long id, @RequestBody KmBlogCategorySaveBo bo) {
        return R.ok(blogCategoryService.updateGitCategory(id, bo));
    }

    @SaCheckPermission("portal:blog:category:remove")
    @Log(title = "博客分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(blogCategoryService.deleteById(id));
    }
}
