package org.dromara.ai.blog.service;

import org.dromara.ai.blog.domain.bo.KmBlogArticleBo;
import org.dromara.ai.blog.domain.bo.KmBlogArticleSaveBo;
import org.dromara.ai.blog.domain.vo.KmBlogArticleVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 博客文章管理服务接口
 *
 * @author KMatrix
 */
public interface IBlogArticleService {

    TableDataInfo<KmBlogArticleVo> pageList(KmBlogArticleBo bo, PageQuery pageQuery);

    KmBlogArticleVo queryById(Long id);

    Long save(KmBlogArticleSaveBo bo);

    Boolean update(Long id, KmBlogArticleSaveBo bo);

    Boolean toggleStatus(Long id, String status);

    Boolean deleteByIds(List<Long> ids);

    Boolean updateStatusByIds(List<Long> ids, String status);

    Boolean syncToKb(Long id);

    Boolean syncToKbBatch(List<Long> ids);
}
