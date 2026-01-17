package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmAppKnowledge;

import java.io.Serializable;

/**
 * 应用-知识库关联视图对象 km_app_knowledge
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@AutoMapper(target = KmAppKnowledge.class)
public class KmAppKnowledgeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 排序
     */
    private Integer sort;

}
