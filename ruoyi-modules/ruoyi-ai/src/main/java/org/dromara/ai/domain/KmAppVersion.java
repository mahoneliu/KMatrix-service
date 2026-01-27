package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.mybatis.handler.JsonTypeHandler;
import lombok.Data;
import org.dromara.ai.domain.vo.config.AppSnapshot;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用历史版本对象 km_app_version
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@TableName(value = "km_app_version", autoResultMap = true)
public class KmAppVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    @TableId
    private Long versionId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 应用配置快照
     */
    @TableField(typeHandler = JsonTypeHandler.class)
    private AppSnapshot appSnapshot;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 版本说明
     */
    private String remark;

}
