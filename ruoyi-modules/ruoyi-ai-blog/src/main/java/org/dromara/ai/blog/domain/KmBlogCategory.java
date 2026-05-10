package org.dromara.ai.blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 博客分类实体（顶层节点即为"专题"，is_topic='1' 标识）
 *
 * @author Mahone
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_blog_category")
public class KmBlogCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String name;
    private String path;
    private Integer orderNum;
    private String source;
    private String isTopic;
    private Long datasetId;
    private String topicSlug;
    private String customDomain;

    @TableLogic(value = "0", delval = "1")
    private String delFlag;

    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_ONLINE = "ONLINE";
    public static final String SOURCE_GIT = "GIT";

    public static final String IS_TOPIC_YES = "1";
    public static final String IS_TOPIC_NO = "0";

    // Git 仓库集成字段
    private String gitTokenEncrypted;
    private String gitOwner;
    private String gitRepo;
    private String gitBranch;
    private String gitRootPath;
}
