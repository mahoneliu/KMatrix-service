package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 知识库对象 km_knowledge_base
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_knowledge_base")
public class KmKnowledgeBase extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
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
     * 删除标志
     */
    private String delFlag;
}
