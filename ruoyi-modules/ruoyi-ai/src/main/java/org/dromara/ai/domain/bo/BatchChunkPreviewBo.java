package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量分块预览请求BO
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
public class BatchChunkPreviewBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 临时文件ID列表
     */
    @NotNull(message = "临时文件ID列表不能为空")
    private List<Long> tempFileIds;

    /**
     * 分块策略 (AUTO=自动, CUSTOM=自定义)
     */
    @NotNull(message = "分块策略不能为空")
    private String chunkStrategy;

    /**
     * 自定义分隔符列表 (当strategy=CUSTOM时必填)
     */
    private List<String> separators;

    /**
     * 最大分块大小 (可选)
     */
    private Integer chunkSize;

    /**
     * 重叠大小 (可选)
     */
    private Integer overlap;
}
