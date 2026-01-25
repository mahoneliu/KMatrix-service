package org.dromara.ai.workflow.util;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL 生成工具类
 * 
 * @author Mahone
 * @date 2026-01-24
 */
public final class SqlGenerator {

    // SQL 提取正则
    private static final Pattern SQL_PATTERN = Pattern.compile(
            "```sql\\s*([\\s\\S]*?)\\s*```|```\\s*([\\s\\S]*?)\\s*```|SELECT[\\s\\S]*?(?:;|$)",
            Pattern.CASE_INSENSITIVE);

    private SqlGenerator() {
        // 工具类禁止实例化
    }

    /**
     * 生成 SQL (流式)
     *
     * @param streamingModel StreamingChatLanguageModel
     * @param schema         数据库 Schema 描述
     * @param userQuery      用户查询
     * @param context        节点上下文 (用于发送SSE事件和保存token信息)
     * @return 生成的 SQL
     */
    public static String generateSql(dev.langchain4j.model.chat.StreamingChatLanguageModel streamingModel,
            String schema, String userQuery,
            org.dromara.ai.workflow.core.NodeContext context) {
        String systemPrompt = """
                你是一个专业的数据库助手。根据用户的问题和提供的数据库表结构，生成正确的SQL查询语句。

                要求：
                1. 只生成 SELECT 语句，不允许任何修改数据的操作
                2. SQL 语句用 ```sql ``` 包裹
                3. 确保SQL语法正确
                4. 如果用户问的问题无法通过给定的表结构查询，请说明原因

                数据库表结构：
                """ + schema;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userQuery));

        StringBuilder fullResponse = new StringBuilder();
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = context != null
                ? context.getSseEmitter()
                : null;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>> responseRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

        streamingModel.generate(messages,
                new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        fullResponse.append(token);
                        if (emitter != null) {
                            try {
                                // 发送 THINKING 事件
                                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                        .name(org.dromara.ai.domain.enums.SseEventType.THINKING.getEventName())
                                        .data(token));
                            } catch (Exception e) {
                                // 忽略发送失败
                            }
                        }
                    }

                    @Override
                    public void onComplete(
                            dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                        responseRef.set(response);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorRef.set(error);
                        latch.countDown();
                    }
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SQL生成被中断", e);
        }

        if (errorRef.get() != null) {
            throw new RuntimeException("SQL生成失败", errorRef.get());
        }

        // 保存 token 使用信息
        var response = responseRef.get();
        if (context != null && response != null && response.tokenUsage() != null) {
            dev.langchain4j.model.output.TokenUsage tokenUsage = response.tokenUsage();
            java.util.Map<String, Object> tokenUsageMap = new java.util.HashMap<>();
            tokenUsageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
            tokenUsageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
            tokenUsageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
            context.setTokenUsage(tokenUsageMap);
        }

        String responseText = fullResponse.toString();
        if (StrUtil.isBlank(responseText)) {
            throw new RuntimeException("LLM未返回有效响应");
        }

        // 提取 SQL
        Matcher matcher = SQL_PATTERN.matcher(responseText);
        if (matcher.find()) {
            String sql = matcher.group(1);
            if (sql == null)
                sql = matcher.group(2);
            if (sql == null)
                sql = matcher.group(0);
            return sql.trim();
        }

        throw new RuntimeException("无法从LLM响应中提取SQL语句: " + responseText);
    }

    /**
     * 生成 SQL
     *
     * @param chatModel ChatLanguageModel
     * @param schema    数据库 Schema 描述
     * @param userQuery 用户查询
     * @param context   节点上下文 (用于保存token信息，可为null)
     * @return 生成的 SQL
     */
    public static String generateSql(ChatLanguageModel chatModel, String schema, String userQuery,
            org.dromara.ai.workflow.core.NodeContext context) {
        String systemPrompt = """
                你是一个专业的数据库助手。根据用户的问题和提供的数据库表结构，生成正确的SQL查询语句。

                要求：
                1. 只生成 SELECT 语句，不允许任何修改数据的操作
                2. SQL 语句用 ```sql ``` 包裹
                3. 确保SQL语法正确
                4. 如果用户问的问题无法通过给定的表结构查询，请说明原因

                数据库表结构：
                """ + schema;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userQuery));

        var response = chatModel.generate(messages);
        if (response == null || response.content() == null || response.content().text() == null) {
            throw new RuntimeException("LLM未返回有效响应（可能使用了思考模式但未生成文本）");
        }

        // 保存 token 使用信息
        if (context != null && response.tokenUsage() != null) {
            dev.langchain4j.model.output.TokenUsage tokenUsage = response.tokenUsage();
            java.util.Map<String, Object> tokenUsageMap = new java.util.HashMap<>();
            tokenUsageMap.put("inputTokenCount", tokenUsage.inputTokenCount());
            tokenUsageMap.put("outputTokenCount", tokenUsage.outputTokenCount());
            tokenUsageMap.put("totalTokenCount", tokenUsage.totalTokenCount());
            context.setTokenUsage(tokenUsageMap);
        }

        String responseText = response.content().text();

        // 提取 SQL
        Matcher matcher = SQL_PATTERN.matcher(responseText);
        if (matcher.find()) {
            String sql = matcher.group(1);
            if (sql == null)
                sql = matcher.group(2);
            if (sql == null)
                sql = matcher.group(0);
            return sql.trim();
        }

        throw new RuntimeException("无法从LLM响应中提取SQL语句: " + responseText);
    }

    /**
     * 选择相关表
     *
     * @param chatModel ChatLanguageModel
     * @param tableList 数据库表列表
     * @param userQuery 用户查询
     * @return 相关表名列表
     */
    public static List<String> selectRelevantTables(ChatLanguageModel chatModel, String tableList, String userQuery) {
        String systemPrompt = """
                你是一个数据库专家。请根据用户的问题和提供的数据库表清单，找出回答用户问题需要用到的表。

                要求：
                1. 只返回表名，多个表名用逗号分隔
                2. 不要返回任何其他内容，不要解释
                3. 如果没有相关的表，返回空字符串
                4. 请仔细分析表名和表注释，确保选出的表是相关的

                数据库表清单：
                """ + tableList;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userQuery));

        var aiMessage = chatModel.generate(messages);
        if (aiMessage == null || aiMessage.content() == null || aiMessage.content().text() == null) {
            return Collections.emptyList();
        }
        String response = aiMessage.content().text();
        if (StrUtil.isBlank(response)) {
            return Collections.emptyList();
        }

        // 清理响应，移除可能的无关字符
        response = response.replace("`", "").replace("\n", "").trim();

        if (StrUtil.isBlank(response)) {
            return Collections.emptyList();
        }

        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
