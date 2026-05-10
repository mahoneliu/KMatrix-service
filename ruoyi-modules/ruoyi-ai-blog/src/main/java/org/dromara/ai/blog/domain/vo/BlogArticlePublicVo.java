package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客文章公共门户 VO（精简，不含 content 字段）
 *
 * @author KMatrix
 */
@Data
public class BlogArticlePublicVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String slug;
    private String description;
    private String coverImage;
    private List<String> tags;
    private String categoryPath;
    private Long categoryId;
    private String source;
    private LocalDateTime publishedAt;
    private Integer viewCount;
}
