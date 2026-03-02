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
    @NotNull(message = "{ai.val.dataset.id_required}")
    private Long datasetId;

    /**
     * 网页URL列表
     */
    @NotEmpty(message = "{ai.val.url.list_required}")
    private List<String> urls;
}
