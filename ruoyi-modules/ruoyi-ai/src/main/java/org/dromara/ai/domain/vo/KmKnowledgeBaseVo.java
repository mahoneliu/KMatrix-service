package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmKnowledgeBase;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识库VO
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmKnowledgeBase.class)
public class KmKnowledgeBaseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 所属用户ID
     */
    private Long ownerId;

    /**
     * 权限等级 (PRIVATE/TEAM/PUBLIC)
     */
    private String permissionLevel;

    /**
     * 状态 (ACTIVE/ARCHIVED)
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 数据集数量
     */
    private Integer datasetCount;

    /**
     * 文档数量
     */
    private Integer documentCount;
}
