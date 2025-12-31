package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmChatMessage;
import org.dromara.ai.domain.KmChatSession;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.dromara.ai.domain.vo.config.AppModelConfig;
import org.dromara.ai.mapper.KmChatMessageMapper;
import org.dromara.ai.mapper.KmChatSessionMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.service.IKmChatService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI聊天Service业务层处理
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmChatServiceImpl implements IKmChatService {

    private final KmChatSessionMapper sessionMapper;
    private final KmChatMessageMapper messageMapper;
    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final IKmAppService appService;

    private static final Long SSE_TIMEOUT = 5 * 60 * 1000L; // 5分钟

    /**
     * 流式对话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter streamChat(KmChatSendBo bo) {
        // 获取当前用户ID (必须在主线程获取，异步线程无法获取ThreadLocal)
        Long userId = LoginHelper.getUserId();

        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 异步处理对话
        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                // 1. 加载应用和模型配置
                KmAppVo app = loadApp(bo.getAppId());
                KmModel model = loadModel(app.getModelId());
                KmModelProvider provider = loadProvider(model.getProviderId());

                // 2. 获取或创建会话
                Long sessionId = getOrCreateSession(bo.getAppId(), bo.getSessionId(), userId);

                // 3. 保存用户消息
                saveMessage(sessionId, "user", bo.getMessage(), userId);

                // 4. 构建对话上下文
                List<ChatMessage> messages = buildChatMessages(sessionId, app.getModelSetting(), bo.getMessage());

                // 5. 构建流式模型并生成响应
                StreamingChatLanguageModel streamingModel = ModelBuilder.buildStreamingChatModel(model,
                        provider.getProviderKey());

                // 使用Token级流式处理
                streamingModel.generate(messages,
                        new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                            @Override
                            public void onNext(String token) {
                                try {
                                    fullResponse.append(token);
                                    emitter.send(SseEmitter.event().data(token));
                                } catch (IOException e) {
                                    log.error("发送SSE数据失败", e);
                                }
                            }

                            @Override
                            public void onComplete(Response<AiMessage> response) {
                                try {
                                    // 保存AI响应
                                    String aiResponse = fullResponse.toString();
                                    if (StrUtil.isNotBlank(aiResponse)) {
                                        saveMessage(sessionId, "assistant", aiResponse, userId);
                                    }

                                    // 记录token使用情况
                                    TokenUsage tokenUsage = response.tokenUsage();
                                    if (tokenUsage != null) {
                                        log.info("Token使用: input={}, output={}, total={}",
                                                tokenUsage.inputTokenCount(),
                                                tokenUsage.outputTokenCount(),
                                                tokenUsage.totalTokenCount());
                                    }

                                    // 发送完成信号
                                    emitter.send(SseEmitter.event().name("done").data(""));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("完成SSE流失败", e);
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("AI生成失败", error);
                                try {
                                    emitter.send(
                                            SseEmitter.event().name("error").data("AI生成失败: " + error.getMessage()));
                                } catch (IOException e) {
                                    log.error("发送错误消息失败", e);
                                }
                                emitter.completeWithError(error);
                            }
                        });

            } catch (Exception e) {
                log.error("流式对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("对话失败: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 普通对话(非流式)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String chat(KmChatSendBo bo) {
        // 1. 加载应用和模型配置
        KmAppVo app = loadApp(bo.getAppId());
        KmModel model = loadModel(app.getModelId());
        KmModelProvider provider = loadProvider(model.getProviderId());
        Long userId = LoginHelper.getUserId();

        // 2. 获取或创建会话
        Long sessionId = getOrCreateSession(bo.getAppId(), bo.getSessionId(), userId);

        // 3. 保存用户消息
        saveMessage(sessionId, "user", bo.getMessage(), userId);

        // 4. 构建对话上下文
        List<ChatMessage> messages = buildChatMessages(sessionId, app.getModelSetting(), bo.getMessage());

        // 5. 构建模型并生成响应
        ChatLanguageModel chatModel = ModelBuilder.buildChatModel(model, provider.getProviderKey());
        Response<AiMessage> response = chatModel.generate(messages);

        // 6. 获取AI响应
        String aiResponse = response.content().text();

        // 7. 记录token使用情况
        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            log.info("Token使用: input={}, output={}, total={}",
                    tokenUsage.inputTokenCount(),
                    tokenUsage.outputTokenCount(),
                    tokenUsage.totalTokenCount());
        }

        // 8. 保存AI响应
        saveMessage(sessionId, "assistant", aiResponse, LoginHelper.getUserId());

        return aiResponse;
    }

    /**
     * 获取会话历史消息
     */
    @Override
    public List<KmChatMessageVo> getHistory(Long sessionId) {
        List<KmChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByAsc(KmChatMessage::getCreateTime));
        return MapstructUtils.convert(messages, KmChatMessageVo.class);
    }

    /**
     * 获取应用的所有会话
     */
    @Override
    public List<Long> getSessionsByAppId(Long appId) {
        Long userId = LoginHelper.getUserId();
        List<KmChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<KmChatSession>()
                        .eq(KmChatSession::getAppId, appId)
                        .eq(KmChatSession::getUserId, userId)
                        .eq(KmChatSession::getDelFlag, "0")
                        .orderByDesc(KmChatSession::getCreateTime));
        return sessions.stream().map(KmChatSession::getSessionId).collect(Collectors.toList());
    }

    /**
     * 清除会话历史
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clearHistory(Long sessionId) {
        // 删除会话消息
        messageMapper.delete(new LambdaQueryWrapper<KmChatMessage>()
                .eq(KmChatMessage::getSessionId, sessionId));

        // 软删除会话
        KmChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setDelFlag("1");
            return sessionMapper.updateById(session) > 0;
        }
        return false;
    }

    /**
     * 获取应用下的会话列表
     */
    @Override
    public List<KmChatSessionVo> getSessionList(Long appId) {
        Long userId = LoginHelper.getUserId();
        List<KmChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<KmChatSession>()
                        .eq(KmChatSession::getAppId, appId)
                        .eq(KmChatSession::getUserId, userId)
                        .eq(KmChatSession::getDelFlag, "0")
                        .orderByDesc(KmChatSession::getCreateTime));
        return MapstructUtils.convert(sessions, KmChatSessionVo.class);
    }

    /**
     * 清除应用下所有会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clearAppHistory(Long appId) {
        Long userId = LoginHelper.getUserId();

        // 查询用户在该应用下的所有会话
        List<KmChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<KmChatSession>()
                        .eq(KmChatSession::getAppId, appId)
                        .eq(KmChatSession::getUserId, userId)
                        .eq(KmChatSession::getDelFlag, "0"));

        if (sessions.isEmpty()) {
            return true;
        }

        List<Long> sessionIds = sessions.stream()
                .map(KmChatSession::getSessionId)
                .collect(Collectors.toList());

        // 删除所有消息
        messageMapper.delete(new LambdaQueryWrapper<KmChatMessage>()
                .in(KmChatMessage::getSessionId, sessionIds));

        // 软删除所有会话
        for (KmChatSession session : sessions) {
            session.setDelFlag("1");
        }
        return sessionMapper.updateBatchById(sessions);
    }

    /**
     * 加载应用配置
     */
    private KmAppVo loadApp(Long appId) {
        KmAppVo app = appService.queryById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        if (app.getModelId() == null) {
            throw new ServiceException("应用未配置模型");
        }
        return app;
    }

    /**
     * 加载模型配置
     */
    private KmModel loadModel(Long modelId) {
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new ServiceException("模型不存在");
        }
        if (!"0".equals(model.getStatus())) {
            throw new ServiceException("模型已停用");
        }
        return model;
    }

    /**
     * 加载供应商配置
     */
    private KmModelProvider loadProvider(Long providerId) {
        KmModelProvider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new ServiceException("模型供应商不存在");
        }
        return provider;
    }

    /**
     * 构建对话消息上下文
     */
    private List<ChatMessage> buildChatMessages(Long sessionId, AppModelConfig modelConfig, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. 添加系统提示词
        if (modelConfig != null && StrUtil.isNotBlank(modelConfig.getSystemPrompt())) {
            messages.add(new SystemMessage(modelConfig.getSystemPrompt()));
        }

        // 2. 加载历史消息(最近20条)
        List<KmChatMessage> historyMessages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByDesc(KmChatMessage::getCreateTime)
                        .last("LIMIT 20"));

        // 反转为时间正序
        java.util.Collections.reverse(historyMessages);

        // 转换为LangChain4j消息
        for (KmChatMessage msg : historyMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AiMessage(msg.getContent()));
            }
        }

        return messages;
    }

    /**
     * 获取或创建会话
     */
    private Long getOrCreateSession(Long appId, Long sessionId, Long userId) {
        if (sessionId != null) {
            KmChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && "0".equals(session.getDelFlag())) {
                return sessionId;
            }
        }

        // 创建新会话
        KmChatSession newSession = new KmChatSession();
        newSession.setAppId(appId);
        newSession.setUserId(userId);
        newSession.setTitle("新会话");
        newSession.setCreateTime(new Date());
        newSession.setDelFlag("0");

        // 手动设置BaseEntity字段
        newSession.setCreateBy(userId);
        newSession.setUpdateBy(userId);
        newSession.setUpdateTime(new Date());

        sessionMapper.insert(newSession);
        return newSession.getSessionId();
    }

    /**
     * 保存消息
     */
    private void saveMessage(Long sessionId, String role, String content, Long userId) {
        KmChatMessage message = new KmChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(new Date());

        // 手动设置BaseEntity字段
        message.setCreateBy(userId);
        message.setUpdateBy(userId);
        message.setUpdateTime(new Date());

        messageMapper.insert(message);
    }
}
