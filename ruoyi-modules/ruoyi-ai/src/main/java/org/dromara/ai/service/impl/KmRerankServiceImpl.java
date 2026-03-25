package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.enums.AiModelType;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.service.IKmRerankService;
import org.dromara.ai.util.ModelBuilder;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rerank 重排序服务实现
 *
 * <p>重排序调用优先级（从高到低）：</p>
 * <ol>
 *   <li>数据库默认 Rerank 模型（如 SiliconFlow Qwen3-Reranker-0.6B）—— 需用户在界面配置 API Key 并设为默认</li>
 *   <li>内置本地 ONNX 模型（bge-reranker-v2-m3）—— 需配置文件路径并开启 ai.reranker.enabled=true</li>
 *   <li>关键词回退（fallback）—— 没有任何模型时使用简单的关键词权重算法</li>
 * </ol>
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmRerankServiceImpl implements IKmRerankService {

    private final KmModelMapper kmModelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

    @Override
    public List<KmRetrievalResultVo> rerank(String query, List<KmRetrievalResultVo> results, int topK) {
        if (CollUtil.isEmpty(results)) {
            return results;
        }

        // 1. 根据数据库 km_model.is_default 来决定采用哪个 rerank 模型
        //    统一使用 modelBuilder.buildScoringModel 方法来管理实例及缓存
        KmModel defaultRerankModel = kmModelMapper.selectOne(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, AiModelType.RERANK.getCode())
                .eq(KmModel::getIsDefault, 1)
                .eq(KmModel::getStatus, "0"));

        if (defaultRerankModel != null) {
            try {
                var provider = providerMapper.selectById(defaultRerankModel.getProviderId());
                ScoringModel dbScoringModel = modelBuilder.buildScoringModel(defaultRerankModel, provider.getProviderKey());
                return rerankWithModel(query, results, topK, dbScoringModel);
            } catch (Exception e) {
                log.error("Rerank 模型 [{}] 调用失败，尝试关键词回退: {}", defaultRerankModel.getModelName(), e.getMessage());
            }
        }

        // 2. 如果没有启用默认模型，使用关键词回退
        return rerankWithKeywords(query, results, topK);
    }

    /**
     * 检查 Rerank 服务是否可用
     * <p>现在 Rerank 为动态加载，只要有默认开启的模型即视为可用。</p>
     */
    @Override
    public boolean isEnabled() {
        return kmModelMapper.selectCount(Wrappers.lambdaQuery(KmModel.class)
                .eq(KmModel::getModelType, AiModelType.RERANK.getCode())
                .eq(KmModel::getIsDefault, 1)
                .eq(KmModel::getStatus, "0")) > 0;
    }

    /**
     * 使用指定的 ScoringModel 进行重排序
     */
    private List<KmRetrievalResultVo> rerankWithModel(String query, List<KmRetrievalResultVo> results, int topK, ScoringModel model) {
        long start = System.currentTimeMillis();
        try {
            List<TextSegment> segments = results.stream()
                    .map(r -> TextSegment.from(r.getContent()))
                    .collect(Collectors.toList());

            List<Double> scores = model.scoreAll(segments, query).content();

            List<KmRetrievalResultVo> scored = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                KmRetrievalResultVo vo = results.get(i);
                vo.setRerankScore(scores.get(i));
                scored.add(vo);
            }

            List<KmRetrievalResultVo> finalResults = scored.stream()
                    .sorted(Comparator.comparing(KmRetrievalResultVo::getRerankScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("【性能分析】模型重排序(Rerank)耗时: {}ms", System.currentTimeMillis() - start);
            return finalResults;

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
        long start = System.currentTimeMillis();
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

        List<KmRetrievalResultVo> finalResults = results.stream()
                .sorted(Comparator.comparing(KmRetrievalResultVo::getRerankScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        log.info("【性能分析】基于关键词重排序(Fallback Rerank)耗时: {}ms", System.currentTimeMillis() - start);
        return finalResults;
    }
}
