package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 博客专题 VO（顶层分类节点的视图）
 *
 * @author KMatrix
 */
@Data
public class BlogTopicVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String path;
    private Long datasetId;
    private String topicSlug;
    private String customDomain;
    private Integer orderNum;
    private Integer articleCount;
    private String source;
    private List<BlogCategoryVo> categories;
}
