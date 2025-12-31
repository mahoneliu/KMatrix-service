package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用访问统计对象 km_app_access_stat
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@TableName("km_app_access_stat")
public class KmAppAccessStat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总访问次数
     */
    private Long accessCount;

    /**
     * 最后访问时间
     */
    private Date lastAccessTime;

}
