package org.dromara.ai.blog.service;

import org.dromara.ai.blog.domain.bo.KmBlogCategorySaveBo;
import org.dromara.ai.blog.domain.vo.BlogCategoryVo;
import org.dromara.ai.blog.domain.vo.BlogTopicVo;
import org.dromara.ai.blog.domain.vo.GitCategoryConfigVo;
import org.dromara.ai.blog.domain.vo.GitCategoryTokenVo;

import java.util.List;

/**
 * 博客分类与专题管理服务接口
 *
 * @author Mahone
 */
public interface IBlogCategoryService {

    List<BlogCategoryVo> getCategoryTree();

    List<BlogTopicVo> listTopics();

    Long save(KmBlogCategorySaveBo bo);

    Boolean update(Long id, KmBlogCategorySaveBo bo);

    Boolean deleteById(Long id);

    Long saveGitCategory(KmBlogCategorySaveBo bo);

    Boolean updateGitCategory(Long id, KmBlogCategorySaveBo bo);

    GitCategoryConfigVo getGitCategoryConfig(Long categoryId);

    GitCategoryTokenVo getGitCategoryToken(Long categoryId);
}
