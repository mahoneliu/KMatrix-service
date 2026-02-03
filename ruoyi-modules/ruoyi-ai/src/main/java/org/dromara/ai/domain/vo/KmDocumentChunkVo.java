package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDocumentChunk;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 文档切片 VO
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Data
@AutoMapper(target = KmDocumentChunk.class)
public class KmDocumentChunkVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 切片ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 分块标题
     */
    private String title;

    /**
     * 父级标题链路
     */
    private String parentChain;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 启用状态 (0=禁用, 1=启用)
     */
    private Integer enabled;

    /**
     * 向量化状态 (0=未生成, 1=生成中, 2=已生成, 3=生成失败)
     */
    private Integer embeddingStatus;

    /**
     * 问题生成状态 (0=未生成, 1=生成中, 2=已生成, 3=生成失败)
     */
    private Integer questionStatus;

    /**
     * 状态追踪元数据
     */
    private Map<String, Object> statusMeta;
}
