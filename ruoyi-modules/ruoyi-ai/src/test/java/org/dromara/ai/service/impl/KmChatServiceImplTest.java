package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.domain.KmApp;
import org.dromara.ai.domain.KmChatMessage;
import org.dromara.ai.domain.KmChatSession;
import org.dromara.ai.mapper.KmAppMapper;
import org.dromara.ai.mapper.KmChatMessageMapper;
import org.dromara.ai.mapper.KmChatSessionMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.mapper.KmNodeExecutionMapper;
import org.dromara.ai.service.IChatRateLimitService;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.workflow.WorkflowExecutor;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KmChatServiceImplTest extends BaseUnitTest {

    @Mock
    private KmChatSessionMapper sessionMapper;
    @Mock
    private KmChatMessageMapper messageMapper;
    @Mock
    private KmModelMapper modelMapper;
    @Mock
    private KmModelProviderMapper providerMapper;
    @Mock
    private IKmAppService appService;
    @Mock
    private KmNodeExecutionMapper executionMapper;
    @Mock
    private WorkflowExecutor workflowExecutor;
    @Mock
    private ModelBuilder modelBuilder;
    @Mock
    private KmAppMapper appMapper;
    @Mock
    private IChatRateLimitService rateLimitService;

    private KmChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatService = new KmChatServiceImpl(
                sessionMapper, messageMapper, modelMapper, providerMapper,
                appService, executionMapper, workflowExecutor, modelBuilder,
                appMapper, rateLimitService
        );
    }

    @Test
    void submitFeedback_Success_NewLike() {
        Long messageId = 1L;
        Long userId = 100L;
        Long sessionId = 10L;
        Long appId = 5L;

        KmChatMessage message = new KmChatMessage();
        message.setMessageId(messageId);
        message.setSessionId(sessionId);
        message.setFeedbackStatus(0); // 初始没有评价

        KmChatSession session = new KmChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setAppId(appId);

        when(messageMapper.selectById(messageId)).thenReturn(message);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.updateById(any(KmChatMessage.class))).thenReturn(1);
        when(appMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Boolean result = chatService.submitFeedback(messageId, 1, userId);

        assertTrue(result);
        assertEquals(1, message.getFeedbackStatus());
        verify(messageMapper, times(1)).updateById(message);
        verify(appMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void submitFeedback_Success_ChangeLikeToDislike() {
        Long messageId = 1L;
        Long userId = 100L;
        Long sessionId = 10L;
        Long appId = 5L;

        KmChatMessage message = new KmChatMessage();
        message.setMessageId(messageId);
        message.setSessionId(sessionId);
        message.setFeedbackStatus(1); // 初始为赞

        KmChatSession session = new KmChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setAppId(appId);

        when(messageMapper.selectById(messageId)).thenReturn(message);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.updateById(any(KmChatMessage.class))).thenReturn(1);
        when(appMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Boolean result = chatService.submitFeedback(messageId, -1, userId);

        assertTrue(result);
        assertEquals(-1, message.getFeedbackStatus());
        verify(messageMapper, times(1)).updateById(message);
        verify(appMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void submitFeedback_Fail_InvalidStatus() {
        Long messageId = 1L;
        Long userId = 100L;

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            chatService.submitFeedback(messageId, 2, userId);
        });

        assertEquals("无效的评价状态", exception.getMessage());
    }

    @Test
    void submitFeedback_Fail_NotOwner() {
        Long messageId = 1L;
        Long userId = 100L;
        Long sessionId = 10L;

        KmChatMessage message = new KmChatMessage();
        message.setMessageId(messageId);
        message.setSessionId(sessionId);

        KmChatSession session = new KmChatSession();
        session.setSessionId(sessionId);
        session.setUserId(200L); // 不同的用户

        when(messageMapper.selectById(messageId)).thenReturn(message);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            chatService.submitFeedback(messageId, 1, userId);
        });

        assertEquals("无权限评价此消息", exception.getMessage());
    }
}
