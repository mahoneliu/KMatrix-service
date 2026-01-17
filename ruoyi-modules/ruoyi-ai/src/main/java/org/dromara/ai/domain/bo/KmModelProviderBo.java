package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 模型供应商业务对象 km_model_provider
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmModelProvider.class, reverseConvertGenerate = false)
public class KmModelProviderBo extends BaseEntity {

    /**
     * 供应商ID
     */
    private Long providerId;

    /**
     * 供应商名称
     */
    private String providerName;

    /**
     * 供应商标识(openai/ollama)
     */
    private String providerKey;

    /**
     * 默认API地址
     */
    private String defaultEndpoint;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * 官网URL
     */
    private String siteUrl;

    /**
     * 配置参数定义 (JSON)
     */
    private String configSchema;

    /**
     * 支持的基础模型(JSON)
     */
    private String models;

    /**
     * 供应商类型（1公用 2本地）
     */
    private String providerType;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

}
