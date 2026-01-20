package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.List;

/**
 * 数据库元数据对象 km_database_meta
 * 存储表结构信息供LLM生成SQL时参考
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "km_database_meta", autoResultMap = true)
public class KmDatabaseMeta extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 元数据ID
     */
    @TableId
    private Long metaId;

    /**
     * 关联数据源ID
     */
    private Long dataSourceId;

    /**
     * 元数据来源类型 (DDL: 建表语句 / JDBC: 实时获取)
     */
    private String metaSourceType;

    /**
     * 建表SQL原文 (metaSourceType=DDL时使用)
     */
    private String ddlContent;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 列信息 (JSON数组)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ColumnMeta> columns;

    /**
     * 列元数据内部类
     */
    @Data
    public static class ColumnMeta {
        /**
         * 列名
         */
        private String columnName;
        /**
         * 列类型
         */
        private String columnType;
        /**
         * 列注释
         */
        private String columnComment;
        /**
         * 是否主键
         */
        private Boolean isPrimaryKey;
        /**
         * 是否可空
         */
        private Boolean isNullable;
    }

}
