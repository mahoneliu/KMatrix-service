package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmAppAccessStat;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用访问统计视图对象 km_app_access_stat
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@AutoMapper(target = KmAppAccessStat.class)
public class KmAppAccessStatVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
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
     * 用户名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "userId")
    private String userName;

    /**
     * 总访问次数
     */
    private Long accessCount;

    /**
     * 最后访问时间
     */
    private Date lastAccessTime;

}
