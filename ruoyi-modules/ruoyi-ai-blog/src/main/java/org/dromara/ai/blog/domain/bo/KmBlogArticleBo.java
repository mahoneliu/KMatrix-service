package org.dromara.ai.blog.domain.bo;

import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 博客文章查询 BO（管理端）
 *
 * @author KMatrix
 */
@Data
public class KmBlogArticleBo extends BaseEntity {

    private String title;
    private Long categoryId;
    private String status;
    private String source;
    private String tags;
}
