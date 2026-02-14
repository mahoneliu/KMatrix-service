package org.dromara.ai.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 数据源配置业务对象 km_data_source
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KmDataSource.class, reverseConvertGenerate = false)
public class KmDataSourceBo extends BaseEntity {

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据源名称
     */
    private String dataSourceName;

    /**
     * 数据源类型 (DYNAMIC: 多数据源选择 / MANUAL: 手工录入)
     */
    private String sourceType;

    /**
     * dynamic-datasource的数据源标识
     */
    private String dsKey;

    /**
     * JDBC驱动类
     */
    private String driverClassName;

    /**
     * 连接URL
     */
    private String jdbcUrl;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 是否启用 (0停用/1启用)
     */
    private String isEnabled;

    /**
     * 备注
     */
    private String remark;

}
