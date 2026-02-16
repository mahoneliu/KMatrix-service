package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.KmModelProvider;
import org.dromara.ai.domain.bo.KmModelBo;
import org.dromara.ai.domain.bo.KmModelChatSendBo;
import org.dromara.ai.domain.vo.KmModelVo;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.service.IKmModelService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.ai.util.ModelConnectionTester;
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
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmModelBo bo) {
        KmModel update = MapstructUtils.convert(bo, KmModel.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
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
        // 1. 验证模型是否存在且为语言模型
        KmModel model = baseMapper.selectById(modelId);
        if (model == null) {
            throw new ServiceException("模型不存在");
        }
        if (!"1".equals(model.getModelType())) {
            throw new ServiceException("只能将语言模型设置为默认模型");
        }

        // 2. 清除当前默认模型
        baseMapper.update(null, Wrappers.lambdaUpdate(KmModel.class)
                .set(KmModel::getIsDefault, 0)
                .eq(KmModel::getModelType, "1")
                .eq(KmModel::getIsDefault, 1));

        // 3. 设置新的默认模型
        return baseMapper.update(null, Wrappers.lambdaUpdate(KmModel.class)
                .set(KmModel::getIsDefault, 1)
                .eq(KmModel::getModelId, modelId)) > 0;
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
            String apiBase = StrUtil.isNotBlank(bo.getApiBase()) ? bo.getApiBase() : provider.getDefaultEndpoint();
            String modelKey = bo.getModelKey();

            // 根据供应商类型调用对应的测试方法
            return switch (providerKey) {
                case "openai" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "OpenAI");
                case "ollama" -> ModelConnectionTester.testOllama(apiBase, modelKey);
                case "qwen", "bailian" -> ModelConnectionTester.testQwen(apiKey, modelKey);
                case "gemini" -> ModelConnectionTester.testGemini(apiKey, modelKey);
                case "azure" -> {
                    // Azure 需要解析 apiBase 获取 endpoint 和 deploymentName
                    // 假设 apiBase 是 endpoint, modelKey 是 deploymentName
                    yield ModelConnectionTester.testAzureOpenAi(apiKey, apiBase, modelKey);
                }
                case "zhipu" -> ModelConnectionTester.testZhipu(apiKey, modelKey);
                case "anthropic" -> ModelConnectionTester.testAnthropic(apiKey, apiBase, modelKey);
                // 其它兼容 OpenAI 协议的供应商
                case "deepseek" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "DeepSeek");
                case "moonshot" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "Moonshot");
                case "xai" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "X.AI");
                case "vllm" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "vLLM");
                case "doubao" -> ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, "Doubao");
                default ->
                    ModelConnectionTester.testOpenAiCompatible(apiKey, apiBase, modelKey, provider.getProviderName());
            };
        } catch (Exception e) {
            log.error("模型连接测试失败", e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    @Override
    public SseEmitter streamTestChat(
            KmModelChatSendBo bo) {
        // 创建SSE发射器
        SseEmitter emitter = new SseEmitter(
                5 * 60 * 1000L);

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 获取模型
                KmModel model = baseMapper.selectById(bo.getModelId());
                if (model == null) {
                    sendError(emitter, "模型不存在");
                    return;
                }

                KmModelProvider provider = providerMapper.selectById(model.getProviderId());
                if (provider == null) {
                    sendError(emitter, "供应商不存在");
                    return;
                }

                // 构建 ChatModel
                // 这里为了简单，暂不使用流式 TokenHandler 回调，而是生成后一次性返回 (模拟流式，因为 ModelBuilder 返回的是
                // ChatLanguageModel)
                // 如果 ModelBuilder 支持 buildStreamingChatModel，则可以使用 real streaming
                // 让我们尝试使用 ChatLanguageModel (同步) 并发送结果

                // 如果需要实时流式，需修改 ModelBuilder 或在此处自行构建 StreamingModel
                // ModelBuilder 确实有 buildStreamingChatModel

                StreamingChatLanguageModel streamingModel = modelBuilder
                        .buildStreamingChatModel(
                                model, provider.getProviderKey(), bo.getTemperature(), bo.getMaxTokens());

                // 构造消息 (简单单轮对话)
                UserMessage userMessage = new UserMessage(
                        bo.getMessage());

                // 流式生成
                streamingModel.generate(Collections.singletonList(userMessage),
                        new StreamingResponseHandler<AiMessage>() {
                            @Override
                            public void onNext(String token) {
                                try {
                                    // 发送片段
                                    emitter.send(SseEmitter
                                            .event().name("token").data(token));
                                } catch (Exception e) {
                                    log.warn("SSE发送失败", e);
                                }
                            }

                            @Override
                            public void onComplete(
                                    Response<AiMessage> response) {
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
            emitter.send(
                    SseEmitter.event().name("error").data(msg));
            emitter.complete();
        } catch (Exception e) {
            // ignore
        }
    }
}
