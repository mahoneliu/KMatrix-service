package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmModel;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * AI模型配置视图对象 km_model
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Data
@AutoMapper(target = KmModel.class)
public class KmModelVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 关联供应商ID
     */
    private Long providerId;

    /**
     * 供应商图标
     */
    private String providerIcon;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型（1语言模型 2向量模型）
     */
    private String modelType;

    /**
     * 基础模型（如 gpt-4）
     */
    private String modelKey;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API Base URL
     */
    private String apiBase;

    /**
     * 支持的基础模型(JSON)
     */
    private String config;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 是否内置（Y是 N否）
     */
    private String isBuiltin;

    /**
     * 模型来源（1公有 2本地）
     */
    private String modelSource;

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

    /**
     * 是否为系统默认模型(0-否 1-是)
     */
    private Integer isDefault;
}
