package org.dromara.ai.blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * 博客文章实体
 *
 * @author KMatrix
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_blog_article")
public class KmBlogArticle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
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

    @TableLogic(value = "0", delval = "1")
    private String delFlag;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_ONLINE = "ONLINE";
}
