package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmRetrievalService;
import org.dromara.ai.service.IKmRerankService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现
 * 支持向量检索、关键词检索、混合检索
 * 支持多源检索 (Content + Question)
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmRetrievalServiceImpl implements IKmRetrievalService {

    private final KmDatasetMapper datasetMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final KmQuestionMapper questionMapper;
    private final IKmRerankService rerankService;
    private final EmbeddingModel embeddingModel;

    // RRF 融合常数
    private static final int RRF_K = 60;

    @Override
    public List<KmRetrievalResultVo> search(KmRetrievalBo bo) {
        if (StrUtil.isBlank(bo.getQuery())) {
            return Collections.emptyList();
        }

        // 获取知识库ID列表
        List<Long> kbIds = bo.getKbIds();
        if (CollUtil.isEmpty(kbIds) && CollUtil.isNotEmpty(bo.getDatasetIds())) {
            // 如果只有 datasetIds，转换为 kbIds
            kbIds = resolveKbIdsFromDatasets(bo.getDatasetIds());
        }

        int topK = bo.getTopK() != null ? bo.getTopK() : 5;
        double threshold = bo.getThreshold() != null ? bo.getThreshold() : 0.0; // Debug: default to 0.0
        String mode = bo.getMode() != null ? bo.getMode() : "VECTOR";

        log.info("Search Params: query={}, kbIds={}, topK={}, threshold={}, mode={}",
                bo.getQuery(), kbIds, topK, threshold, mode);

        List<KmRetrievalResultVo> results;

        // 执行多源检索 (搜索 km_embedding 表，包括 Content + Question + Title)
        switch (mode.toUpperCase()) {
            case "KEYWORD":
                results = multiSourceKeywordSearch(bo.getQuery(), kbIds, topK);
                break;
            case "HYBRID":
                results = multiSourceHybridSearch(bo.getQuery(), kbIds, topK, threshold);
                break;
            case "VECTOR":
            default:
                results = multiSourceVectorSearch(bo.getQuery(), kbIds, topK, threshold);
                break;
        }

        // 如果启用 Rerank
        if (Boolean.TRUE.equals(bo.getEnableRerank()) && CollUtil.isNotEmpty(results)) {
            results = rerankService.rerank(bo.getQuery(), results, topK);
        }

        log.info("Search Results: count={}", results != null ? results.size() : 0);

        return results;
    }

    /**
     * 多源向量检索 (优化版 - 使用 SQL 表关联一次性获取所有数据)
     * 搜索 km_embedding 表，包括 Content、Question 和 Title 类型
     * 通过表关联优化性能，减少数据库交互次数
     *
     * @param query     查询文本
     * @param kbIds     知识库ID列表
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceVectorSearch(String query, List<Long> kbIds, int topK,
            double threshold) {
        // 生成查询向量
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();
        String vectorStr = floatArrayToString(queryEmbedding);

        // 使用优化后的多表关联查询，一次性获取所有数据（始终查询所有源类型）
        List<Map<String, Object>> results = embeddingMapper.vectorSearch(
                vectorStr, kbIds, topK * 2, threshold);

        return processSearchResults(results, topK);
    }

    /**
     * 处理多表关联查询的结果 (优化版)
     * 数据已经通过 SQL JOIN 获取，只需聚合和去重
     * 
     * @param results 关联查询结果
     * @param topK    返回数量
     * @return 检索结果列表
     */
    private List<KmRetrievalResultVo> processSearchResults(List<Map<String, Object>> results, int topK) {
        if (CollUtil.isEmpty(results)) {
            return Collections.emptyList();
        }

        // 按 chunkId 分组聚合
        Map<Long, KmRetrievalResultVo> chunkMap = new LinkedHashMap<>();
        Map<Long, Double> chunkScores = new HashMap<>();
        Map<Long, List<String>> chunkSourceTypes = new HashMap<>();
        Set<Long> questionIds = new HashSet<>();
        Map<Long, List<Long>> chunkQuestionMap = new HashMap<>(); // chunkId -> List<questionId>

        for (Map<String, Object> row : results) {
            Long chunkId = ((Number) row.get("chunk_id")).longValue();
            Double score = ((Number) row.get("score")).doubleValue();
            String sourceTypeLabel = (String) row.get("source_type_label");
            Long questionId = row.get("question_id") != null ? ((Number) row.get("question_id")).longValue() : null;

            // 如果是新的 chunk，创建 VO
            if (!chunkMap.containsKey(chunkId)) {
                KmRetrievalResultVo vo = new KmRetrievalResultVo();
                vo.setChunkId(chunkId);
                vo.setDocumentId(((Number) row.get("document_id")).longValue());
                vo.setContent((String) row.get("content"));
                vo.setTitle((String) row.get("chunk_title"));
                vo.setMetadata(row.get("metadata"));
                vo.setDocumentName((String) row.get("document_name"));
                vo.setSourceTypes(new ArrayList<>());
                // 设置高亮字段(如果存在)
                String highlight = (String) row.get("highlight");
                if (StrUtil.isNotBlank(highlight)) {
                    vo.setHighlight(highlight);
                }
                chunkMap.put(chunkId, vo);
            }

            KmRetrievalResultVo vo = chunkMap.get(chunkId);

            // 记录最高分数
            chunkScores.merge(chunkId, score, Math::max);

            // 记录来源类型（去重）
            chunkSourceTypes.computeIfAbsent(chunkId, k -> new ArrayList<>());
            if (!chunkSourceTypes.get(chunkId).contains(sourceTypeLabel)) {
                chunkSourceTypes.get(chunkId).add(sourceTypeLabel);
                vo.getSourceTypes().add(sourceTypeLabel);
            }

            // 收集问题ID（用于批量更新命中次数和查询问题内容）
            if (questionId != null) {
                questionIds.add(questionId);
                // 建立 chunkId -> questionId 的映射
                chunkQuestionMap.computeIfAbsent(chunkId, k -> new ArrayList<>()).add(questionId);
            }
        }

        // 批量更新问题命中次数（不阻塞主流程）
        if (CollUtil.isNotEmpty(questionIds)) {
            questionMapper.batchIncrementHitNum(new ArrayList<>(questionIds));
        }

        // 批量查询问题内容并填充 matchedQuestions
        if (CollUtil.isNotEmpty(questionIds)) {
            List<KmQuestion> questions = questionMapper.selectByIds(questionIds);
            Map<Long, String> questionContentMap = questions.stream()
                    .collect(Collectors.toMap(KmQuestion::getId, KmQuestion::getContent));

            // 为每个 chunk 填充 matchedQuestions
            for (Map.Entry<Long, List<Long>> entry : chunkQuestionMap.entrySet()) {
                Long chunkId = entry.getKey();
                List<Long> qIds = entry.getValue();
                List<String> matchedQuestions = qIds.stream()
                        .map(questionContentMap::get)
                        .filter(StrUtil::isNotBlank)
                        .distinct() // 去重
                        .collect(Collectors.toList());

                KmRetrievalResultVo vo = chunkMap.get(chunkId);
                if (vo != null && CollUtil.isNotEmpty(matchedQuestions)) {
                    vo.setMatchedQuestions(matchedQuestions);
                }
            }
        }

        // 设置分数并排序
        return chunkMap.values().stream()
                .peek(vo -> vo.setScore(chunkScores.get(vo.getChunkId())))
                .sorted(Comparator.comparingDouble(KmRetrievalResultVo::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 多源关键词检索 (优化版 - 使用 SQL 表关联一次性获取所有数据)
     * 使用全文检索搜索 km_embedding 表，包括 Content、Question 和 Title 类型
     * 通过表关联优化性能，减少数据库交互次数
     *
     * @param query 查询文本
     * @param kbIds 知识库ID列表
     * @param topK  返回数量
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceKeywordSearch(String query, List<Long> kbIds, int topK) {
        // 使用优化后的多表关联查询，一次性获取所有数据（始终查询所有源类型）
        List<Map<String, Object>> results = embeddingMapper.keywordSearch(
                query, kbIds, topK * 2);

        return processSearchResults(results, topK);
    }

    /**
     * 多源混合检索 (向量 + 关键词 + RRF 融合)
     *
     * @param query     查询文本
     * @param kbIds     知识库ID列表
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceHybridSearch(String query, List<Long> kbIds, int topK,
            double threshold) {
        // 1. 多源向量检索
        List<KmRetrievalResultVo> vectorResults = multiSourceVectorSearch(query, kbIds, topK * 2, 0);
        // 2. 多源关键词检索
        List<KmRetrievalResultVo> keywordResults = multiSourceKeywordSearch(query, kbIds, topK * 2);

        // 3. RRF 融合
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, KmRetrievalResultVo> chunkMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            KmRetrievalResultVo r = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);
            chunkMap.putIfAbsent(r.getChunkId(), r);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            KmRetrievalResultVo r = keywordResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);

            if (chunkMap.containsKey(r.getChunkId())) {
                KmRetrievalResultVo existing = chunkMap.get(r.getChunkId());
                // Merge matched questions
                if (existing.getMatchedQuestions() == null) {
                    existing.setMatchedQuestions(r.getMatchedQuestions());
                } else if (CollUtil.isNotEmpty(r.getMatchedQuestions())) {
                    // Add non-duplicate questions
                    Set<String> questions = new LinkedHashSet<>(existing.getMatchedQuestions());
                    questions.addAll(r.getMatchedQuestions());
                    existing.setMatchedQuestions(new ArrayList<>(questions));
                }

                // Merge source types
                if (existing.getSourceTypes() == null) {
                    existing.setSourceTypes(r.getSourceTypes());
                } else if (CollUtil.isNotEmpty(r.getSourceTypes())) {
                    Set<String> types = new LinkedHashSet<>(existing.getSourceTypes());
                    types.addAll(r.getSourceTypes());
                    existing.setSourceTypes(new ArrayList<>(types));
                }
            } else {
                chunkMap.put(r.getChunkId(), r);
            }
        }

        // 4. 按 RRF 分数排序
        List<KmRetrievalResultVo> results = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    KmRetrievalResultVo vo = chunkMap.get(e.getKey());
                    vo.setScore(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());

        // 5. 归一化并过滤
        double maxScore = results.isEmpty() ? 1 : results.get(0).getScore();
        return results.stream()
                .peek(r -> r.setScore(r.getScore() / maxScore))
                .filter(r -> r.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 从数据集ID列表反向解析知识库ID列表
     */
    private List<Long> resolveKbIdsFromDatasets(List<Long> datasetIds) {
        if (CollUtil.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }
        List<KmDataset> datasets = datasetMapper.selectByIds(datasetIds);
        return datasets.stream()
                .map(KmDataset::getKbId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 字符串格式
     */
    private String floatArrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

}
