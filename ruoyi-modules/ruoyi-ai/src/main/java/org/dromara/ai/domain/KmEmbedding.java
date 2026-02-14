package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一向量存储对象 km_embedding
 * 解耦 chunk 的文本和向量化存储
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Data
@TableName(value = "km_embedding")
public class KmEmbedding implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 关联的源 ID (question_id 或 chunk_id)
     */
    private Long sourceId;

    /**
     * 来源类型: 0=QUESTION, 1=CONTENT, 2=TITLE
     */
    private Integer sourceType;

    /**
     * 向量 (存储为 float[] 或 String)
     * 注意: PostgreSQL 使用 vector 类型，需要自定义 TypeHandler
     */
    @TableField(exist = false)
    private float[] embedding;

    /**
     * 向量字符串格式 (用于 SQL 插入)
     */
    @TableField(exist = false)
    private String embeddingString;

    /**
     * 原始文本内容 (用于全文检索)
     */
    private String textContent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 来源类型枚举
     */
    public static class SourceType {
        public static final int QUESTION = 0;
        public static final int CONTENT = 1;
        public static final int TITLE = 2;
    }
}
