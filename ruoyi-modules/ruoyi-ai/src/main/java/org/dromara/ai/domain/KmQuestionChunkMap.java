package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 问题与分块关联对象 km_question_chunk_map
 * 一个 Chunk 可以关联多个问题 (1:N)
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Data
@TableName(value = "km_question_chunk_map")
public class KmQuestionChunkMap implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 问题ID
     */
    private Long questionId;

    /**
     * 分块ID
     */
    private Long chunkId;
}
