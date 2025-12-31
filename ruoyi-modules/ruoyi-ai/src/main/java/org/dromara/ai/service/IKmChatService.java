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
     * @return 消息列表
     */
    List<KmChatMessageVo> getHistory(Long sessionId);

    /**
     * 获取应用的所有会话
     *
     * @param appId 应用ID
     * @return 会话列表
     */
    List<Long> getSessionsByAppId(Long appId);

    /**
     * 获取应用下的会话列表
     *
     * @param appId 应用ID
     * @return 会话列表
     */
    List<KmChatSessionVo> getSessionList(Long appId);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    Boolean clearHistory(Long sessionId);

    /**
     * 清除应用下所有会话
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    Boolean clearAppHistory(Long appId);
}
