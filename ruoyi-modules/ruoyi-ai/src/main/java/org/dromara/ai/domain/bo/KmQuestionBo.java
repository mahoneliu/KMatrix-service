package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 问题查询对象
 *
 * @author Mahone
 * @date 2026-02-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmQuestion.class, reverseConvertGenerate = false)
public class KmQuestionBo extends BaseEntity {

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 问题内容
     */
    private String content;

}
