package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Git 类型博客分类 Token VO（仅供 Nuxt server/api 内部调用，含 token 明文）
 *
 * @author Mahone
 */
@Data
public class GitCategoryTokenVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;
    private String owner;
    private String repo;
    private String branch;
    private String rootPath;
    /** Git 平台：github / gitee */
    private String platform;
}
