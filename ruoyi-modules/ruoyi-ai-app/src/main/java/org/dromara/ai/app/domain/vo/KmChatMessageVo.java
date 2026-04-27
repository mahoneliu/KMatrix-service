package org.dromara.ai.app.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.ai.app.domain.KmChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import org.dromara.ai.workflow.domain.vo.KmNodeExecutionVo;

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

    /**
     * 用户反馈状态: 0=无, 1=赞同, -1=踩
     */
    private Integer feedbackStatus;

    /**
     * 中断状态: none=未中断, aborted=已中断
     */
    private String abortStatus;

    /**
     * 请求被中断时已生成的部分内容
     */
    private String partialContent;

    /**
     * 请求被中断的时间
     */
    private java.time.LocalDateTime abortTime;

    /**
     * 中断原因: user_abort=用户主动中断, exception=异常中断, network_error=网络错误
     */
    private String abortReason;

    /**
     * LangChain4j ChatMessage 完整序列化JSON（内部使用，用于历史记录还原）
     */
    private String rawMessageJson;
}
