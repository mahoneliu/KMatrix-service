package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.domain.KmDataset;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据集Bo
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@AutoMapper(target = KmDataset.class, reverseConvertGenerate = false)
public class KmDatasetBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    private Long id;

    /**
     * 所属知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long kbId;

    /**
     * 数据集名称
     */
    @NotBlank(message = "数据集名称不能为空")
    private String name;

    /**
     * 类型 (FILE/WEB/MANUAL)
     */
    @NotBlank(message = "数据集类型不能为空")
    private String type;

    /**
     * ETL配置 (JSON)
     */
    private Map<String, Object> config;
}
