package org.dromara.ai.blog.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.blog.domain.KmBlogArticle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 博客文章 VO（管理端，含完整字段）
 *
 * @author KMatrix
 */
@Data
@AutoMapper(target = KmBlogArticle.class)
public class KmBlogArticleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryPath;
    private String categorySource;
    private String title;
    private String slug;
    private String content;
    private String description;
    private String coverImage;
    private String tags;
    private String status;
    private String source;
    private Long datasetId;
    private Long kmDocumentId;
    private String sourcePath;
    private String contentHash;
    private LocalDateTime publishedAt;
    private Integer viewCount;
    private Date createTime;
    private Date updateTime;
}
