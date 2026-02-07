package org.dromara.ai.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分块预览结果VO
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
public class ChunkPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 临时分块ID (用于前端标识)
     */
    private String chunkId;

    /**
     * 标题 (可为空)
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 序号
     */
    private Integer index;
}
