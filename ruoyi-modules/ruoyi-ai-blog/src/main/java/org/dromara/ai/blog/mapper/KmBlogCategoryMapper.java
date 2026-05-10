package org.dromara.ai.blog.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.ai.blog.domain.KmBlogCategory;
import org.dromara.ai.blog.domain.vo.BlogCategoryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 博客分类 Mapper 接口
 *
 * @author KMatrix
 */
public interface KmBlogCategoryMapper extends BaseMapperPlus<KmBlogCategory, BlogCategoryVo> {

    KmBlogCategory selectByPath(@Param("path") String path);

    List<BlogCategoryVo> selectTopics();

    List<BlogCategoryVo> selectCategoryTreeWithCount(@Param("topicId") Long topicId);
}
