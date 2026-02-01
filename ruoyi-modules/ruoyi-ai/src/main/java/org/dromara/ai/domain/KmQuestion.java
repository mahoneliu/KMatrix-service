package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 问题对象 km_question
 * 用于存储从 QA 对导入或由 LLM 自动生成的问题
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_question")
public class KmQuestion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 问题ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 问题内容 (限制500字符)
     */
    private String content;

    /**
     * 命中次数
     */
    private Integer hitNum;

    /**
     * 来源类型: IMPORT (导入), LLM (LLM生成)
     */
    private String sourceType;
}
