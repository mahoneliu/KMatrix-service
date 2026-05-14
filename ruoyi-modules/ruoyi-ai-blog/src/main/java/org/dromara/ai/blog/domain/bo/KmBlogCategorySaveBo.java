package org.dromara.ai.blog.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 博客分类新增/修改 BO
 *
 * @author Mahone
 */
@Data
public class KmBlogCategorySaveBo {

    private Long parentId = 0L;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Integer orderNum = 0;
    private Long datasetId;
    private String topicSlug;
    private String customDomain;

    // Git 仓库集成字段
    private String path;
    private String gitToken;
    private String gitOwner;
    private String gitRepo;
    private String gitBranch;
    private String gitRootPath;
    private String source;
    /** Git 平台：github / gitee，默认 github */
    private String gitPlatform;
}
