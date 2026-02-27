package org.dromara.ai.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量创建网页链接业务对象
 *
 * @author AI
 */
@Data
public class BatchWebLinkBo {

    /**
     * 数据集ID
     */
    @NotNull(message = "数据集ID不能为空")
    private Long datasetId;

    /**
     * 网页URL列表
     */
    @NotEmpty(message = "URL列表不能为空")
    private List<String> urls;
}
