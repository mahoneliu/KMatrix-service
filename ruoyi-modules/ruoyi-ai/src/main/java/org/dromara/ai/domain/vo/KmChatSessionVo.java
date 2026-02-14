package org.dromara.ai.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.domain.KmChatSession;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 聊天会话视图对象 km_chat_session
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@AutoMapper(target = KmChatSession.class)
public class KmChatSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型 (anonymous_user/system_user/third_user)
     */
    private String userType;

    /**
     * 创建时间
     */
    private Date createTime;
}
