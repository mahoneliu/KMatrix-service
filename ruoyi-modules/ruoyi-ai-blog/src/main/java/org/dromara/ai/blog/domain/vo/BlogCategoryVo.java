package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 博客分类 VO（含专题节点字段）
 *
 * @author KMatrix
 */
@Data
public class BlogCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String source;
    private String isTopic;
    private Long datasetId;
    private Long kbId;
    private String topicSlug;
    private String customDomain;
    private String kbName;
    private String datasetName;
    private Integer orderNum;
    private String gitOwner;
    private String gitRepo;
    private String gitBranch;
    private String gitRootPath;
    private Boolean hasToken;
    private String gitPlatform;
    private Integer articleCount;
    private List<BlogCategoryVo> children;
}
