package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用-知识库关联对象 km_app_knowledge
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@TableName("km_app_knowledge")
public class KmAppKnowledge implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
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
