package org.dromara.ai.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.domain.KmChatSession;
import org.dromara.ai.app.domain.vo.AbortResponseVo;
import org.dromara.ai.app.domain.vo.KmChatSessionVo;
import org.dromara.ai.app.mapper.KmChatMessageMapper;
import org.dromara.ai.app.mapper.KmChatSessionMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天服务中断和恢复功能的 Mixin 类
 * 包含中断请求、恢复会话等相关方法的实现
 *
 * @author KMatrix AI Assistant
 * @date 2026-03-28
 */
@Slf4j
@Component
public class ChatServiceAbortMixin {

    private final ChatStreamHandler chatStreamHandler;
    private final KmChatMessageMapper kmChatMessageMapper;
    private final KmChatSessionMapper kmChatSessionMapper;

    public ChatServiceAbortMixin(ChatStreamHandler chatStreamHandler,
                                KmChatMessageMapper kmChatMessageMapper,
                                KmChatSessionMapper kmChatSessionMapper) {
        this.chatStreamHandler = chatStreamHandler;
        this.kmChatMessageMapper = kmChatMessageMapper;
        this.kmChatSessionMapper = kmChatSessionMapper;
    }

    /**
     * 中断请求
     *
     * @param requestId 请求ID
     * @param userId    用户ID
     * @return 中断响应
     */
    public AbortResponseVo abortRequest(String requestId, Long userId) {
        if (StringUtils.isBlank(requestId)) {
            throw new ServiceException("请求ID不能为空");
        }

        try {
            log.info("Starting abort for requestId={}, userId={}", requestId, userId);
            
            // 发送中断信号
            chatStreamHandler.abortStreamResponse(requestId);

            // 等待流处理完成（最多 100ms）
            Thread.sleep(100);

            // 获取已收集的部分内容
            String partialContent = chatStreamHandler.getCollectedContent(requestId);
            log.debug("Collected partial content for requestId={}: {} chars", requestId, 
                partialContent != null ? partialContent.length() : 0);

            // 更新数据库 - 更新消息
            KmChatMessage message = new KmChatMessage();
            message.setRequestId(requestId);
            message.setAbortStatus("aborted");
            message.setPartialContent(partialContent);
            message.setAbortTime(LocalDateTime.now());
            message.setAbortReason("user_abort");

            // 根据 requestId 更新消息
            int messageUpdateCount = kmChatMessageMapper.updateByRequestId(message);
            log.info("Updated {} messages for requestId={}", messageUpdateCount, requestId);

            // 更新会话 - 标记为可恢复
            // 需要从消息中获取 sessionId，然后更新会话
            KmChatMessage existingMessage = kmChatMessageMapper.selectOne(
                new LambdaQueryWrapper<KmChatMessage>()
                    .eq(KmChatMessage::getRequestId, requestId)
                    .last("LIMIT 1")
            );
            
            if (existingMessage != null && existingMessage.getSessionId() != null) {
                log.info("Found message with sessionId={} for requestId={}", existingMessage.getSessionId(), requestId);
                
                // 使用 updateWrapper 只更新特定字段，避免其他字段被设置为 NULL
                LambdaQueryWrapper<KmChatSession> updateWrapper = new LambdaQueryWrapper<KmChatSession>()
                    .eq(KmChatSession::getSessionId, existingMessage.getSessionId());
                
                KmChatSession sessionUpdate = new KmChatSession();
                sessionUpdate.setIsResumable("1");
                sessionUpdate.setAbortReason("user_abort");
                sessionUpdate.setAbortTimestamp(LocalDateTime.now());
                
                int sessionUpdateCount = kmChatSessionMapper.update(sessionUpdate, updateWrapper);
                log.info("Updated {} sessions (sessionId={}) to be resumable", sessionUpdateCount, existingMessage.getSessionId());
            } else {
                log.warn("Could not find message with sessionId for requestId={}", requestId);
            }

            log.info("Request {} aborted successfully by user {}", requestId, userId);

            return new AbortResponseVo(
                requestId,
                "aborted",
                partialContent,
                LocalDateTime.now(),
                "user_abort"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while aborting request {}", requestId, e);
            throw new ServiceException("中断请求失败");
        } catch (Exception e) {
            log.error("Error aborting request {}", requestId, e);
            throw new ServiceException("中断处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取可恢复的会话列表
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @return 可恢复的会话列表
     */
    public List<KmChatSessionVo> getResumableSessions(Long appId, Long userId) {
        try {
            log.info("Querying resumable sessions for appId={}, userId={}", appId, userId);
            
            // 查询该用户在该应用下的所有可恢复会话
            List<KmChatSession> sessions = kmChatSessionMapper.selectResumableSessions(appId, userId);
            
            log.info("Found {} resumable sessions for appId={}, userId={}", sessions.size(), appId, userId);
            for (KmChatSession session : sessions) {
                log.debug("Resumable session: sessionId={}, isResumable={}, abortReason={}, abortTimestamp={}", 
                    session.getSessionId(), session.getIsResumable(), session.getAbortReason(), session.getAbortTimestamp());
            }

            return sessions.stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting resumable sessions for app {} and user {}", appId, userId, e);
            throw new ServiceException("获取可恢复会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 恢复会话
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 恢复后的会话
     */
    public KmChatSessionVo resumeSession(Long sessionId, Long userId) {
        try {
            // 查询会话
            KmChatSession session = kmChatSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new ServiceException("会话不存在");
            }

            // 验证用户权限
            if (!session.getUserId().equals(userId)) {
                throw new ServiceException("无权限恢复此会话");
            }

            // 检查会话是否可恢复
            if (!"1".equals(session.getIsResumable())) {
                throw new ServiceException("此会话不可恢复");
            }

            // 生成恢复令牌
            String resumeToken = UUID.randomUUID().toString();

            // 更新会话状态
            session.setResumeToken(resumeToken);
            session.setResumedAt(LocalDateTime.now());
            session.setAbortReason(null);
            session.setAbortTimestamp(null);
            session.setAbortExceptionType(null);
            session.setAbortExceptionMessage(null);
            session.setAbortExceptionStacktrace(null);

            kmChatSessionMapper.updateById(session);

            log.info("Session {} resumed by user {}", sessionId, userId);

            return convertToVo(session);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error resuming session {} for user {}", sessionId, userId, e);
            throw new ServiceException("恢复会话失败");
        }
    }

    /**
     * 清除会话中断状态
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @return 是否成功
     */
    public Boolean clearAbortStatus(Long sessionId, Long userId) {
        try {
            // 查询会话
            KmChatSession session = kmChatSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new ServiceException("会话不存在");
            }

            // 验证用户权限
            if (!session.getUserId().equals(userId)) {
                throw new ServiceException("无权限清除此会话的中断状态");
            }

            // 清除中断状态
            session.setAbortReason(null);
            session.setAbortTimestamp(null);
            session.setAbortExceptionType(null);
            session.setAbortExceptionMessage(null);
            session.setAbortExceptionStacktrace(null);
            session.setIsResumable("0");
            session.setResumeToken(null);

            kmChatSessionMapper.updateById(session);

            log.info("Abort status cleared for session {} by user {}", sessionId, userId);

            return true;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error clearing abort status for session {} and user {}", sessionId, userId, e);
            throw new ServiceException("清除中断状态失败");
        }
    }

    /**
     * 将 KmChatSession 转换为 KmChatSessionVo
     *
     * @param session 会话实体
     * @return 会话视图对象
     */
    private KmChatSessionVo convertToVo(KmChatSession session) {
        KmChatSessionVo vo = new KmChatSessionVo();
        vo.setSessionId(session.getSessionId());
        vo.setAppId(session.getAppId());
        vo.setTitle(session.getTitle());
        vo.setUserId(session.getUserId());
        vo.setUserType(session.getUserType());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        vo.setAbortReason(session.getAbortReason());
        vo.setAbortTimestamp(session.getAbortTimestamp());
        vo.setIsResumable(session.getIsResumable());
        return vo;
    }
}
