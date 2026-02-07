package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.ai.domain.KmQuestionChunkMap;

import org.dromara.ai.domain.bo.KmRetrievalBo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private final KmDocumentChunkMapper chunkMapper;
    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final KmQuestionMapper questionMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;
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
     * 多源向量检索 (搜索 km_embedding 表，包括 Content 和 Question)
     * 如果命中问题，则通过 km_question_chunk_map 关联到内容块
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

        // 搜索 Content, Question 和 Title 类型的 embedding
        List<Integer> sourceTypes = Arrays.asList(
                KmEmbedding.SourceType.CONTENT,
                KmEmbedding.SourceType.QUESTION,
                KmEmbedding.SourceType.TITLE);

        List<Map<String, Object>> embeddingResults = embeddingMapper.vectorSearch(
                vectorStr, kbIds, sourceTypes, topK * 2, threshold);

        return processEmbeddingResults(embeddingResults, topK);
    }

    /**
     * 处理 Embedding 检索结果 (转换、关联 Chunk、去重、排序)
     */
    /**
     * 处理 Embedding 检索结果 (转换、关联 Chunk、去重、排序)
     */
    private List<KmRetrievalResultVo> processEmbeddingResults(List<Map<String, Object>> embeddingResults, int topK) {
        if (CollUtil.isEmpty(embeddingResults)) {
            return Collections.emptyList();
        }

        // 解析结果，将 Question 类型转换为关联的 Chunk
        Set<Long> chunkIds = new HashSet<>();
        Map<Long, Double> chunkScores = new HashMap<>();
        Map<Long, String> chunkHighlights = new HashMap<>();

        // 存储标题命中的文档ID
        Set<Long> titleDocIds = new HashSet<>();
        Map<Long, Double> titleDocScores = new HashMap<>();

        // 存储问题命中关联 (ChunkId -> List<QuestionId>)
        Map<Long, List<Long>> chunkQuestionMap = new HashMap<>();
        Set<Long> questionIds = new HashSet<>();

        // 存储切片命中来源类型 (ChunkId -> List<SourceType>)
        Map<Long, List<String>> chunkSourceTypes = new HashMap<>();

        for (Map<String, Object> row : embeddingResults) {
            Long sourceId = ((Number) row.get("source_id")).longValue();
            Integer sourceType = ((Number) row.get("source_type")).intValue();
            Double score = ((Number) row.get("score")).doubleValue();
            String highlight = (String) row.get("highlight");

            if (sourceType == KmEmbedding.SourceType.CONTENT) {
                // Content 类型：sourceId 就是 chunkId
                chunkIds.add(sourceId);
                chunkScores.merge(sourceId, score, Math::max);
                // 优先保存 Content 的高亮
                if (highlight != null) {
                    chunkHighlights.put(sourceId, highlight);
                }
                // 记录来源类型
                chunkSourceTypes.computeIfAbsent(sourceId, k -> new ArrayList<>()).add("CONTENT");

            } else if (sourceType == KmEmbedding.SourceType.TITLE) {
                // Title 类型：sourceId 是 documentId
                titleDocIds.add(sourceId);
                titleDocScores.merge(sourceId, score, Math::max);
            } else if (sourceType == KmEmbedding.SourceType.QUESTION) {
                // Question 类型：sourceId 是关联记录ID (question_chunk_map.id)
                // 需要通过 km_question_chunk_map 查找关联的 chunkId 和 questionId
                KmQuestionChunkMap mapRecord = questionChunkMapMapper.selectById(sourceId);
                if (mapRecord != null) {
                    chunkIds.add(mapRecord.getChunkId());
                    // 问题匹配可能比内容匹配更重要，这里取最高分
                    chunkScores.merge(mapRecord.getChunkId(), score, Math::max);
                    // 更新问题命中次数
                    questionMapper.incrementHitNum(mapRecord.getQuestionId());
                    // 记录问题ID与切片的关联
                    chunkQuestionMap.computeIfAbsent(mapRecord.getChunkId(), k -> new ArrayList<>())
                            .add(mapRecord.getQuestionId());
                    questionIds.add(mapRecord.getQuestionId());

                    // 记录来源类型
                    chunkSourceTypes.computeIfAbsent(mapRecord.getChunkId(), k -> new ArrayList<>())
                            .add("QUESTION");
                }
            }

            // 确保不重复添加
            List<String> types = chunkSourceTypes.getOrDefault(sourceId, new ArrayList<>());
            if (types != null && !types.isEmpty()) {
                // do not add default type if matched by question
            }
        }

        // 处理标题命中的文档，获取其第一个切片作为代表
        if (CollUtil.isNotEmpty(titleDocIds)) {
            for (Long docId : titleDocIds) {
                // 获取该文档的第一个切片(按ID排序)
                List<KmDocumentChunk> chunks = chunkMapper.selectList(
                        new LambdaQueryWrapper<KmDocumentChunk>()
                                .select(KmDocumentChunk::getId)
                                .eq(KmDocumentChunk::getDocumentId, docId)
                                .orderByAsc(KmDocumentChunk::getId)
                                .last("LIMIT 1"));

                if (CollUtil.isNotEmpty(chunks)) {
                    Long chunkId = chunks.get(0).getId();
                    chunkIds.add(chunkId);
                    chunkScores.merge(chunkId, titleDocScores.get(docId), Math::max);
                    // 记录来源类型
                    chunkSourceTypes.computeIfAbsent(chunkId, k -> new ArrayList<>()).add("TITLE");
                }
            }
        }

        if (chunkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取 Chunk 内容
        List<Map<String, Object>> chunkRows = chunkMapper.selectChunksByIds(new ArrayList<>(chunkIds));

        // 获取文档名称映射
        Set<Long> docIds = chunkRows.stream()
                .map(r -> ((Number) r.get("document_id")).longValue())
                .collect(Collectors.toSet());

        Map<Long, String> docNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<KmDocument> docs = documentMapper.selectByIds(docIds);
            docs.forEach(d -> docNameMap.put(d.getId(), d.getOriginalFilename()));
        }

        // 获取关联的问题内容
        Map<Long, List<String>> chunkMatchedQuestions = new HashMap<>();

        if (CollUtil.isNotEmpty(questionIds)) {
            List<KmQuestion> questions = questionMapper.selectByIds(questionIds);
            Map<Long, String> questionContentMap = questions.stream()
                    .collect(Collectors.toMap(KmQuestion::getId, KmQuestion::getContent));

            // 构建 chunkId -> List<QuestionContent> 的映射
            for (Map.Entry<Long, List<Long>> entry : chunkQuestionMap.entrySet()) {
                Long chunkId = entry.getKey();
                List<Long> qIds = entry.getValue();
                List<String> qContents = qIds.stream()
                        .map(questionContentMap::get)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());
                if (CollUtil.isNotEmpty(qContents)) {
                    chunkMatchedQuestions.put(chunkId, qContents);
                }
            }
        }

        // 构建结果并按分数排序
        return chunkRows.stream()
                .map(row -> {
                    Long chunkId = ((Number) row.get("id")).longValue();
                    KmRetrievalResultVo vo = new KmRetrievalResultVo();
                    vo.setChunkId(chunkId);
                    vo.setDocumentId(((Number) row.get("document_id")).longValue());
                    vo.setContent((String) row.get("content"));
                    vo.setTitle((String) row.get("title"));
                    vo.setMetadata(row.get("metadata"));
                    vo.setScore(chunkScores.getOrDefault(chunkId, 0.0));
                    vo.setDocumentName(docNameMap.get(vo.getDocumentId()));
                    vo.setHighlight(chunkHighlights.get(chunkId));
                    vo.setMatchedQuestions(chunkMatchedQuestions.get(chunkId));
                    vo.setSourceTypes(chunkSourceTypes.get(chunkId));
                    return vo;
                })
                .sorted(Comparator.comparingDouble(KmRetrievalResultVo::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 多源关键词检索
     * 对于关键词检索，仍然使用 km_document_chunk 表的全文索引
     * 但结果会与多源向量检索的结果格式保持一致
     *
     * @param query 查询文本
     * @param kbIds 知识库ID列表
     * @param topK  返回数量
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceKeywordSearch(String query, List<Long> kbIds, int topK) {
        // 搜索 Content, Question 和 Title 类型的 embedding
        List<Integer> sourceTypes = Arrays.asList(
                KmEmbedding.SourceType.CONTENT,
                KmEmbedding.SourceType.QUESTION,
                KmEmbedding.SourceType.TITLE);

        List<Map<String, Object>> embeddingResults = embeddingMapper.keywordSearch(
                query, kbIds, sourceTypes, topK * 2);

        return processEmbeddingResults(embeddingResults, topK);
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
