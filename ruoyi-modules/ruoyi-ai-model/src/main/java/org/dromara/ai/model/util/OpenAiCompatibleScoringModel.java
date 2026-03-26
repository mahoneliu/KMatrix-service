package org.dromara.ai.model.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容的 Rerank (ScoringModel) 实现
 * <p>
 * 兼容 SiliconFlow、Jina AI 等使用 OpenAI 兼容 Rerank API 的供应商。
 * 请求格式：POST /rerank
 * Body: { "model": "...", "query": "...", "documents": ["...", "..."] }
 * </p>
 *
 * @author Mahone
 * @date 2026-03-26
 */
@Slf4j
@Builder
public class OpenAiCompatibleScoringModel implements ScoringModel {

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    @Builder.Default
    private final Duration timeout = Duration.ofSeconds(60);

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        if (segments == null || segments.isEmpty()) {
            return Response.from(List.of());
        }

        List<String> documents = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("query", query);
        body.put("documents", documents);
        body.put("top_n", documents.size());

        String requestBody = JSONUtil.toJsonStr(body);
        log.debug("Rerank request to {}: model={}, query={}, docCount={}",
                baseUrl, modelName, query, documents.size());

        // 构建 endpoint
        String endpoint = buildEndpoint();

        long start = System.currentTimeMillis();
        String responseStr = HttpUtil.createPost(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout((int) timeout.toMillis())
                .body(requestBody)
                .execute()
                .body();
        log.debug("Rerank response in {}ms", System.currentTimeMillis() - start);

        // 解析响应
        return parseResponse(responseStr, documents.size());
    }

    private String buildEndpoint() {
        String base = StrUtil.isBlank(baseUrl) ? "https://api.siliconflow.cn/v1/" : baseUrl;
        if (!base.endsWith("/")) {
            base += "/";
        }
        // 移除末尾的 /v1/，合并为正确路径
        if (base.endsWith("/v1/")) {
            return base + "rerank";
        }
        return base + "rerank";
    }

    private Response<List<Double>> parseResponse(String responseStr, int docCount) {
        try {
            JSONObject json = JSONUtil.parseObj(responseStr);

            // 检查错误
            if (json.containsKey("error")) {
                String errMsg = json.getJSONObject("error").getStr("message", "Unknown error");
                throw new RuntimeException("Rerank API error: " + errMsg);
            }

            // 解析 results 数组
            JSONArray results = json.getJSONArray("results");
            if (results == null) {
                throw new RuntimeException("Invalid rerank response: missing 'results' field");
            }

            // results 按 index 排序后提取分数
            Double[] scores = new Double[docCount];
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                int index = result.getInt("index");
                double score = result.getDouble("relevance_score");
                if (index >= 0 && index < docCount) {
                    scores[index] = score;
                }
            }

            // 填充缺失分数 (默认为 0.0)
            List<Double> scoreList = new ArrayList<>(docCount);
            for (int i = 0; i < docCount; i++) {
                scoreList.add(scores[i] != null ? scores[i] : 0.0);
            }

            return Response.from(scoreList);
        } catch (Exception e) {
            log.error("Failed to parse rerank response: {}", e.getMessage());
            log.debug("Raw response: {}", responseStr);
            throw new RuntimeException("Failed to parse rerank response: " + e.getMessage(), e);
        }
    }
}
