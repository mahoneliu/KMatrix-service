package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.ai.domain.KmKnowledgeBase;

import java.io.Serializable;

/**
 * 知识库Bo
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmKnowledgeBase.class, reverseConvertGenerate = false)
public class KmKnowledgeBaseBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    @NotBlank(message = "{ai.val.kb.name_required}")
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 权限等级 (PRIVATE/TEAM/PUBLIC)
     */
    private String permissionLevel;

    /**
     * 状态 (ACTIVE/ARCHIVED)
     */
    private String status;
}
