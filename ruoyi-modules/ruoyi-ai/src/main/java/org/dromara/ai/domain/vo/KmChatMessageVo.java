package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 聊天消息视图对象 km_chat_message
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@AutoMapper(target = KmChatMessage.class)
public class KmChatMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 角色(user/assistant)
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 工作流实例ID
     */
    private Long instanceId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 节点执行记录列表
     */
    private List<KmNodeExecutionVo> executions;
}
