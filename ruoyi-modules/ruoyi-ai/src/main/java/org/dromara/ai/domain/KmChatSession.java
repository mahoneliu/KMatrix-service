package org.dromara.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 聊天会话对象 km_chat_session
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_chat_session")
public class KmChatSession extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "session_id")
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
     * 删除标志(0代表存在 1代表删除)
     */
    private String delFlag;
}
