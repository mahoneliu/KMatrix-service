package org.dromara.ai.util;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GeminiHarmCategory;
import dev.langchain4j.model.googleai.GeminiHarmBlockThreshold;
import lombok.extern.slf4j.Slf4j;

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
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 测试 OpenAI 模型连接
     */
    public static String testOpenAI(String apiKey, String apiBase, String modelKey) {
        try {
            if (StrUtil.isBlank(apiKey)) {
                return "API Key 不能为空";
            }
            if (StrUtil.isBlank(modelKey)) {
                return "基础模型不能为空";
            }

            var builder = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelKey)
                    .timeout(DEFAULT_TIMEOUT);

            if (StrUtil.isNotBlank(apiBase)) {
                builder.baseUrl(apiBase);
            }

            ChatLanguageModel model = builder.build();
            String response = model.generate(TEST_MESSAGE);

            log.info("OpenAI 连接测试成功: model={}, response={}", modelKey, response);
            return "连接测试成功";
        } catch (Exception e) {
            log.error("OpenAI 连接测试失败: model={}", modelKey, e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    /**
     * 测试 Ollama 模型连接
     */
    public static String testOllama(String apiBase, String modelKey) {
        try {
            if (StrUtil.isBlank(apiBase)) {
                return "API Base URL 不能为空";
            }
            if (StrUtil.isBlank(modelKey)) {
                return "基础模型不能为空";
            }

            ChatLanguageModel model = OllamaChatModel.builder()
                    .baseUrl(apiBase)
                    .modelName(modelKey)
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            String response = model.generate(TEST_MESSAGE);

            log.info("Ollama 连接测试成功: model={}, response={}", modelKey, response);
            return "连接测试成功";
        } catch (Exception e) {
            log.error("Ollama 连接测试失败: model={}", modelKey, e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    /**
     * 测试通义千问 (DashScope) 模型连接
     */
    public static String testQwen(String apiKey, String modelKey) {
        try {
            if (StrUtil.isBlank(apiKey)) {
                return "API Key 不能为空";
            }
            if (StrUtil.isBlank(modelKey)) {
                return "基础模型不能为空";
            }

            ChatLanguageModel model = QwenChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelKey)
                    .build();

            String response = model.generate(TEST_MESSAGE);

            log.info("通义千问连接测试成功: model={}, response={}", modelKey, response);
            return "连接测试成功";
        } catch (Exception e) {
            log.error("通义千问连接测试失败: model={}", modelKey, e);
            return "连接测试失败: " + e.getMessage();
        }
    }

    /**
     * 测试 Google Gemini 模型连接
     */
    public static String testGemini(String apiKey, String modelKey) {
        try {
            if (StrUtil.isBlank(apiKey)) {
                return "API Key 不能为空";
            }
            if (modelKey == null) {
                return "基础模型不能为空";
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
            return "连接测试成功";
        } catch (Exception e) {
            log.error("Gemini 连接测试失败: model={}", modelKey, e);
            return "连接测试失败: " + e.getMessage();
        }
    }
}
