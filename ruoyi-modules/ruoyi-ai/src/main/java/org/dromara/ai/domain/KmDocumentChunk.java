package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.pgvector.PGvector;
import lombok.Data;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档切片对象 km_document_chunk
 * 注意: PGvector类型需要自定义TypeHandler
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@TableName(value = "km_document_chunk", autoResultMap = true)
public class KmDocumentChunk {

    private static final long serialVersionUID = 1L;

    /**
     * 切片ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 所属知识库ID (冗余字段，便于查询)
     */
    private Long kbId;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 分块标题 (用于标题向量化)
     */
    private String title;

    /**
     * 父级标题链路 (JSON数组)
     */
    private String parentChain;

    /**
     * 元数据 (JSON: page, source, etc.)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 向量嵌入 (pgvector类型)
     * 注意：此字段在插入时由 embeddingString 字段处理
     */
    @TableField(exist = false)
    private float[] embedding;

    /**
     * 向量嵌入字符串形式 (用于SQL插入)
     * 格式: [1.0,2.0,3.0]
     */
    @TableField(exist = false)
    private String embeddingString;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
