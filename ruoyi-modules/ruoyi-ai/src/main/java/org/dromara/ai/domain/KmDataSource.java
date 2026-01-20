package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 数据源配置对象 km_data_source
 * 用于数据库查询节点连接业务数据库
 *
 * @author Mahone
 * @date 2026-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_data_source")
public class KmDataSource extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @TableId
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
     * dynamic-datasource的数据源标识 (sourceType=DYNAMIC时使用)
     */
    private String dsKey;

    /**
     * JDBC驱动类 (sourceType=MANUAL时使用)
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
     * 密码 (加密存储)
     */
    private String password;

    /**
     * 数据库类型 (mysql/postgresql/oracle等)
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
