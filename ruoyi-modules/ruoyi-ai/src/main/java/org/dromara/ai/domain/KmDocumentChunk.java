package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档切片对象 km_document_chunk
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@TableName(value = "km_document_chunk", autoResultMap = true)
public class KmDocumentChunk {

    private static final long serialVersionUID = 1L;

    /**
     * 块类型常量
     */
    public static final class ChunkType {
        /** 父块 - 不直接向量化，仅存内容供检索后返回给 LLM */
        public static final int PARENT = 0;
        /** 子块 - 参与向量化检索，命中后溢源返回父块内容 */
        public static final int CHILD = 1;
        /** 独立块 - 内容较短无法细分，或存量数据兼容 */
        public static final int STANDALONE = 2;

        private ChunkType() {
        }
    }

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
    @TableField(typeHandler = JsonTypeHandler.class)
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
     * 状态追踪元数据 (JSON格式)
     * 包含 state_time 状态时间记录
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private Map<String, Object> statusMeta;

    /**
     * 块类型: 0=PARENT(父块), 1=CHILD(子块), 2=STANDALONE(独立块)
     * 默认为 2 (STANDALONE)，兼容存量数据
     */
    private Integer chunkType;

    /**
     * 父块ID，子块指向其所属父块的 ID
     * 父块和独立块此字段为 NULL
     */
    private Long parentId;
}
