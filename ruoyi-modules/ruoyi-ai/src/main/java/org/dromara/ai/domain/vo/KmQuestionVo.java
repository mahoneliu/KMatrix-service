package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmQuestion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 问题 VO
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Data
@AutoMapper(target = KmQuestion.class)
public class KmQuestionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 问题ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 所属知识库ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long kbId;

    /**
     * 问题内容
     */
    private String content;

    /**
     * 命中次数
     */
    private Integer hitNum;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 关联分段数量
     */
    private Integer chunkCount;
}
