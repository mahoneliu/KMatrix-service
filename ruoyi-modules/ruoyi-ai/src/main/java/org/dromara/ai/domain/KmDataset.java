package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Map;

/**
 * 数据集对象 km_dataset
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_dataset", autoResultMap = true)
public class KmDataset extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 数据集ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 类型 (FILE/WEB/MANUAL)
     */
    private String type;

    /**
     * ETL配置 (JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;
}
