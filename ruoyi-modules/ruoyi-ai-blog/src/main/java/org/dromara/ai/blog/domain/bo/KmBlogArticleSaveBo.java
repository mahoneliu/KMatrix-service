package org.dromara.ai.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 博客文章新增/修改 BO
 *
 * @author KMatrix
 */
@Data
public class KmBlogArticleSaveBo {

    @NotBlank(message = "文章标题不能为空")
    private String title;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private String content;
    private String description;
    private String coverImage;
    private String tags;

    @NotBlank(message = "文章状态不能为空")
    private String status;

    private Long datasetId;
    private String slug;
}
