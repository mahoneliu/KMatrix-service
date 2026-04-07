package org.dromara.ai.app.service.impl;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.dromara.ai.api.enums.SseEventType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.domain.KmChatMessage;
import org.dromara.ai.app.domain.KmChatSession;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.domain.vo.KmModelVo;
import org.dromara.ai.workflow.domain.KmNodeExecution;
import org.dromara.ai.workflow.domain.bo.WorkflowExecutionReq;
import org.dromara.ai.app.domain.bo.KmChatSendBo;
import org.dromara.ai.api.enums.AiAppType;
import org.dromara.ai.app.domain.vo.KmAppVo;
import org.dromara.ai.app.domain.vo.KmChatMessageVo;
import org.dromara.ai.app.domain.vo.KmChatSessionVo;
import org.dromara.ai.workflow.domain.vo.KmNodeExecutionVo;
import org.dromara.ai.api.domain.vo.config.AppModelConfig;
import org.dromara.ai.app.domain.vo.config.AppSnapshot;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.app.mapper.KmChatMessageMapper;
import org.dromara.ai.app.mapper.KmChatSessionMapper;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.service.IKmModelService;
import org.dromara.ai.workflow.mapper.KmNodeExecutionMapper;
import org.dromara.ai.app.service.IKmAppService;
import org.dromara.ai.app.service.IKmChatService;
import org.dromara.ai.workflow.workflow.WorkflowExecutor;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.ai.app.service.IChatRateLimitService;
import org.dromara.ai.app.service.ChatServiceAbortMixin;
import org.dromara.ai.app.service.ChatStreamHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import cn.hutool.json.JSONUtil;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.service.IKmFileService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
    private final IChatRateLimitService rateLimitService;
    private final IKmFileService kmFileService;
    private final IKmModelService modelService;
    private final ChatServiceAbortMixin abortMixin;
    private final ChatStreamHandler chatStreamHandler;

    private static final Long SSE_TIMEOUT = 5 * 60 * 1000L; // 5分钟

    /**
     * 获取会话历史消息
     */
    @Override
    public List<KmChatMessageVo> getHistory(Long sessionId, Long userId, Boolean includeExecutions) {
        List<KmChatMessageVo> vos = messageMapper.selectVoList(
                new LambdaQueryWrapper<KmChatMessage>()
                        .eq(KmChatMessage::getSessionId, sessionId)
                        .orderByAsc(KmChatMessage::getCreateTime));

        // 仅在请求包含执行详情时查询
        if (Boolean.TRUE.equals(includeExecutions)) {
            // 填充节点执行记录
            for (KmChatMessageVo vo : vos) {
                if (vo.getInstanceId() != null) {
                    List<KmNodeExecution> executions = executionMapper.selectList(
                            new LambdaQueryWrapper<KmNodeExecution>()
                                    .eq(KmNodeExecution::getInstanceId, vo.getInstanceId())
                                    .orderByAsc(KmNodeExecution::getStartTime));

                    if (!executions.isEmpty()) {
                        List<KmNodeExecutionVo> executionVos = MapstructUtils.convert(executions,
                                KmNodeExecutionVo.class);
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

                // 4. 限流校验
                rateLimitService.checkRequestLimit(effectiveUserId.toString());
                rateLimitService.checkTokenLimit(effectiveUserId.toString());

                // 5. 获取或创建会话
                Long sessionId = getOrCreateSession(bo.getAppId(), bo.getSessionId(), effectiveUserId,
                        bo.getUserType());

                // 判断是否为新会话（首次对话）
                boolean isNewSession = (bo.getSessionId() == null);

                // 记录请求状态（用于中止请求时获取 sessionId）
                if (bo.getRequestId() != null) {
                    log.info("Recording request state: requestId={}, sessionId={}", bo.getRequestId(), sessionId);
                    chatStreamHandler.startStreamResponse(bo.getRequestId(), sessionId);
                } else {
                    log.warn("RequestId is null in streamChat, cannot record request state");
                }

                // 5. 检查应用类型
                log.info("开始处理流式对话: appId={}, appType={}", app.getAppId(), app.getAppType());

                if (AiAppType.CUSTOM_WORKFLOW.getCode().equals(app.getAppType())
                        || AiAppType.FIXED_TEMPLATE.getCode().equals(app.getAppType())) {
                    // 工作流或固定模板类型应用
                    log.info("使用工作流处理对话, appId={}, appType={}, isNewSession={}", app.getAppId(), app.getAppType(),
                            isNewSession);
                    try {
                        // 先执行工作流获取 instanceId
                        WorkflowExecutionReq req = WorkflowExecutionReq.builder()
                                .appId(app.getAppId())
                                .dslData(app.getDslData())
                                .enableExecutionDetail(app.getEnableExecutionDetail())
                                .showExecutionInfo(bo.getShowExecutionInfo())
                                .message(buildMultimodalMessage(bo.getMessage(), bo.getTempFileIds()))
                                .sessionId(sessionId)
                                .userId(userId)
                                .tempFileIds(bo.getTempFileIds())
                                .build();
                        Map<String, Object> result = workflowExecutor.executeWorkflow(req, emitter);

                        String aiResponse = (String) result.get("finalResponse");
                        Long instanceId = (Long) result.get("instanceId");
                        Integer totalTokens = (Integer) result.get("totalTokens");

                        // 记录 Token 消耗
                        if (totalTokens != null && totalTokens > 0) {
                            rateLimitService.recordTokenUsage(effectiveUserId.toString(), totalTokens);
                        }

                        // 保存用户消息（带 instanceId）
                        saveMessage(sessionId, "user", bo.getMessage(), instanceId, null, effectiveUserId, bo.getRequestId());

                        // 保存AI响应
                        if (aiResponse != null) {
                            KmChatMessage assistantMessage = saveMessage(sessionId, "assistant", aiResponse, instanceId,
                                    totalTokens, effectiveUserId, bo.getRequestId());
                            // 发送消息ID给前端，以便进行评价
                            sendSseEvent(emitter, SseEventType.DONE,
                                    Map.of("messageId", assistantMessage.getMessageId().toString()));
                        }

                        // 异步生成标题（仅在首次对话时）
                        if (isNewSession && aiResponse != null) {
                            try {
                                KmModel model = null;
                                String providerKey = null;
                                if (app.getModelId() != null) {
                                    model = loadModel(app.getModelId());
                                    KmModelProvider provider = loadProvider(model.getProviderId());
                                    providerKey = provider.getProviderKey();
                                }
                                generateSessionTitle(sessionId, bo.getMessage(), aiResponse, model,
                                        providerKey, emitter);
                            } catch (Exception e) {
                                log.warn("生成工作流标题失败", e);
                            }
                        }

                        // 工作流完成
                        emitter.complete();

                    } catch (Exception e) {
                        log.error("工作流执行失败", e);
                        emitter.completeWithError(e);
                    }
                    return;
                } else {
                    log.warn("不支持的应用类型: {}", app.getAppType());
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data("暂不支持该应用类型: " + app.getAppType()));
                    } catch (IOException e) {
                        log.error("发送错误消息失败", e);
                    }
                    emitter.complete();
                    return;
                }

                // 基础对话类型 - 先保存用户消息
                // saveMessage(sessionId, "user", bo.getMessage(), userId);
                // ... (rest of the commented out code)

            } catch (Exception e) {
                log.error("流式对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(MessageUtils.message("ai.msg.chat.failed") + ": " + e.getMessage()));
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
    public KmChatMessageVo chat(KmChatSendBo bo) {
        // 1. 加载应用和模型配置
        KmAppVo app = loadApp(bo.getAppId());
        KmModel model = loadModel(app.getModelId());
        KmModelProvider provider = loadProvider(model.getProviderId());

        Long userId = bo.getUserId();
        if (userId == null) {
            userId = LoginHelper.getUserId();
        }

        // 2. 限流校验
        rateLimitService.checkRequestLimit(userId.toString());
        rateLimitService.checkTokenLimit(userId.toString());

        // 3. 获取或创建会话
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

            // 记录 Token 消耗
            rateLimitService.recordTokenUsage(userId.toString(), tokenUsage.totalTokenCount());
        }

        // 8. 保存AI响应
        Integer totalTokenCount = tokenUsage != null ? tokenUsage.totalTokenCount() : null;
        KmChatMessage assistantMessage = saveMessage(sessionId, "assistant", aiResponse, null, totalTokenCount, userId);

        return MapstructUtils.convert(assistantMessage, KmChatMessageVo.class);
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
            throw new ServiceException(MessageUtils.message("ai.msg.app.not_found"));
        }

        // 如果是工作流类型或固定模板应用,从最新发布版本加载 DSL
        if (AiAppType.CUSTOM_WORKFLOW.getCode().equals(app.getAppType())
                || AiAppType.FIXED_TEMPLATE.getCode().equals(app.getAppType())) {
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
            throw new ServiceException(MessageUtils.message("ai.msg.model.not_found"));
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
    private KmChatMessage saveMessage(Long sessionId, String role, String content, Long userId) {
        return saveMessage(sessionId, role, content, null, null, userId, null);
    }

    /**
     * 保存带有进度实例和 Token 使用情况的消息
     */
    private KmChatMessage saveMessage(Long sessionId, String role, String content, Long instanceId, Integer totalTokens,
            Long userId) {
        return saveMessage(sessionId, role, content, instanceId, totalTokens, userId, null);
    }

    /**
     * 保存带有进度实例、Token 使用情况和请求ID的消息
     */
    private KmChatMessage saveMessage(Long sessionId, String role, String content, Long instanceId, Integer totalTokens,
            Long userId, String requestId) {
        KmChatMessage message = new KmChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setInstanceId(instanceId);
        message.setTotalTokens(totalTokens);
        message.setRequestId(requestId);
        message.setCreateTime(new Date());

        // 手动设置BaseEntity字段
        message.setCreateBy(userId);
        message.setUpdateBy(userId);
        message.setUpdateTime(new Date());

        messageMapper.insert(message);

        // 更新统计数据
        if ("user".equals(role)) {
            appService.updateAccessStat(sessionMapper.selectById(sessionId).getAppId(), userId, 0L, 0, 0, 1);
        } else if ("assistant".equals(role) && totalTokens != null && totalTokens > 0) {
            appService.updateAccessStat(sessionMapper.selectById(sessionId).getAppId(), userId, totalTokens.longValue(),
                    0, 0, 0);
        }

        return message;
    }

    /**
     * 生成会话标题
     */
    private void generateSessionTitle(Long sessionId, String userMessage, String aiResponse,
            KmModel model, String providerKey, SseEmitter emitter) {
        try {
            // 如果应用未绑定模型，采用系统默认模型
            if (model == null) {
                KmModelVo defaultModelVo = modelService.getDefaultModel("1"); // 1 为 LLM
                if (defaultModelVo == null) {
                    log.warn("无法生成会话标题：未配置应用模型且系统无默认模型, sessionId={}", sessionId);
                    return;
                }
                model = MapstructUtils.convert(defaultModelVo, KmModel.class);
                KmModelProvider provider = loadProvider(model.getProviderId());
                providerKey = provider.getProviderKey();
            }
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

                // 发送SSE事件通知前端更新
                if (emitter != null) {
                    try {
                        KmChatSessionVo sessionVo = MapstructUtils.convert(session, KmChatSessionVo.class);
                        emitter.send(SseEmitter.event()
                                .name(SseEventType.SESSION_UPDATE.getEventName())
                                .data(sessionVo));
                    } catch (Exception e) {
                        log.warn("发送会话更新事件失败", e);
                    }
                }
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
     * 提交消息评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitFeedback(Long messageId, Integer feedbackStatus, Long userId) {
        if (feedbackStatus == null || (feedbackStatus != 0 && feedbackStatus != 1 && feedbackStatus != -1)) {
            throw new ServiceException("无效的评价状态");
        }

        KmChatMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException("消息不存在");
        }

        KmChatSession session = sessionMapper.selectById(message.getSessionId());
        if (session == null || !session.getUserId().equals(userId)) {
            throw new ServiceException("无权限评价此消息");
        }

        Integer oldStatus = message.getFeedbackStatus() == null ? 0 : message.getFeedbackStatus();
        if (oldStatus.equals(feedbackStatus)) {
            return true;
        }

        // 更新消息评价状态
        message.setFeedbackStatus(feedbackStatus);
        message.setUpdateTime(new Date());
        message.setUpdateBy(userId);
        messageMapper.updateById(message);

        // 安全更新应用的计数字段
        Long appId = session.getAppId();
        if (appId != null) {
            int likeDiff = 0;
            int dislikeDiff = 0;

            if (oldStatus == 1)
                likeDiff -= 1;
            else if (oldStatus == -1)
                dislikeDiff -= 1;

            if (feedbackStatus == 1)
                likeDiff += 1;
            else if (feedbackStatus == -1)
                dislikeDiff += 1;

            if (likeDiff != 0 || dislikeDiff != 0) {
                // 1. 更新 km_app (保留旧逻辑，或可逐步废弃)
                // com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KmApp>
                // wrapper =
                // new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                // wrapper.eq(KmApp::getAppId, appId);

                // if (likeDiff > 0) {
                // wrapper.setSql("like_count = COALESCE(like_count, 0) + " + likeDiff);
                // } else if (likeDiff < 0) {
                // wrapper.setSql("like_count = GREATEST(COALESCE(like_count, 0) - " +
                // Math.abs(likeDiff) + ", 0)");
                // }

                // if (dislikeDiff > 0) {
                // wrapper.setSql("dislike_count = COALESCE(dislike_count, 0) + " +
                // dislikeDiff);
                // } else if (dislikeDiff < 0) {
                // wrapper.setSql("dislike_count = GREATEST(COALESCE(dislike_count, 0) - " +
                // Math.abs(dislikeDiff) + ", 0)");
                // }

                // appMapper.update(null, wrapper);

                // 2. 更新 km_app_access_stat (新统计体系)
                appService.updateAccessStat(appId, userId, 0L, likeDiff, dislikeDiff, 0);
            }
        }
        return true;
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
                throw new ServiceException(MessageUtils.message("ai.msg.app.not_found"));
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

            WorkflowExecutionReq req = WorkflowExecutionReq.builder().appId(debugApp.getAppId()).dslData(debugApp.getDslData()).enableExecutionDetail(debugApp.getEnableExecutionDetail()).showExecutionInfo(bo.getShowExecutionInfo()).message(bo.getMessage()).sessionId(debugSessionId).userId(userId).build();
            workflowExecutor.executeWorkflowDebug(req, emitter);

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

    private void sendSseEvent(SseEmitter emitter, SseEventType eventType, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventType.getEventName()).data(data));
        } catch (IllegalStateException e) {
            // 连接已关闭或已完成，这是正常的中止行为
            log.debug("SSE连接已关闭: {}", eventType);
        } catch (java.io.IOException e) {
            // 客户端中止连接，这是正常的中止行为
            if (e.getMessage() != null && e.getMessage().contains("中止")) {
                log.debug("客户端中止了连接: {}", eventType);
            } else {
                log.warn("发送SSE事件失败: {}", eventType, e);
            }
        } catch (Exception e) {
            log.error("发送SSE事件失败: {}", eventType, e);
        }
    }

    @Override
    public Object abortRequest(String requestId, Long userId) {
        return abortMixin.abortRequest(requestId, userId);
    }

    @Override
    public List<KmChatSessionVo> getResumableSessions(Long appId, Long userId) {
        return abortMixin.getResumableSessions(appId, userId);
    }

    @Override
    public KmChatSessionVo resumeSession(Long sessionId, Long userId) {
        return abortMixin.resumeSession(sessionId, userId);
    }

    @Override
    public Boolean clearAbortStatus(Long sessionId, Long userId) {
        return abortMixin.clearAbortStatus(sessionId, userId);
    }

    /**
     * 构建多模态消息
     * 当有附件时，将消息和附件组装为 LlmChatNode 期望的 JSON 数组格式：
     * [{type:text, text:...}, {type:image, ossId:..., url:...}, ...]
     *
     * @param message     用户文本消息
     * @param tempFileIds 临时文件 ID 列表（可为空）
     * @return 纯文本消息 or JSON 数组多模态消息
     */
    private String buildMultimodalMessage(String message, List<Long> tempFileIds) {
        if (tempFileIds == null || tempFileIds.isEmpty()) {
            return message;
        }

        List<Map<String, Object>> contents = new ArrayList<>();

        // 1. 文本部分
        if (StrUtil.isNotBlank(message)) {
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", message);
            contents.add(textContent);
        }

        // 2. 附件部分
        for (Long tempFileId : tempFileIds) {
            try {
                KmTempFile tempFile = kmFileService.getTempFile(tempFileId);
                if (tempFile == null) {
                    log.warn("临时文件不存在，跳过: tempFileId={}", tempFileId);
                    continue;
                }

                String ext = tempFile.getFileExtension() != null ? tempFile.getFileExtension().toLowerCase() : "";
                Map<String, Object> fileContent = new HashMap<>();

                if (isImageExtension(ext)) {
                    fileContent.put("type", "image");
                    fileContent.put("url", tempFile.getFilePath());
                    if (tempFile.getOssId() != null) {
                        fileContent.put("ossId", String.valueOf(tempFile.getOssId()));
                    }
                } else if (isAudioExtension(ext)) {
                    fileContent.put("type", "audio");
                    fileContent.put("url", tempFile.getFilePath());
                    if (tempFile.getOssId() != null) {
                        fileContent.put("ossId", String.valueOf(tempFile.getOssId()));
                    }
                } else {
                    // 通用文件：保留 type=file 及文件引用，供下游节点（FILE_STORAGE/FILE_PARSE）使用
                    fileContent.put("type", "file");
                    fileContent.put("tempFileId", String.valueOf(tempFileId));
                    if (tempFile.getOssId() != null) {
                        fileContent.put("ossId", String.valueOf(tempFile.getOssId()));
                    }
                    fileContent.put("url", tempFile.getFilePath());
                    fileContent.put("name", tempFile.getOriginalFilename());
                }
                contents.add(fileContent);
                log.info("多模态附件解析成功: tempFileId={}, type={}, ext={}", tempFileId, fileContent.get("type"), ext);
            } catch (Exception e) {
                log.error("解析临时文件失败: tempFileId={}", tempFileId, e);
            }
        }

        if (contents.size() == 1 && "text".equals(contents.get(0).get("type"))) {
            return message; // 只有纯文本，退回普通格式
        }

        return JSONUtil.toJsonStr(contents);
    }

    private boolean isImageExtension(String ext) {
        return java.util.Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg").contains(ext);
    }

    private boolean isAudioExtension(String ext) {
        return java.util.Set.of("mp3", "wav", "ogg", "m4a", "aac", "flac").contains(ext);
    }
}
