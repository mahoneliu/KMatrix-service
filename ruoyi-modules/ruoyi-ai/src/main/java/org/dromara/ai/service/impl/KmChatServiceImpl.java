package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmApp;
import org.dromara.ai.domain.KmChatMessage;
import org.dromara.ai.domain.KmChatSession;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.KmNodeExecution;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.dromara.ai.domain.vo.KmNodeExecutionVo;
import org.dromara.ai.domain.vo.config.AppModelConfig;
import org.dromara.ai.domain.vo.config.AppSnapshot;
import org.dromara.ai.mapper.KmAppMapper;
import org.dromara.ai.mapper.KmChatMessageMapper;
import org.dromara.ai.mapper.KmChatSessionMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.mapper.KmNodeExecutionMapper;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.service.IKmChatService;
import org.dromara.ai.workflow.WorkflowExecutor;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final KmNodeExecutionMapper executionMapper;
    private final WorkflowExecutor workflowExecutor;
    private final ModelBuilder modelBuilder;
    private final KmAppMapper appMapper;

    private static final Long SSE_TIMEOUT = 5 * 60 * 1000L; // 5分钟

    /**
     * 获取会话历史消息
     */
    @Override
    public List<KmChatMessageVo> getHistory(Long sessionId, Long userId) {
        List<KmChatMessageVo> vos = messageMapper.selectVoList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByAsc(KmChatMessage::getCreateTime));

        // 填充节点执行记录
        for (KmChatMessageVo vo : vos) {
            if (vo.getInstanceId() != null) {
                List<KmNodeExecution> executions = executionMapper.selectList(
                        new LambdaQueryWrapper<KmNodeExecution>()
                                .eq(KmNodeExecution::getInstanceId, vo.getInstanceId())
                                .orderByAsc(KmNodeExecution::getStartTime));

                if (!executions.isEmpty()) {
                    List<KmNodeExecutionVo> executionVos = MapstructUtils.convert(executions, KmNodeExecutionVo.class);
                    // 尝试从工作流配置中恢复节点名称（暂简略处理，后续可优化为缓存或从DSL提取）
                    for (KmNodeExecutionVo execVo : executionVos) {
                        if (StrUtil.isBlank(execVo.getNodeName())) {
                            execVo.setNodeName(execVo.getNodeType() + " [" + execVo.getNodeId() + "]");
                        }
                    }
                    vo.setExecutions(executionVos);
                }
            }
        }

        return vos;
    }

    /**
     * 流式对话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter streamChat(KmChatSendBo bo) {
        // 获取当前用户ID (优先使用 BO 中的 userId，用于支持匿名调用)
        Long userIdTemp = bo.getUserId();
        if (userIdTemp == null) {
            userIdTemp = LoginHelper.getUserId();
        }
        final Long userId = userIdTemp;

        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 异步处理对话
        CompletableFuture.runAsync(() -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                // 1. 调试模式处理
                if (Boolean.TRUE.equals(bo.getDebug())) {
                    handleDebugChat(bo, emitter, userId);
                    return;
                }

                // 2. 加载应用和模型配置
                KmAppVo app = loadApp(bo.getAppId());

                // 3. 处理用户 ID (免登录模式使用应用创建者)
                Long tempUserId = userId;
                if (tempUserId == null) {
                    try {
                        tempUserId = Long.valueOf(app.getCreateBy());
                    } catch (Exception e) {
                        log.warn("无法从 createBy 获取用户 ID, appId={}", bo.getAppId());
                        throw new ServiceException("应用配置异常，无法识别所属用户");
                    }
                }
                final Long effectiveUserId = tempUserId;

                // 4. 获取或创建会话
                Long sessionId = getOrCreateSession(bo.getAppId(), bo.getSessionId(), effectiveUserId,
                        bo.getUserType());

                // 判断是否为新会话（首次对话）
                boolean isNewSession = (bo.getSessionId() == null);

                // 5. 检查应用类型
                if ("2".equals(app.getAppType())) {
                    // 工作流类型应用
                    log.info("使用工作流处理对话, appId={}, isNewSession={}", app.getAppId(), isNewSession);
                    try {
                        // 先执行工作流获取 instanceId
                        Map<String, Object> result = workflowExecutor.executeWorkflow(
                                app, sessionId, bo, emitter, userId);

                        String aiResponse = (String) result.get("finalResponse");
                        Long instanceId = (Long) result.get("instanceId");

                        // 保存用户消息（带 instanceId）
                        saveMessage(sessionId, "user", bo.getMessage(), instanceId, effectiveUserId);

                        // 保存AI响应
                        if (aiResponse != null) {
                            saveMessage(sessionId, "assistant", aiResponse, instanceId, effectiveUserId);
                        }

                        // 异步生成标题（仅在首次对话时）
                        if (isNewSession && aiResponse != null) {
                            KmModel model = loadModel(app.getModelId());
                            KmModelProvider provider = loadProvider(model.getProviderId());
                            CompletableFuture.runAsync(() -> {
                                try {
                                    generateSessionTitle(sessionId, bo.getMessage(), aiResponse, model,
                                            provider.getProviderKey());
                                } catch (Exception e) {
                                    log.warn("异步生成工作流标题失败", e);
                                }
                            });
                        }

                        // 工作流完成（executeWorkflow内部已发送done事件）
                        emitter.complete();

                    } catch (Exception e) {
                        log.error("工作流执行失败", e);
                        emitter.completeWithError(e);
                    }
                    return;
                }

                // 基础对话类型 - 先保存用户消息
                saveMessage(sessionId, "user", bo.getMessage(), userId);
                KmModel model = loadModel(app.getModelId());
                KmModelProvider provider = loadProvider(model.getProviderId());

                // 4. 构建对话上下文
                List<ChatMessage> messages = buildChatMessages(sessionId, app.getModelSetting(), bo.getMessage());

                // 5. 构建流式模型并生成响应
                StreamingChatLanguageModel streamingModel = modelBuilder.buildStreamingChatModel(model,
                        provider.getProviderKey());

                // 使用Token级流式处理
                streamingModel.generate(messages,
                        new StreamingResponseHandler<AiMessage>() {
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
                                        saveMessage(sessionId, "assistant", aiResponse, effectiveUserId);
                                    }

                                    // 记录token使用情况
                                    TokenUsage tokenUsage = response.tokenUsage();
                                    if (tokenUsage != null) {
                                        log.info("Token使用: input={}, output={}, total={}",
                                                tokenUsage.inputTokenCount(),
                                                tokenUsage.outputTokenCount(),
                                                tokenUsage.totalTokenCount());
                                    }

                                    // 异步生成会话标题(仅在首次对话时)
                                    CompletableFuture.runAsync(() -> {
                                        try {
                                            // 检查是否是首次对话(消息数量为2:一问一答)
                                            long messageCount = messageMapper.selectCount(
                                                    new LambdaQueryWrapper<KmChatMessage>()
                                                            .eq(KmChatMessage::getSessionId, sessionId));
                                            if (messageCount == 2) {
                                                generateSessionTitle(sessionId, bo.getMessage(), aiResponse, model,
                                                        provider.getProviderKey());
                                            }
                                        } catch (Exception e) {
                                            log.warn("异步生成标题失败", e);
                                        }
                                    });

                                    // 发送完成信号,携带sessionId供前端保存
                                    emitter.send(SseEmitter.event().name("done").data(sessionId.toString()));
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

        Long userId = bo.getUserId();
        if (userId == null) {
            userId = LoginHelper.getUserId();
        }

        // 2. 获取或创建会话
        Long sessionId = getOrCreateSession(bo.getAppId(), bo.getSessionId(), userId, bo.getUserType());

        // 3. 保存用户消息
        saveMessage(sessionId, "user", bo.getMessage(), userId);

        // 4. 构建对话上下文
        List<ChatMessage> messages = buildChatMessages(sessionId, app.getModelSetting(), bo.getMessage());

        // 5. 构建模型并生成响应
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());
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
        saveMessage(sessionId, "assistant", aiResponse, userId);

        return aiResponse;
    }

    /**
     * 获取应用的所有会话
     */
    @Override
    public List<Long> getSessionsByAppId(Long appId, Long userId) {
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
    public Boolean clearHistory(Long sessionId, Long userId) {
        // 验证所有权
        KmChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        if (!session.getUserId().equals(userId)) {
            throw new ServiceException("无权限操作此会话");
        }
        // 删除会话消息
        messageMapper.delete(new LambdaQueryWrapper<KmChatMessage>()
                .eq(KmChatMessage::getSessionId, sessionId));

        // 软删除会话
        // 软删除会话
        session.setDelFlag("1");
        return sessionMapper.updateById(session) > 0;
    }

    /**
     * 获取应用下的会话列表
     */
    @Override
    public List<KmChatSessionVo> getSessionList(Long appId, Long userId) {
        List<KmChatSessionVo> sessions = sessionMapper.selectVoList(
                new LambdaQueryWrapper<KmChatSession>()
                        .eq(KmChatSession::getAppId, appId)
                        .eq(KmChatSession::getUserId, userId)
                        .eq(KmChatSession::getDelFlag, "0")
                        .last("limit 20")
                        .orderByDesc(KmChatSession::getCreateTime));
        return sessions;
    }

    /**
     * 清除应用下所有会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean clearAppHistory(Long appId, Long userId) {

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

        // 如果是工作流类型应用,从最新发布版本加载 DSL
        if ("2".equals(app.getAppType())) {
            // 检查应用发布状态
            if (!"1".equals(app.getStatus())) {
                throw new ServiceException("该应用尚未发布,请先在工作流编辑器中发布后再使用");
            }

            AppSnapshot publishedSnapshot = appService.getLatestPublishedSnapshot(appId);
            if (publishedSnapshot != null && publishedSnapshot.getDslData() != null) {
                // 使用发布版本的 DSL
                app.setDslData(publishedSnapshot.getDslData());
                app.setGraphData(publishedSnapshot.getGraphData());
                log.info("对话加载工作流: appId={}, 使用最新发布版本", appId);
            } else {
                // 没有发布版本
                throw new ServiceException("该应用尚未发布,请先在工作流编辑器中发布后再使用");
            }
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
        Collections.reverse(historyMessages);

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
    private Long getOrCreateSession(Long appId, Long sessionId, Long userId, String userType) {
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
        newSession.setUserType(userType);
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
        saveMessage(sessionId, role, content, null, userId);
    }

    /**
     * 保存带有进度实例的消息
     */
    private void saveMessage(Long sessionId, String role, String content, Long instanceId, Long userId) {
        KmChatMessage message = new KmChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setInstanceId(instanceId);
        message.setCreateTime(new Date());

        // 手动设置BaseEntity字段
        message.setCreateBy(userId);
        message.setUpdateBy(userId);
        message.setUpdateTime(new Date());

        messageMapper.insert(message);
    }

    /**
     * 生成会话标题
     */
    private void generateSessionTitle(Long sessionId, String userMessage, String aiResponse,
            KmModel model, String providerKey) {
        try {
            // 构建标题生成prompt
            String titlePrompt = String.format(
                    "请根据以下对话生成一个简洁的标题(5-15个字),只返回标题内容,不要其他解释:\n\n" +
                            "用户: %s\n" +
                            "助手: %s\n\n" +
                            "标题:",
                    userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage,
                    aiResponse.length() > 100 ? aiResponse.substring(0, 100) + "..." : aiResponse);

            // 构建简单的消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new UserMessage(titlePrompt));

            // 使用同步模型快速生成标题
            ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, providerKey);
            Response<AiMessage> response = chatModel.generate(messages);
            String title = response.content().text().trim();

            // 清理标题(去除引号等)
            title = title.replaceAll("^\"|\"$", "")
                    .replaceAll("^'|'$", "")
                    .replaceAll("^《|》$", "")
                    .trim();

            // 限制标题长度
            if (title.length() > 30) {
                title = title.substring(0, 30);
            }

            // 更新session标题
            KmChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && "新会话".equals(session.getTitle())) {
                session.setTitle(title);
                session.setUpdateTime(new Date());
                sessionMapper.updateById(session);
                log.info("会话标题已更新: sessionId={}, title={}", sessionId, title);
            }
        } catch (Exception e) {
            log.warn("生成会话标题失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /**
     * 更新会话标题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSessionTitle(Long sessionId, String title, Long userId) {
        KmChatSession session = sessionMapper.selectById(sessionId);

        if (session == null) {
            throw new ServiceException("会话不存在");
        }

        // 验证权限:只能修改自己的会话
        if (!session.getUserId().equals(userId)) {
            throw new ServiceException("无权限修改此会话");
        }

        session.setTitle(title);
        session.setUpdateTime(new Date());
        session.setUpdateBy(userId);
        return sessionMapper.updateById(session) > 0;
    }

    /**
     * 获取会话的执行详情
     */
    @Override
    public List<KmNodeExecutionVo> getExecutionDetails(Long sessionId, Long userId) {
        // 验证会话存在及权限
        KmChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Collections.emptyList();
        }
        // 如果不是同一个用户，拒绝访问
        if (!session.getUserId().equals(userId)) {
            return Collections.emptyList();
        }
        // 1. 查询会话的所有消息
        List<KmChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .isNotNull(KmChatMessage::getInstanceId));

        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取instanceId列表
        List<Long> instanceIds = messages.stream()
                .map(KmChatMessage::getInstanceId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查询执行记录
        List<KmNodeExecution> executions = executionMapper.selectList(
                new LambdaQueryWrapper<KmNodeExecution>()
                        .in(KmNodeExecution::getInstanceId, instanceIds)
                        .orderByAsc(KmNodeExecution::getStartTime));

        return MapstructUtils.convert(executions, KmNodeExecutionVo.class);
    }

    /**
     * 处理调试模式对话
     * 完全不写数据库：不创建session、不保存message、不创建instance、不保存execution
     * 每次对话都实时获取最新的草稿DSL，支持调试过程中动态修改工作流
     */
    private void handleDebugChat(KmChatSendBo bo, SseEmitter emitter, Long userId) {
        try {
            // 1. 直接查询数据库获取最新草稿（重要：不使用queryById，因为它返回发布版本）
            // 直接从数据库获取应用记录，使用dslData和graphData字段（草稿）
            KmApp appEntity = appMapper.selectById(bo.getAppId());
            if (appEntity == null) {
                throw new ServiceException("应用不存在");
            }

            // 2. 校验草稿DSL是否存在
            if (StrUtil.isBlank(appEntity.getDslData())) {
                throw new ServiceException("工作流草稿为空，请先在编辑器中配置工作流");
            }

            log.info("调试模式: appId={}, 使用草稿数据(dslData字段)", bo.getAppId());

            // 3. 转换为Vo（Mapstruct会自动复制所有字段包括dslData和graphData）
            KmAppVo debugApp = MapstructUtils.convert(appEntity, KmAppVo.class);

            // 4. 使用虚拟会话ID（不写库，完全内存处理）
            Long debugSessionId = -1L; // 负数表示调试会话，不会创建session记录

            // 执行调试工作流（使用草稿数据，不保存任何记录）
            workflowExecutor.executeWorkflowDebug(debugApp, debugSessionId, bo, emitter, userId);

            // 工作流完成（executeWorkflowDebug内部已发送done事件，与streamChat行为一致）
            emitter.complete();

        } catch (Exception e) {
            log.error("调试对话失败", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                emitter.completeWithError(e);
            } catch (Exception ex) {
                log.error("发送调试错误事件失败", ex);
            }
        }
    }
}
