package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * AI模型配置对象 km_model
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_model", autoResultMap = true)
public class KmModel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    @TableId
    private Long modelId;

    /**
     * 关联供应商ID
     */
    private Long providerId;

    /**
     * 供应商图标
     */
    @TableField(exist = false)
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
    @TableField(typeHandler = JsonTypeHandler.class)
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
     * 是否为系统默认模型(0-否 1-是)
     */
    private Integer isDefault;

}
