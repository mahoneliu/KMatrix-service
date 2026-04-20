package org.dromara.ai.model.service.impl;

import org.dromara.common.core.utils.MessageUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.domain.bo.KmModelBo;
import org.dromara.ai.model.domain.bo.KmModelChatSendBo;
import org.dromara.ai.model.domain.vo.KmModelVo;
import org.dromara.ai.api.enums.AiModelType;
import org.springframework.beans.factory.ObjectProvider;
import org.dromara.ai.model.service.IEmbeddingDataChecker;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.service.IKmModelService;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.model.util.ModelConnectionTester;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

/**
 * AI模型配置Service业务层处理
 *
 * @author Mahone
 * @date 2024-01-27
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmModelServiceImpl implements IKmModelService {

    private final KmModelMapper baseMapper;
    private final KmModelProviderMapper providerMapper;
    private final ObjectProvider<IEmbeddingDataChecker> embeddingDataCheckerProvider;
    private final ModelBuilder modelBuilder;

    /**
     * 构建查询条件包装器
     *
     * @param bo 查询条件对象
     * @return LambdaQueryWrapper
     */
    private LambdaQueryWrapper<KmModel> buildQueryWrapper(KmModelBo bo) {
        LambdaQueryWrapper<KmModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getProviderId() != null, KmModel::getProviderId, bo.getProviderId());
        lqw.like(StrUtil.isNotBlank(bo.getModelName()), KmModel::getModelName, bo.getModelName());
        lqw.eq(StrUtil.isNotBlank(bo.getModelType()), KmModel::getModelType, bo.getModelType());
        lqw.eq(StrUtil.isNotBlank(bo.getModelSource()), KmModel::getModelSource, bo.getModelSource());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), KmModel::getStatus, bo.getStatus());
        lqw.orderByDesc(KmModel::getIsDefault);
        lqw.orderByDesc(KmModel::getModelType);
        lqw.orderByDesc(KmModel::getProviderId);
        lqw.orderByDesc(KmModel::getCreateTime);
        return lqw;
    }

    @Override
    public List<KmModelVo> queryList(KmModelBo bo) {
        LambdaQueryWrapper<KmModel> lqw = buildQueryWrapper(bo);
        List<KmModelVo> list = baseMapper.selectVoList(lqw);

        // 填充供应商图标
        if (!list.isEmpty()) {
            List<KmModelProvider> providers = providerMapper.selectList(Wrappers.emptyWrapper());
            Map<Long, String> iconMap = providers.stream()
                    .collect(Collectors.toMap(KmModelProvider::getProviderId,
                            KmModelProvider::getIconUrl, (v1, v2) -> v1));
            list.forEach(m -> m.setProviderIcon(iconMap.get(m.getProviderId())));
        }

        return list;
    }

    @Override
    public KmModelVo queryById(Long modelId) {
        return baseMapper.selectVoById(modelId);
    }

    @Override
    public Boolean insertByBo(KmModelBo bo) {
        KmModel add = MapstructUtils.convert(bo, KmModel.class);
        // 补充默认多模态能力并执行向量模型约束校验
        fillDefaultAbilities(add);
        validateEmbeddingModelConstraint(null, bo, "INSERT");

        // 如果设置为默认模型，清理同类型其他默认模型
        if (Integer.valueOf(1).equals(add.getIsDefault())) {
            clearOtherDefaultModels(add.getModelType(), null);
        }
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmModelBo bo) {
        KmModel existing = baseMapper.selectById(bo.getModelId());
        if (existing == null) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.not_found"));
        }
        // 校验向量模型约束
        validateEmbeddingModelConstraint(existing, bo, "UPDATE");

        KmModel update = MapstructUtils.convert(bo, KmModel.class);
        // 如果开启了默认，清理同类型其他默认记录
        if (Integer.valueOf(1).equals(update.getIsDefault())) {
            clearOtherDefaultModels(update.getModelType(), update.getModelId());
        }

        // 如果传入的apiKey包含星号，说明前端由于脱敏没有修改真实值，这时置空以便让mybatis-plus更新时忽略
        if (StrUtil.isNotBlank(bo.getApiKey()) && bo.getApiKey().contains("*")) {
            update.setApiKey(null);
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteById(Long id) {
        KmModel model = baseMapper.selectById(id);
        if (model != null) {
            validateEmbeddingModelConstraint(model, null, "DELETE");
        }
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public Long copyModel(Long modelId) {
        // 查询原模型
        KmModel original = baseMapper.selectById(modelId);
        if (original == null) {
            throw new ServiceException("原模型不存在");
        }

        // 创建新模型
        KmModel copy = new KmModel();
        copy.setModelName("副本-" + original.getModelName());
        copy.setModelKey(original.getModelKey());
        copy.setModelType(original.getModelType());
        copy.setModelSource(original.getModelSource());
        copy.setProviderId(original.getProviderId());
        copy.setApiKey(original.getApiKey());
        copy.setApiBase(original.getApiBase());
        copy.setStatus(original.getStatus());
        copy.setRemark(original.getRemark());

        baseMapper.insert(copy);
        return copy.getModelId();
    }

    @Override
    public Boolean setDefaultModel(Long modelId) {
        // 1. 验证模型是否存在且为允许设置默认的类型
        KmModel model = baseMapper.selectById(modelId);
        if (model == null) {
            throw new ServiceException(MessageUtils.message("ai.msg.model.not_found"));
        }
        if (!AiModelType.LLM.getCode().equals(model.getModelType()) &&
                !AiModelType.EMBEDDING.getCode().equals(model.getModelType()) &&
                !AiModelType.RERANK.getCode().equals(model.getModelType())) {
            throw new ServiceException(MessageUtils.message("ai.msg.embedding.default_type_invalid"));
        }

        // 2. 校验向量模型约束及清理相同类型的默认模型
        validateEmbeddingModelConstraint(model, null, "SET_DEFAULT");
        clearOtherDefaultModels(model.getModelType(), modelId);

        // 3. 设置新的默认模型
        return baseMapper.update(null, Wrappers.lambdaUpdate(KmModel.class)
                .set(KmModel::getIsDefault, 1)
                .eq(KmModel::getModelId, modelId)) > 0;
    }

    @Override
    public Boolean hasDefaultModel(String modelType) {
        return baseMapper.selectCount(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, modelType)
                .eq(KmModel::getIsDefault, 1)
                .eq(KmModel::getStatus, "0")) > 0;
    }

    @Override
    public KmModelVo getDefaultModel(String modelType) {
        return baseMapper.selectVoOne(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, modelType)
                .eq(KmModel::getIsDefault, 1)
                .eq(KmModel::getStatus, "0"));
    }

    @Override
    public String testConnection(KmModelBo bo) {
        log.info("开始测试模型连接: modelName={}, modelKey={}, providerId={}",
                bo.getModelName(), bo.getModelKey(), bo.getProviderId());

        try {
            // 参数验证
            if (StrUtil.isBlank(bo.getModelKey())) {
                return "基础模型不能为空";
            }
            if (bo.getProviderId() == null) {
                return "供应商ID不能为空";
            }

            // 查询供应商信息
            KmModelProvider provider = providerMapper.selectById(bo.getProviderId());
            if (provider == null) {
                return "供应商不存在";
            }

            String providerKey = provider.getProviderKey();
            String apiKey = bo.getApiKey();

            // 如果前端传过来的apiKey是脱敏后的（包含*），且是修改情况，获取数据库中真实的apiKey
            if (StrUtil.isNotBlank(apiKey) && apiKey.contains("*") && bo.getModelId() != null) {
                KmModel oldModel = baseMapper.selectById(bo.getModelId());
                if (oldModel != null) {
                    apiKey = oldModel.getApiKey();
                }
            }
            bo.setApiKey(apiKey);

            String apiBase = StrUtil.isNotBlank(bo.getApiBase()) ? bo.getApiBase() : provider.getDefaultEndpoint();
            bo.setApiBase(apiBase);
            String modelKey = bo.getModelKey();

            // 根据供应商类型调用对应的测试方法
            return switch (providerKey.toLowerCase()) {
                case "openai", "deepseek", "moonshot", "doubao", "xai" ->
                    ModelConnectionTester.testOpenAiCompatible(bo, provider.getProviderName());
                case "siliconflow" -> ModelConnectionTester.testSiliconFlow(bo);
                case "ollama", "vllm" -> ModelConnectionTester.testOllama(bo.getApiBase(), bo.getModelKey());
                case "qwen", "bailian" -> ModelConnectionTester.testQwen(bo);
                case "gemini" -> ModelConnectionTester.testGemini(apiKey, modelKey);
                case "azure" -> {
                    // Azure 需要解析 apiBase 获取 endpoint 和 deploymentName
                    yield ModelConnectionTester.testAzureOpenAi(apiKey, apiBase, modelKey);
                }
                case "zhipu" -> {
                    // bo.setApiKey(apiKey);
                    yield ModelConnectionTester.testZhipu(bo);
                }
                case "anthropic" -> ModelConnectionTester.testAnthropic(apiKey, apiBase, modelKey);
                default -> {
                    // bo.setApiKey(apiKey);
                    bo.setApiBase(apiBase);
                    yield ModelConnectionTester.testOpenAiCompatible(bo, provider.getProviderName());
                }
            };
        } catch (Exception e) {
            log.error("模型连接测试失败", e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    @Override
    public SseEmitter streamTestChat(KmModelChatSendBo bo) {
        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 获取模型
                KmModel model = baseMapper.selectById(bo.getModelId());
                if (model == null) {
                    sendError(emitter, MessageUtils.message("ai.msg.model.not_found"));
                    return;
                }

                KmModelProvider provider = providerMapper.selectById(model.getProviderId());
                if (provider == null) {
                    sendError(emitter, "供应商不存在");
                    return;
                }

                // 处理 apiBase
                String apiBase = StrUtil.isNotBlank(model.getApiBase()) ? model.getApiBase()
                        : provider.getDefaultEndpoint();
                model.setApiBase(apiBase);

                StreamingChatModel streamingModel = modelBuilder.buildStreamingChatModel(
                        model, provider.getProviderKey(), bo.getTemperature(), bo.getMaxTokens());

                // 构造消息 (简单单轮对话)
                UserMessage userMessage = new UserMessage(bo.getMessage());

                // 流式生成
                streamingModel.chat(Collections.singletonList(userMessage),
                        new StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String token) {
                                try {
                                    // 发送片段
                                    emitter.send(SseEmitter.event().name("token").data(token));
                                } catch (Exception e) {
                                    log.warn("SSE发送失败", e);
                                }
                            }

                            @Override
                            public void onCompleteResponse(ChatResponse response) {
                                emitter.complete();
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("模型生成错误", error);
                                sendError(emitter, error.getMessage());
                            }
                        });

            } catch (Exception e) {
                log.error("测试对话失败", e);
                sendError(emitter, "对话失败: " + e.getMessage());
            }
        });

        return emitter;
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
            emitter.complete();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 校验向量模型约束：
     * 1. 保护兜底模型配置不被破坏。
     * 2. 防止产生多个兜底冲突。
     * 3. 防止删除正在使用的兜底模型。
     */
    private void validateEmbeddingModelConstraint(KmModel model, KmModelBo bo, String op) {
        String modelType = (bo != null) ? bo.getModelType() : (model != null ? model.getModelType() : null);
        if (!AiModelType.EMBEDDING.getCode().equals(modelType)) {
            return;
        }

        // 只有当向量表已有数据时才执行严格校验
        if (!hasEmbeddingData()) {
            return;
        }

        switch (op) {
            case "INSERT":
                if (Integer.valueOf(1).equals(bo.getIsDefault())) {
                    checkOnlyOneDefaultEmbedding(null);
                }
                break;
            case "UPDATE":
                // 如果当前模型是兜底，拦截关键配置修改
                if (Integer.valueOf(1).equals(model.getIsDefault())) {
                    if (!StrUtil.equals(model.getModelKey(), bo.getModelKey())
                            || !model.getProviderId().equals(bo.getProviderId())
                            || !StrUtil.equals(model.getApiBase(), bo.getApiBase())
                            || !StrUtil.equals(model.getModelType(), bo.getModelType())) {
                        throw new ServiceException(MessageUtils.message("ai.msg.embedding.default_immutable"));
                    }
                }
                // 如果新状态是设为兜底，拦截多个兜底情况
                if (Integer.valueOf(1).equals(bo.getIsDefault())) {
                    checkOnlyOneDefaultEmbedding(model.getModelId());
                }
                break;
            case "DELETE":
                if (Integer.valueOf(1).equals(model.getIsDefault())) {
                    throw new ServiceException(MessageUtils.message("ai.msg.embedding.default_undeletable"));
                }
                break;
            case "SET_DEFAULT":
                checkOnlyOneDefaultEmbedding(model.getModelId());
                break;
        }
    }

    private void checkOnlyOneDefaultEmbedding(Long currentModelId) {
        Long count = baseMapper.selectCount(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, AiModelType.EMBEDDING.getCode())
                .eq(KmModel::getIsDefault, 1)
                .ne(currentModelId != null, KmModel::getModelId, currentModelId));
        if (count > 0) {
            throw new ServiceException(MessageUtils.message("ai.msg.embedding.once_only"));
        }
    }

    /**
     * 安全地调用由 knowledge 模块注入的数据检查器
     */
    private boolean hasEmbeddingData() {
        IEmbeddingDataChecker checker = embeddingDataCheckerProvider.getIfAvailable();
        return checker != null && checker.hasData();
    }

    /**
     * 补充默认的多模态能力标识
     */
    private void fillDefaultAbilities(KmModel model) {
        if (model.getAbilities() == null || model.getAbilities().isEmpty()) {
            String mk = model.getModelKey() != null ? model.getModelKey().toLowerCase() : "";
            List<String> abilities = new java.util.ArrayList<>();
            if (mk.contains("vision") || mk.contains("-vl")) {
                abilities.add("vision");
            }
            if (mk.contains("whisper") || mk.contains("audio")) {
                abilities.add("audio");
            }
            if (!abilities.isEmpty()) {
                model.setAbilities(abilities);
            }
        }
    }

    /**
     * 清理同类型的其他默认模型
     *
     * @param modelType      模型类型
     * @param currentModelId 当前模型ID（排除自身）
     */
    private void clearOtherDefaultModels(String modelType, Long currentModelId) {
        // 针对 LLM, RERANK, AUDIO, IMAGE, VIDEO 类型进行自动清理
        if (AiModelType.LLM.getCode().equals(modelType) ||
                AiModelType.RERANK.getCode().equals(modelType) ||
                AiModelType.AUDIO.getCode().equals(modelType) ||
                AiModelType.IMAGE.getCode().equals(modelType) ||
                AiModelType.VIDEO.getCode().equals(modelType)) {
            baseMapper.update(null, Wrappers.lambdaUpdate(KmModel.class)
                    .set(KmModel::getIsDefault, 0)
                    .eq(KmModel::getModelType, modelType)
                    .eq(KmModel::getIsDefault, 1)
                    .ne(currentModelId != null, KmModel::getModelId, currentModelId));
        }
    }
}
