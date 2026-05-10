package org.dromara.ai.blog.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.blog.domain.bo.KmBlogArticleBo;
import org.dromara.ai.blog.domain.bo.KmBlogArticleSaveBo;
import org.dromara.ai.blog.domain.vo.KmBlogArticleVo;
import org.dromara.ai.blog.service.IBlogArticleService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客文章管理端 Controller
 *
 * @author KMatrix
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/blog/article")
public class BlogArticleController extends BaseController {

    private final IBlogArticleService blogArticleService;

    @SaCheckLogin
    @GetMapping("/list")
    public TableDataInfo<KmBlogArticleVo> list(KmBlogArticleBo bo, PageQuery pageQuery) {
        return blogArticleService.pageList(bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{id}")
    public R<KmBlogArticleVo> getById(@PathVariable Long id) {
        return R.ok(blogArticleService.queryById(id));
    }

    @SaCheckLogin
    @PostMapping
    public R<Long> save(@RequestBody @Validated KmBlogArticleSaveBo bo) {
        return R.ok(blogArticleService.save(bo));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Validated KmBlogArticleSaveBo bo) {
        return R.ok(blogArticleService.update(id, bo));
    }

    @SaCheckLogin
    @PutMapping("/{id}/status")
    public R<Boolean> toggleStatus(@PathVariable Long id, @RequestParam String status) {
        return R.ok(blogArticleService.toggleStatus(id, status));
    }

    @SaCheckLogin
    @PutMapping("/batch/status")
    public R<Boolean> batchStatus(@RequestBody List<Long> ids, @RequestParam String status) {
        return R.ok(blogArticleService.updateStatusByIds(ids, status));
    }

    @SaCheckLogin
    @DeleteMapping("/{ids}")
    public R<Boolean> delete(@PathVariable List<Long> ids) {
        return R.ok(blogArticleService.deleteByIds(ids));
    }

    @SaCheckLogin
    @PostMapping("/{id}/sync-kb")
    public R<Boolean> syncToKb(@PathVariable Long id) {
        return R.ok(blogArticleService.syncToKb(id));
    }

    @SaCheckLogin
    @PostMapping("/batch/sync-kb")
    public R<Boolean> syncToKbBatch(@RequestBody List<Long> ids) {
        return R.ok(blogArticleService.syncToKbBatch(ids));
    }
}
