package org.dromara.ai.workflow.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 节点连接模式视图对象
 *
 * @author Mahone
 */
@Data
public class KmConnectionModeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前模式：whitelist / blacklist */
    private String mode;

    /** 模式标签（中文） */
    private String modeLabel;

    /** 生效逻辑说明 */
    private String description;
}
