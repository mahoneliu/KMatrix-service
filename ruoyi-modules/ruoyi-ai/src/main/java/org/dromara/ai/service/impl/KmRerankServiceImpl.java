package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.service.IKmRerankService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BGE-Reranker 重排序服务实现
 * 支持 ONNX 本地模型或关键词回退方案
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Slf4j
@Service
public class KmRerankServiceImpl implements IKmRerankService {

    @Value("${ai.reranker.enabled:false}")
    private boolean enabled;

    // TODO: 将来集成 ONNX 模型后取消注释
    // @Value("${ai.reranker.model-path:}")
    // private String modelPath;
    // @Value("${ai.reranker.tokenizer-path:}")
    // private String tokenizerPath;

    private ScoringModel scoringModel;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Reranker model is disabled, using keyword-based rerank fallback");
            return;
        }

        // TODO: 集成 ONNX BGE-Reranker 模型
        // 需要下载模型文件并配置路径:
        // - ai.reranker.model-path: ONNX 模型文件路径 (.onnx)
        // - ai.reranker.tokenizer-path: Tokenizer 文件路径
        //
        // 示例代码:
        // try {
        // scoringModel = OnnxScoringModel.builder()
        // .modelPath(modelPath)
        // .tokenizerPath(tokenizerPath)
        // .build();
        // initialized = true;
        // log.info("BGE-Reranker initialized from {}", modelPath);
        // } catch (Exception e) {
        // log.error("Failed to initialize BGE-Reranker: {}", e.getMessage());
        // }

        log.info("Reranker enabled but ONNX model not configured, using keyword-based fallback");
    }

    @Override
    public List<KmRetrievalResultVo> rerank(String query, List<KmRetrievalResultVo> results, int topK) {
        if (CollUtil.isEmpty(results)) {
            return results;
        }

        if (initialized && scoringModel != null) {
            return rerankWithModel(query, results, topK);
        } else {
            return rerankWithKeywords(query, results, topK);
        }
    }

    @Override
    public boolean isEnabled() {
        return initialized && scoringModel != null;
    }

    /**
     * 使用 ONNX 模型进行重排序 (预留接口)
     */
    private List<KmRetrievalResultVo> rerankWithModel(String query, List<KmRetrievalResultVo> results, int topK) {
        try {
            List<TextSegment> segments = results.stream()
                    .map(r -> TextSegment.from(r.getContent()))
                    .collect(Collectors.toList());

            List<Double> scores = scoringModel.scoreAll(segments, query).content();

            List<KmRetrievalResultVo> scored = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                KmRetrievalResultVo vo = results.get(i);
                vo.setRerankScore(scores.get(i));
                scored.add(vo);
            }

            return scored.stream()
                    .sorted(Comparator.comparing(KmRetrievalResultVo::getRerankScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Model rerank failed, using keyword fallback: {}", e.getMessage());
            return rerankWithKeywords(query, results, topK);
        }
    }

    /**
     * 基于关键词匹配的简单重排序
     * 结合原始相似度分数和关键词匹配度进行重排
     */
    private List<KmRetrievalResultVo> rerankWithKeywords(String query, List<KmRetrievalResultVo> results, int topK) {
        String[] keywords = query.toLowerCase().split("\\s+");

        for (KmRetrievalResultVo r : results) {
            String content = r.getContent().toLowerCase();
            int matchCount = 0;
            int exactMatchBonus = 0;

            for (String kw : keywords) {
                if (kw.length() > 1 && content.contains(kw)) {
                    matchCount++;
                    // 完全匹配额外加分
                    if (content.contains(" " + kw + " ") || content.startsWith(kw + " ")
                            || content.endsWith(" " + kw)) {
                        exactMatchBonus++;
                    }
                }
            }

            // Rerank 分数 = 原始分数 * 0.6 + 关键词匹配度 * 0.3 + 精确匹配奖励 * 0.1
            double matchScore = keywords.length > 0 ? (double) matchCount / keywords.length : 0;
            double exactScore = keywords.length > 0 ? (double) exactMatchBonus / keywords.length : 0;
            double rerankScore = r.getScore() * 0.6 + matchScore * 0.3 + exactScore * 0.1;
            r.setRerankScore(rerankScore);
        }

        return results.stream()
                .sorted(Comparator.comparing(KmRetrievalResultVo::getRerankScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
