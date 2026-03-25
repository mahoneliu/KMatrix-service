package org.dromara.ai.util;

import cn.hutool.core.util.StrUtil;
import org.dromara.common.core.utils.MessageUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GeminiHarmCategory;
import dev.langchain4j.model.googleai.GeminiHarmBlockThreshold;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.bo.KmModelBo;

import java.time.Duration;
import java.util.Collections;

/**
 * 模型连接测试工具类
 *
 * @author Mahone
 * @date 2025-12-25
 */
@Slf4j
public class ModelConnectionTester {

    private static final String TEST_MESSAGE = "Hello";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /**
     * 测试 OpenAI 兼容模型连接 (含 DeepSeek, Moonshot, XAI, vLLM 等)
     */
    public static String testOpenAiCompatible(KmModelBo bo, String providerName) {
        try {
            if (StrUtil.isBlank(bo.getApiKey())) {
                return MessageUtils.message("ai.msg.model.api_key_empty");
            }
            if (StrUtil.isBlank(bo.getModelKey())) {
                return MessageUtils.message("ai.msg.model.config_empty");
            }

            String type = bo.getModelType();
            if (StrUtil.isBlank(type)) type = "1"; // 默认聊天模型

            return switch (type) {
                case "2" -> testOpenAiEmbedding(bo, providerName);
                case "3" -> testOpenAiScoring(bo, providerName);
                default -> testOpenAiChat(bo, providerName);
            };
        } catch (Exception e) {
            log.error("{} 连接测试失败: model={}", providerName, bo.getModelKey(), e);
            return MessageUtils.message("ai.msg.model.connection_failed", e.getMessage());
        }
    }

    private static String testOpenAiChat(KmModelBo bo, String providerName) {
        var builder = OpenAiChatModel.builder()
                .apiKey(bo.getApiKey())
                .modelName(bo.getModelKey())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(bo.getApiBase())) {
            builder.baseUrl(bo.getApiBase());
        }

        ChatLanguageModel model = builder.build();
        String response = model.generate(TEST_MESSAGE);
        log.info("{} Chat连接测试成功: model={}, response={}", providerName, bo.getModelKey(), response);
        return MessageUtils.message("ai.msg.model.connection_success");
    }

    private static String testOpenAiEmbedding(KmModelBo bo, String providerName) {
        var builder = OpenAiEmbeddingModel.builder()
                .apiKey(bo.getApiKey())
                .modelName(bo.getModelKey())
                .timeout(DEFAULT_TIMEOUT);

        if (StrUtil.isNotBlank(bo.getApiBase())) {
            builder.baseUrl(bo.getApiBase());
        }

        EmbeddingModel model = builder.build();
        model.embed(TEST_MESSAGE);
        log.info("{} Embedding连接测试成功: model={}", providerName, bo.getModelKey());
        return MessageUtils.message("ai.msg.model.connection_success");
    }

    private static String testOpenAiScoring(KmModelBo bo, String providerName) {
        String apiBase = bo.getApiBase();
        if (StrUtil.isBlank(apiBase)) {
            apiBase = "https://api.siliconflow.cn/v1/";
        }

        ScoringModel model = OpenAiCompatibleScoringModel.builder()
                .apiKey(bo.getApiKey())
                .baseUrl(apiBase)
                .modelName(bo.getModelKey())
                .timeout(DEFAULT_TIMEOUT)
                .build();

        model.score(TEST_MESSAGE, "Hi");
        log.info("{} Scoring连接测试成功: model={}", providerName, bo.getModelKey());
        return MessageUtils.message("ai.msg.model.connection_success");
    }

    /**
     * 测试 Azure OpenAI 模型连接 (暂未引入依赖)
     */
    public static String testAzureOpenAi(String apiKey, String endpoint, String deploymentName) {
        return MessageUtils.message("ai.msg.model.azure_not_supported");
    }

    /**
     * 测试 Ollama 模型连接
     */
    public static String testOllama(String apiBase, String modelKey) {
        try {
            if (StrUtil.isBlank(apiBase)) {
                return MessageUtils.message("ai.msg.model.endpoint_empty");
            }
            if (StrUtil.isBlank(modelKey)) {
                return MessageUtils.message("ai.msg.model.config_empty");
            }

            ChatLanguageModel model = OllamaChatModel.builder()
                    .baseUrl(apiBase)
                    .modelName(modelKey)
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            String response = model.generate(TEST_MESSAGE);

            log.info("Ollama 连接测试成功: model={}, response={}", modelKey, response);
            return MessageUtils.message("ai.msg.model.connection_success");
        } catch (Exception e) {
            log.error("Ollama 连接测试失败: model={}", modelKey, e);
            return MessageUtils.message("ai.msg.model.connection_failed", e.getMessage());
        }
    }

    /**
     * 测试通义千问 (DashScope) 模型连接
     */
    public static String testQwen(String apiKey, String modelKey) {
        try {
            if (StrUtil.isBlank(apiKey)) {
                return MessageUtils.message("ai.msg.model.api_key_empty");
            }
            if (StrUtil.isBlank(modelKey)) {
                return MessageUtils.message("ai.msg.model.config_empty");
            }

            ChatLanguageModel model = QwenChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelKey)
                    .build();

            String response = model.generate(TEST_MESSAGE);

            log.info("通义千问连接测试成功: model={}, response={}", modelKey, response);
            return MessageUtils.message("ai.msg.model.connection_success");
        } catch (Exception e) {
            log.error("通义千问连接测试失败: model={}", modelKey, e);
            return MessageUtils.message("ai.msg.model.connection_failed", e.getMessage());
        }
    }

    /**
     * 测试 Google Gemini 模型连接
     */
    public static String testGemini(String apiKey, String modelKey) {
        try {
            if (StrUtil.isBlank(apiKey)) {
                return MessageUtils.message("ai.msg.model.api_key_empty");
            }
            if (modelKey == null) {
                return MessageUtils.message("ai.msg.model.config_empty");
            }

            ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelKey)
                    .safetySettings(Collections.singletonMap(
                            GeminiHarmCategory.HARM_CATEGORY_HATE_SPEECH, GeminiHarmBlockThreshold.BLOCK_NONE))
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            String response = model.generate(TEST_MESSAGE);

            log.info("Gemini 连接测试成功: model={}, response={}", modelKey, response);
            return MessageUtils.message("ai.msg.model.connection_success");
        } catch (Exception e) {
            log.error("Gemini 连接测试失败: model={}", modelKey, e);
            return MessageUtils.message("ai.msg.model.connection_failed", e.getMessage());
        }
    }

    /**
     * 测试智谱AI (Zhipu) 模型连接 - 使用 OpenAI 兼容模式
     */
    public static String testZhipu(KmModelBo bo) {
        // 智谱AI 新版接口兼容 OpenAI
        bo.setApiBase("https://open.bigmodel.cn/api/paas/v4/");
        return testOpenAiCompatible(bo, "智谱AI");
    }

    /**
     * 测试 SiliconFlow 模型连接 - 使用 OpenAI 兼容模式
     */
    public static String testSiliconFlow(KmModelBo bo) {
        if (StrUtil.isBlank(bo.getApiBase())) {
            bo.setApiBase("https://api.siliconflow.cn/v1/");
        }
        return testOpenAiCompatible(bo, "SiliconFlow");
    }

    /**
     * 测试 Anthropic (Claude) 模型连接 (暂未引入依赖)
     */
    public static String testAnthropic(String apiKey, String apiBase, String modelKey) {
        return MessageUtils.message("ai.msg.model.anthropic_not_supported");
    }
}
