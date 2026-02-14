package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmDatabaseMeta;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.List;

/**
 * 数据库元数据业务对象 km_database_meta
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmDatabaseMeta.class, reverseConvertGenerate = false)
public class KmDatabaseMetaBo extends BaseEntity {

    /**
     * 元数据ID
     */
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
     * 列信息列表
     */
    private List<KmDatabaseMeta.ColumnMeta> columns;

}
