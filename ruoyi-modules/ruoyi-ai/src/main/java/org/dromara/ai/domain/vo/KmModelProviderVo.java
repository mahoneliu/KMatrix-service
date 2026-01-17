package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 模型供应商视图对象 km_model_provider
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Data
@AutoMapper(target = KmModelProvider.class)
public class KmModelProviderVo implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 支持的基础模型(JSON)
     */
    private String models;

    /**
     * 供应商类型（1公用 2本地）
     */
    private String providerType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "updateBy")
    private String updateByName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
