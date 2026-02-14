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
    @NotNull(message = "临时文件ID不能为空")
    private Long tempFileId;

    /**
     * 数据集ID
     */
    @NotNull(message = "数据集ID不能为空")
    private Long datasetId;

    /**
     * 分块列表
     */
    @NotEmpty(message = "分块列表不能为空")
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
        @NotNull(message = "分块内容不能为空")
        private String content;
    }
}
