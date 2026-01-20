package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmDataSource;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据源配置视图对象 km_data_source
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@AutoMapper(target = KmDataSource.class)
public class KmDataSourceVo implements Serializable {

    private static final long serialVersionUID = 1L;

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
