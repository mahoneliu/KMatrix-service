package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("km_model")
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

}
