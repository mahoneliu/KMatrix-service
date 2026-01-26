package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI聊天Service接口
 *
 * @author Mahone
 * @date 2025-12-31
 */
public interface IKmChatService {

    /**
     * 流式对话
     *
     * @param bo 发送消息参数
     * @return SSE发射器
     */
    SseEmitter streamChat(KmChatSendBo bo);

    /**
     * 普通对话(非流式)
     *
     * @param bo 发送消息参数
     * @return AI响应内容
     */
    String chat(KmChatSendBo bo);

    /**
     * 获取会话历史消息
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @return 消息列表
     */
    List<KmChatMessageVo> getHistory(Long sessionId, Long userId);

    /**
     * 获取应用的所有会话 (仅ID)
     *
     * @param appId  应用ID
     * @param userId 当前用户ID
     * @return 会话ID列表
     */
    List<Long> getSessionsByAppId(Long appId, Long userId);

    /**
     * 获取应用下的会话列表
     *
     * @param appId  应用ID
     * @param userId 当前用户ID
     * @return 会话列表
     */
    List<KmChatSessionVo> getSessionList(Long appId, Long userId);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @return 是否成功
     */
    Boolean clearHistory(Long sessionId, Long userId);

    /**
     * 清除应用下所有会话
     *
     * @param appId  应用ID
     * @param userId 当前用户ID
     * @return 是否成功
     */
    Boolean clearAppHistory(Long appId, Long userId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @param userId    当前用户ID
     * @return 是否成功
     */
    Boolean updateSessionTitle(Long sessionId, String title, Long userId);

    /**
     * 获取会话的执行详情
     *
     * @param sessionId 会话ID
     * @param userId    当前用户ID
     * @return 执行详情列表
     */
    List<org.dromara.ai.domain.vo.KmNodeExecutionVo> getExecutionDetails(Long sessionId, Long userId);

}
