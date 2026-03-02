package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分块提交请求BO
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Data
public class ChunkSubmitBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 临时文件ID
     */
    @NotNull(message = "{ai.val.file.temp_id_required}")
    private Long tempFileId;

    /**
     * 数据集ID
     */
    @NotNull(message = "{ai.val.dataset.id_required}")
    private Long datasetId;

    /**
     * 分块列表
     */
    @NotEmpty(message = "{ai.val.chunk.list_required}")
    private List<ChunkItem> chunks;

    /**
     * 分块项
     */
    @Data
    public static class ChunkItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标题 (可选)
         */
        private String title;

        /**
         * 内容
         */
        @NotNull(message = "{ai.val.chunk.content_required}")
        private String content;
    }
}
