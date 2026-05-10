package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Git 类型博客分类配置 VO（公开接口使用，不含 token 明文）
 *
 * @author Mahone
 */
@Data
public class GitCategoryConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String owner;
    private String repo;
    private String branch;
    private String rootPath;
    private String repoUrl;
    private boolean hasToken;
}
