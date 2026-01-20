package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDatabaseMeta;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据库元数据视图对象 km_database_meta
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@AutoMapper(target = KmDatabaseMeta.class)
public class KmDatabaseMetaVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 元数据ID
     */
    private Long metaId;

    /**
     * 关联数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据源名称 (关联查询)
     */
    private String dataSourceName;

    /**
     * 元数据来源类型 (DDL: 建表语句 / JDBC: 实时获取)
     */
    private String metaSourceType;

    /**
     * 建表SQL原文
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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
