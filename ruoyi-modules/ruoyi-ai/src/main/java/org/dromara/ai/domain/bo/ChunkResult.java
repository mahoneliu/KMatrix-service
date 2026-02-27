package org.dromara.ai.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.ai.domain.KmDocumentChunk;

import java.util.List;
import java.util.Map;

/**
 * 分块结果DTO
 * 用于EtlHandler返回的文档分块数据
 * 支持父子分块：父块的 children 列表包含子块内容
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResult {

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块标题（可选）
     */
    private String title;

    /**
     * 分块元数据
     */
    private Map<String, Object> metadata;

    /**
     * 块类型: 参考 {@link KmDocumentChunk.ChunkType}
     * 0=PARENT, 1=CHILD, 2=STANDALONE
     * 未设置时默认为 STANDALONE
     */
    @Builder.Default
    private int chunkType = KmDocumentChunk.ChunkType.STANDALONE;

    /**
     * 子块列表（仅父块有效）
     * 子块的 chunkType = CHILD
     */
    private List<ChunkResult> children;

    /**
     * 便捷构造方法：仅内容
     */
    public static ChunkResult of(String content) {
        return ChunkResult.builder().content(content).build();
    }

    /**
     * 便捷构造方法：内容+标题
     */
    public static ChunkResult of(String content, String title) {
        return ChunkResult.builder().content(content).title(title).build();
    }
}
