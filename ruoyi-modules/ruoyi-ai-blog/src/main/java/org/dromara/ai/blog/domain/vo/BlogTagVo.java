package org.dromara.ai.blog.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 博客标签 VO
 *
 * @author KMatrix
 */
@Data
public class BlogTagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tag;
    private Integer articleCount;
}
