package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmAppVersion;
import org.dromara.ai.domain.vo.config.AppSnapshot;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serializable;
import java.util.Date;

/**
 * 应用历史版本视图对象 km_app_version
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Data
@AutoMapper(target = KmAppVersion.class)
public class KmAppVersionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
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
     * 创建人名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")
    private String createByName;

    /**
     * 版本说明
     */
    private String remark;

}
